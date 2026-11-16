package com.confeitaria.precificacao.application;

import com.confeitaria.canalvenda.application.CanalVendaService;
import com.confeitaria.canalvenda.persistence.CanalVenda;
import com.confeitaria.canalvenda.persistence.CanalVendaRepository;
import com.confeitaria.configuracao.application.ConfiguracaoService;
import com.confeitaria.configuracao.persistence.Bandeira;
import com.confeitaria.configuracao.persistence.ConfiguracaoCustos;
import com.confeitaria.custofixo.persistence.CustoFixoRepository;
import com.confeitaria.precificacao.api.PrecificacaoDtos.PrecoResponse;
import com.confeitaria.precificacao.api.PrecificacaoDtos.SimulacaoRequest;
import com.confeitaria.precificacao.domain.CalculadoraPreco;
import com.confeitaria.precificacao.domain.ParametrosPrecificacao;
import com.confeitaria.precificacao.domain.ParametrosPrecificacao.CanalTaxa;
import com.confeitaria.precificacao.domain.ParametrosPrecificacao.Energia;
import com.confeitaria.precificacao.domain.ParametrosPrecificacao.Gas;
import com.confeitaria.precificacao.domain.ParametrosPrecificacao.ItemCusto;
import com.confeitaria.precificacao.domain.ParametrosPrecificacao.MaoDeObra;
import com.confeitaria.precificacao.domain.ParametrosPrecificacao.RateioCustosFixos;
import com.confeitaria.precificacao.domain.ParametrosPrecificacao.UsoEquipamento;
import com.confeitaria.precificacao.domain.PrecificacaoException;
import com.confeitaria.receita.persistence.Receita;
import com.confeitaria.receita.persistence.ReceitaRepository;
import com.confeitaria.shared.domain.Decimais;
import com.confeitaria.shared.error.RecursoNaoEncontradoException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Monta a fotografia dos dados atuais (custos de insumos, tarifas vigentes, custos fixos, canais)
 * e delega o cálculo ao domínio puro. Nada é persistido: o preço é sempre recalculado, então
 * qualquer alteração de custo de insumo reflete imediatamente nas receitas.
 */
@Service
public class PrecificacaoService {

    private final ReceitaRepository receitas;
    private final ConfiguracaoService configuracaoService;
    private final CustoFixoRepository custosFixos;
    private final CanalVendaRepository canais;
    private final CanalVendaService canalVendaService;
    private final CalculadoraPreco calculadora = new CalculadoraPreco();

    public PrecificacaoService(ReceitaRepository receitas, ConfiguracaoService configuracaoService,
            CustoFixoRepository custosFixos, CanalVendaRepository canais, CanalVendaService canalVendaService) {
        this.receitas = receitas;
        this.configuracaoService = configuracaoService;
        this.custosFixos = custosFixos;
        this.canais = canais;
        this.canalVendaService = canalVendaService;
    }

    @Transactional
    public PrecoResponse calcular(UUID ownerId, UUID receitaId) {
        return executar(ownerId, receitaId, SimulacaoRequest.vazia(), false);
    }

    /** Mesmo cálculo com parâmetros alternativos. Não grava nada. */
    @Transactional
    public PrecoResponse simular(UUID ownerId, UUID receitaId, SimulacaoRequest simulacao) {
        return executar(ownerId, receitaId, simulacao, true);
    }

    private PrecoResponse executar(UUID ownerId, UUID receitaId, SimulacaoRequest s, boolean simulacao) {
        Receita receita = receitas.findByIdAndOwnerId(receitaId, ownerId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Receita"));
        ConfiguracaoCustos config = configuracaoService.obterEntidade(ownerId);
        Bandeira bandeira = s.bandeira() != null ? s.bandeira() : config.getBandeiraVigente();
        BigDecimal margem = s.margemLucro() != null ? s.margemLucro() : receita.getMargemLucro();

        ParametrosPrecificacao parametros = new ParametrosPrecificacao(
                receita.getRendimento(),
                receita.getTempoPreparoMin(),
                receita.getMinutosGas(),
                Decimais.percentualParaFracao(margem),
                receita.getInsumos().stream().map(i -> new ItemCusto(i.getInsumo().getNome(),
                        i.getInsumo().unidadeBase().name().toLowerCase(), i.getQuantidade(), i.getBase(),
                        i.getInsumo().getCustoUnitario())).toList(),
                receita.getEmbalagens().stream().map(e -> new ItemCusto(e.getEmbalagem().getNome(), "un",
                        e.getQuantidade(), e.getBase(), e.getEmbalagem().getCustoUnitario())).toList(),
                receita.getEquipamentos().stream().map(e -> new UsoEquipamento(e.getEquipamento().getNome(),
                        e.getEquipamento().getPotenciaWatts(), e.getMinutosUso())).toList(),
                new MaoDeObra(config.getSalarioDesejadoMes(), config.getHorasTrabalhadasMes()),
                receita.getEquipamentos().isEmpty() ? null : energia(ownerId, bandeira, s.tarifaKwh()),
                config.getGasPrecoBotijao() == null ? null
                        : new Gas(config.getGasPrecoBotijao(), config.getGasDuracaoHoras()),
                new RateioCustosFixos(RateioCustosFixos.Criterio.valueOf(config.getCriterioRateio().name()),
                        custosFixos.totalMensalAtivo(ownerId), config.getHorasTrabalhadasMes(),
                        receitas.totalVendasEstimadasMes(ownerId)),
                canais(ownerId, s));

        return PrecoResponse.of(receita.getId(), receita.getNome(), simulacao, bandeira, calculadora.calcular(parametros));
    }

    private Energia energia(UUID ownerId, Bandeira bandeira, BigDecimal tarifaSimulada) {
        LocalDate hoje = configuracaoService.hoje();
        BigDecimal tarifa = tarifaSimulada != null ? tarifaSimulada
                : configuracaoService.valorTarifaVigente(ownerId, hoje).orElseThrow(() -> new PrecificacaoException(
                        "A receita usa equipamentos elétricos: cadastre a tarifa do kWh em /configuracao/tarifas."));
        BigDecimal adicional = configuracaoService.valorAdicionalVigente(ownerId, bandeira, hoje)
                .orElseThrow(() -> new PrecificacaoException("Cadastre o adicional vigente da bandeira " + bandeira
                        + " em /configuracao/bandeiras."));
        return new Energia(tarifa, adicional);
    }

    private List<CanalTaxa> canais(UUID ownerId, SimulacaoRequest s) {
        if (s.canalVendaId() == null && s.taxaPercentual() == null) {
            return canais.findByOwnerIdAndAtivoTrueOrderByNome(ownerId).stream().map(PrecificacaoService::taxa).toList();
        }
        List<CanalTaxa> lista = new ArrayList<>();
        if (s.canalVendaId() != null) {
            lista.add(taxa(canalVendaService.obter(ownerId, s.canalVendaId())));
        }
        if (s.taxaPercentual() != null) {
            lista.add(new CanalTaxa(null, "Taxa simulada", Decimais.percentualParaFracao(s.taxaPercentual())));
        }
        return lista;
    }

    private static CanalTaxa taxa(CanalVenda c) {
        return new CanalTaxa(c.getId(), c.getNome(), Decimais.percentualParaFracao(c.getTaxaPercentual()));
    }
}
