package com.confeitaria.estoque.application;

import com.confeitaria.configuracao.application.ConfiguracaoService;
import com.confeitaria.embalagem.persistence.EmbalagemRepository;
import com.confeitaria.estoque.api.EstoqueDtos.AjusteRequest;
import com.confeitaria.estoque.api.EstoqueDtos.CompraRequest;
import com.confeitaria.estoque.api.EstoqueDtos.CompraResponse;
import com.confeitaria.estoque.api.EstoqueDtos.ItemAbaixoMinimoResponse;
import com.confeitaria.estoque.api.EstoqueDtos.MovimentacaoResponse;
import com.confeitaria.estoque.domain.CalculadoraCustoMedio;
import com.confeitaria.estoque.domain.EstoqueException;
import com.confeitaria.estoque.domain.TipoItem;
import com.confeitaria.estoque.persistence.MotivoMovimentacao;
import com.confeitaria.estoque.persistence.MovimentacaoEstoque;
import com.confeitaria.estoque.persistence.MovimentacaoEstoqueRepository;
import com.confeitaria.estoque.persistence.MovimentacaoSpecs;
import com.confeitaria.estoque.persistence.TipoMovimentacao;
import com.confeitaria.insumo.persistence.Insumo;
import com.confeitaria.insumo.persistence.InsumoRepository;
import com.confeitaria.shared.domain.Decimais;
import com.confeitaria.shared.domain.UnidadeMedida;
import com.confeitaria.shared.error.RecursoNaoEncontradoException;
import com.confeitaria.shared.persistence.ItemEstoque;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Compras, ajustes e consultas de estoque. O saldo só muda aqui e na produção, sempre com movimentação. */
@Service
public class EstoqueService {

    private final InsumoRepository insumos;
    private final EmbalagemRepository embalagens;
    private final MovimentacaoEstoqueRepository movimentacoes;
    private final ConfiguracaoService configuracaoService;
    private final ExecutorComRetry executor;
    private final Clock clock;
    private final CalculadoraCustoMedio custoMedio = new CalculadoraCustoMedio();

    public EstoqueService(InsumoRepository insumos, EmbalagemRepository embalagens,
            MovimentacaoEstoqueRepository movimentacoes, ConfiguracaoService configuracaoService,
            ExecutorComRetry executor, Clock clock) {
        this.insumos = insumos;
        this.embalagens = embalagens;
        this.movimentacoes = movimentacoes;
        this.configuracaoService = configuracaoService;
        this.executor = executor;
        this.clock = clock;
    }

    /** ENTRADA no estoque e atualização do custo pelo custo médio ponderado. */
    public CompraResponse registrarCompra(UUID ownerId, CompraRequest r) {
        return executor.executar(() -> {
            ItemEstoque item = buscarItem(ownerId, r.insumoId(), r.embalagemId());
            BigDecimal quantidade = paraBase(item, r.quantidade(), r.unidade());
            BigDecimal saldoAnterior = item.getSaldo();
            BigDecimal custoAnterior = item.getCustoUnitario();
            BigDecimal novoCusto = custoMedio.calcular(saldoAnterior, custoAnterior, quantidade, r.valorPago());

            item.registrarEntrada(quantidade, novoCusto);
            if (item instanceof Insumo insumo) {
                insumo.registrarUltimaCompra(quantidade, r.valorPago());
            }
            BigDecimal custoDaCompra = Decimais.dividir(r.valorPago(), quantidade);
            MovimentacaoEstoque mov = movimentacoes.save(new MovimentacaoEstoque(item, TipoMovimentacao.ENTRADA,
                    MotivoMovimentacao.COMPRA, quantidade, custoDaCompra, Decimais.monetario(r.valorPago()),
                    r.observacao(), null, ownerId, clock.instant()));
            return new CompraResponse(MovimentacaoResponse.of(mov), saldoAnterior, item.getSaldo(),
                    custoAnterior.setScale(6, Decimais.ARREDONDAMENTO),
                    item.getCustoUnitario().setScale(6, Decimais.ARREDONDAMENTO));
        });
    }

