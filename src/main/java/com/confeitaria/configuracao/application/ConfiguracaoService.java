package com.confeitaria.configuracao.application;

import com.confeitaria.configuracao.api.ConfiguracaoDtos.AdicionalBandeiraRequest;
import com.confeitaria.configuracao.api.ConfiguracaoDtos.AdicionalBandeiraResponse;
import com.confeitaria.configuracao.api.ConfiguracaoDtos.ConfiguracaoRequest;
import com.confeitaria.configuracao.api.ConfiguracaoDtos.ConfiguracaoResponse;
import com.confeitaria.configuracao.api.ConfiguracaoDtos.TarifaRequest;
import com.confeitaria.configuracao.api.ConfiguracaoDtos.TarifaResponse;
import com.confeitaria.configuracao.persistence.AdicionalBandeira;
import com.confeitaria.configuracao.persistence.AdicionalBandeiraRepository;
import com.confeitaria.configuracao.persistence.Bandeira;
import com.confeitaria.configuracao.persistence.ConfiguracaoCustos;
import com.confeitaria.configuracao.persistence.ConfiguracaoCustosRepository;
import com.confeitaria.configuracao.persistence.TarifaEnergia;
import com.confeitaria.configuracao.persistence.TarifaEnergiaRepository;
import com.confeitaria.shared.domain.Decimais;
import com.confeitaria.shared.error.ConflitoException;
import com.confeitaria.shared.error.RecursoNaoEncontradoException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfiguracaoService {

    public static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    private final ConfiguracaoCustosRepository configuracoes;
    private final TarifaEnergiaRepository tarifas;
    private final AdicionalBandeiraRepository adicionais;
    private final Clock clock;

    public ConfiguracaoService(ConfiguracaoCustosRepository configuracoes, TarifaEnergiaRepository tarifas,
            AdicionalBandeiraRepository adicionais, Clock clock) {
        this.configuracoes = configuracoes;
        this.tarifas = tarifas;
        this.adicionais = adicionais;
        this.clock = clock;
    }

    public LocalDate hoje() {
        return LocalDate.now(clock.withZone(ZONA));
    }

    @Transactional
    public void criarPadrao(UUID ownerId) {
        if (!configuracoes.existsById(ownerId)) {
            configuracoes.save(ConfiguracaoCustos.padrao(ownerId));
        }
    }

    @Transactional
    public ConfiguracaoCustos obterEntidade(UUID ownerId) {
        return configuracoes.findById(ownerId)
                .orElseGet(() -> configuracoes.save(ConfiguracaoCustos.padrao(ownerId)));
    }

    @Transactional
    public ConfiguracaoResponse obter(UUID ownerId) {
        return toResponse(obterEntidade(ownerId));
    }

    @Transactional
    public ConfiguracaoResponse atualizar(UUID ownerId, ConfiguracaoRequest r) {
        ConfiguracaoCustos config = obterEntidade(ownerId);
        config.atualizar(r.salarioDesejadoMes(), r.horasTrabalhadasMes(), r.bandeiraVigente(), r.gasPrecoBotijao(),
                r.gasDuracaoHoras(), r.criterioRateio(), r.permitirSaldoNegativo());
        return toResponse(config);
    }

    // ---- tarifa do kWh ----

    @Transactional(readOnly = true)
    public Page<TarifaResponse> listarTarifas(UUID ownerId, Pageable pageable) {
        return tarifas.findByOwnerId(ownerId, pageable).map(TarifaResponse::of);
    }

    @Transactional
    public TarifaResponse registrarTarifa(UUID ownerId, TarifaRequest r) {
        if (tarifas.existsByOwnerIdAndVigenciaInicio(ownerId, r.vigenciaInicio())) {
            throw new ConflitoException("Já existe uma tarifa com vigência a partir de " + r.vigenciaInicio() + ".");
        }
        return TarifaResponse.of(tarifas.save(new TarifaEnergia(ownerId, r.tarifaKwh(), r.vigenciaInicio())));
    }

    @Transactional(readOnly = true)
    public TarifaResponse tarifaVigente(UUID ownerId) {
        return tarifas.findFirstByOwnerIdAndVigenciaInicioLessThanEqualOrderByVigenciaInicioDesc(ownerId, hoje())
                .map(TarifaResponse::of)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tarifa vigente"));
    }

    @Transactional(readOnly = true)
    public Optional<BigDecimal> valorTarifaVigente(UUID ownerId, LocalDate data) {
        return tarifas.findFirstByOwnerIdAndVigenciaInicioLessThanEqualOrderByVigenciaInicioDesc(ownerId, data)
                .map(TarifaEnergia::getTarifaKwh);
    }

    @Transactional
    public void removerTarifa(UUID ownerId, UUID id) {
        tarifas.delete(tarifas.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tarifa")));
    }

    // ---- adicionais das bandeiras ----

    @Transactional(readOnly = true)
    public Page<AdicionalBandeiraResponse> listarAdicionais(UUID ownerId, Bandeira bandeira, Pageable pageable) {
        Page<AdicionalBandeira> page = bandeira == null
                ? adicionais.findByOwnerId(ownerId, pageable)
                : adicionais.findByOwnerIdAndBandeira(ownerId, bandeira, pageable);
        return page.map(AdicionalBandeiraResponse::of);
    }

    @Transactional
    public AdicionalBandeiraResponse registrarAdicional(UUID ownerId, AdicionalBandeiraRequest r) {
        if (adicionais.existsByOwnerIdAndBandeiraAndVigenciaInicio(ownerId, r.bandeira(), r.vigenciaInicio())) {
            throw new ConflitoException("Já existe um adicional para a bandeira " + r.bandeira()
                    + " com vigência a partir de " + r.vigenciaInicio() + ".");
        }
        return AdicionalBandeiraResponse.of(adicionais.save(
                new AdicionalBandeira(ownerId, r.bandeira(), r.adicionalKwh(), r.vigenciaInicio())));
    }

    /** Adicional vigente hoje de cada bandeira (as não cadastradas são omitidas). */
    @Transactional(readOnly = true)
    public List<AdicionalBandeiraResponse> adicionaisVigentes(UUID ownerId) {
        LocalDate hoje = hoje();
        return Arrays.stream(Bandeira.values())
                .flatMap(b -> adicionais
                        .findFirstByOwnerIdAndBandeiraAndVigenciaInicioLessThanEqualOrderByVigenciaInicioDesc(
                                ownerId, b, hoje)
                        .stream())
                .map(AdicionalBandeiraResponse::of)
                .toList();
    }

    /** VERDE sem cadastro equivale a adicional zero; as demais bandeiras precisam estar cadastradas. */
    @Transactional(readOnly = true)
    public Optional<BigDecimal> valorAdicionalVigente(UUID ownerId, Bandeira bandeira, LocalDate data) {
        Optional<BigDecimal> valor = adicionais
                .findFirstByOwnerIdAndBandeiraAndVigenciaInicioLessThanEqualOrderByVigenciaInicioDesc(
                        ownerId, bandeira, data)
                .map(AdicionalBandeira::getAdicionalKwh);
        return valor.isEmpty() && bandeira == Bandeira.VERDE ? Optional.of(BigDecimal.ZERO) : valor;
    }

    @Transactional
    public void removerAdicional(UUID ownerId, UUID id) {
        adicionais.delete(adicionais.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Adicional de bandeira")));
    }

    private static ConfiguracaoResponse toResponse(ConfiguracaoCustos c) {
        BigDecimal valorHora = Decimais.monetario(
                Decimais.dividir(c.getSalarioDesejadoMes(), c.getHorasTrabalhadasMes()));
        BigDecimal gasHora = c.getGasPrecoBotijao() == null ? null
                : Decimais.monetario(Decimais.dividir(c.getGasPrecoBotijao(), c.getGasDuracaoHoras()));
        return new ConfiguracaoResponse(c.getSalarioDesejadoMes(), c.getHorasTrabalhadasMes(), valorHora,
                c.getBandeiraVigente(), c.getGasPrecoBotijao(), c.getGasDuracaoHoras(), gasHora,
                c.getCriterioRateio(), c.isPermitirSaldoNegativo());
    }
}