    /** AJUSTE (perda, vencimento, inventário). Não altera o custo unitário. */
    public MovimentacaoResponse registrarAjuste(UUID ownerId, AjusteRequest r) {
        return executor.executar(() -> {
            ItemEstoque item = buscarItem(ownerId, r.insumoId(), r.embalagemId());
            BigDecimal delta = paraBase(item, r.quantidade(), r.unidade());
            boolean permitirNegativo = configuracaoService.obterEntidade(ownerId).isPermitirSaldoNegativo();
            if (!permitirNegativo && item.getSaldo().add(delta).signum() < 0) {
                throw new EstoqueException("O ajuste deixaria o saldo de '" + item.getNome() + "' negativo (saldo atual: "
                        + item.getSaldo().stripTrailingZeros().toPlainString() + ").");
            }
            item.aplicarVariacao(delta);
            MovimentacaoEstoque mov = movimentacoes.save(new MovimentacaoEstoque(item, TipoMovimentacao.AJUSTE,
                    r.motivo(), delta, item.getCustoUnitario(), null, r.observacao(), null, ownerId, clock.instant()));
            return MovimentacaoResponse.of(mov);
        });
    }

    @Transactional(readOnly = true)
    public Page<MovimentacaoResponse> listarMovimentacoes(UUID ownerId, TipoMovimentacao tipo, UUID insumoId,
            UUID embalagemId, LocalDate de, LocalDate ate, Pageable pageable) {
        Instant inicio = de == null ? null : de.atStartOfDay(ConfiguracaoService.ZONA).toInstant();
        Instant fim = ate == null ? null : ate.plusDays(1).atStartOfDay(ConfiguracaoService.ZONA).toInstant();
        return movimentacoes.findAll(MovimentacaoSpecs.filtrar(ownerId, tipo, insumoId, embalagemId, inicio, fim),
                pageable).map(MovimentacaoResponse::of);
    }

    @Transactional(readOnly = true)
    public List<ItemAbaixoMinimoResponse> abaixoDoMinimo(UUID ownerId) {
        return Stream.concat(
                insumos.abaixoDoMinimo(ownerId).stream().map(i -> abaixo(TipoItem.INSUMO, i)),
                embalagens.abaixoDoMinimo(ownerId).stream().map(e -> abaixo(TipoItem.EMBALAGEM, e)))
                .toList();
    }

    private static ItemAbaixoMinimoResponse abaixo(TipoItem tipo, ItemEstoque item) {
        return new ItemAbaixoMinimoResponse(tipo, item.getId(), item.getNome(), item.unidadeBase().name().toLowerCase(),
                item.getSaldo(), item.getEstoqueMinimo(), item.getEstoqueMinimo().subtract(item.getSaldo()));
    }

    private ItemEstoque buscarItem(UUID ownerId, UUID insumoId, UUID embalagemId) {
        ItemEstoque item = insumoId != null
                ? insumos.findByIdAndOwnerId(insumoId, ownerId).orElseThrow(() -> new RecursoNaoEncontradoException("Insumo"))
                : embalagens.findByIdAndOwnerId(embalagemId, ownerId)
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Embalagem"));
        if (!item.isAtivo()) {
            throw new EstoqueException("'" + item.getNome() + "' está inativo(a).");
        }
        return item;
    }

    private static BigDecimal paraBase(ItemEstoque item, BigDecimal quantidade, UnidadeMedida unidade) {
        if (unidade == null) {
            return quantidade;
        }
        if (!unidade.compativelCom(item.unidadeBase())) {
            throw new EstoqueException("Unidade " + unidade + " incompatível com '" + item.getNome() + "' (unidade base "
                    + item.unidadeBase() + ").");
        }
        return unidade.paraBase(quantidade);
    }
}
