package com.confeitaria.estoque.application;

import com.confeitaria.configuracao.application.ConfiguracaoService;
import com.confeitaria.estoque.api.EstoqueDtos.BaixaResponse;
import com.confeitaria.estoque.api.EstoqueDtos.CapacidadeResponse;
import com.confeitaria.estoque.api.EstoqueDtos.FatorLimitante;
import com.confeitaria.estoque.api.EstoqueDtos.ProducaoRequest;
import com.confeitaria.estoque.api.EstoqueDtos.ProducaoResponse;
import com.confeitaria.estoque.domain.Baixa;
import com.confeitaria.estoque.domain.CalculadoraCapacidade;
import com.confeitaria.estoque.domain.CalculadoraCapacidade.Capacidade;
import com.confeitaria.estoque.domain.Necessidade;
import com.confeitaria.estoque.domain.PlanejadorBaixa;
import com.confeitaria.estoque.domain.TipoItem;
import com.confeitaria.estoque.persistence.MotivoMovimentacao;
import com.confeitaria.estoque.persistence.MovimentacaoEstoque;
import com.confeitaria.estoque.persistence.MovimentacaoEstoqueRepository;
import com.confeitaria.estoque.persistence.Producao;
import com.confeitaria.estoque.persistence.ProducaoRepository;
import com.confeitaria.estoque.persistence.TipoMovimentacao;
import com.confeitaria.receita.persistence.Receita;
import com.confeitaria.receita.persistence.ReceitaRepository;
import com.confeitaria.shared.domain.Decimais;
import com.confeitaria.shared.error.RecursoNaoEncontradoException;
import com.confeitaria.shared.persistence.ItemEstoque;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro de produção: baixa todos os insumos e embalagens de N lotes numa única transação.
 * Ou baixa tudo, ou nada (estoque insuficiente → 422 com a lista de faltantes).
 */
@Service
public class ProducaoService {

    private final ReceitaRepository receitas;
    private final ProducaoRepository producoes;
    private final MovimentacaoEstoqueRepository movimentacoes;
    private final ConfiguracaoService configuracaoService;
    private final ExecutorComRetry executor;
    private final Clock clock;
    private final PlanejadorBaixa planejador = new PlanejadorBaixa();
    private final CalculadoraCapacidade calculadoraCapacidade = new CalculadoraCapacidade();

    public ProducaoService(ReceitaRepository receitas, ProducaoRepository producoes,
            MovimentacaoEstoqueRepository movimentacoes, ConfiguracaoService configuracaoService,
            ExecutorComRetry executor, Clock clock) {
        this.receitas = receitas;
        this.producoes = producoes;
        this.movimentacoes = movimentacoes;
        this.configuracaoService = configuracaoService;
        this.executor = executor;
        this.clock = clock;
    }

    public ProducaoResponse produzir(UUID ownerId, ProducaoRequest request) {
        return executor.executar(() -> {
            Receita receita = receitaAtiva(ownerId, request.receitaId());
            boolean permitirNegativo = configuracaoService.obterEntidade(ownerId).isPermitirSaldoNegativo();
            Map<String, ItemEstoque> itens = new HashMap<>();
            List<Baixa> baixas = planejador.planejar(necessidades(receita, itens), request.lotes(), permitirNegativo);

            Instant agora = clock.instant();
            BigDecimal custoMateriais = BigDecimal.ZERO;
            for (Baixa b : baixas) {
                ItemEstoque item = itens.get(chave(b.tipo(), b.itemId()));
                item.aplicarVariacao(b.quantidade().negate());
                custoMateriais = custoMateriais.add(b.quantidade().multiply(item.getCustoUnitario()));
            }
            Producao producao = producoes.save(new Producao(ownerId, receita, request.lotes(),
                    Decimais.monetario(custoMateriais), ownerId, agora));
            List<BaixaResponse> respostas = new ArrayList<>();
            for (Baixa b : baixas) {
                ItemEstoque item = itens.get(chave(b.tipo(), b.itemId()));
                movimentacoes.save(new MovimentacaoEstoque(item, TipoMovimentacao.SAIDA, MotivoMovimentacao.PRODUCAO,
                        b.quantidade().negate(), item.getCustoUnitario(), null,
                        request.lotes() + " lote(s) de " + receita.getNome(), producao, ownerId, agora));
                respostas.add(new BaixaResponse(b.tipo(), b.itemId(), b.nome(), b.unidade(), b.quantidade(),
                        item.getSaldo()));
            }
            return new ProducaoResponse(producao.getId(), receita.getId(), receita.getNome(), request.lotes(),
                    request.lotes() * receita.getRendimento(), producao.getCustoMateriais(), agora, respostas);
        });
    }

    @Transactional(readOnly = true)
    public CapacidadeResponse capacidade(UUID ownerId, UUID receitaId) {
        Receita receita = receitaAtiva(ownerId, receitaId);
        Capacidade c = calculadoraCapacidade.calcular(necessidades(receita, new HashMap<>()));
        Necessidade n = c.limitante();
        return new CapacidadeResponse(receita.getId(), receita.getNome(), c.lotesPossiveis(),
                c.lotesPossiveis() * receita.getRendimento(),
                new FatorLimitante(n.tipo(), n.itemId(), n.nome(), n.unidade(), n.quantidadePorLote(), n.saldo()));
    }

    @Transactional(readOnly = true)
    public Page<ProducaoResponse> listar(UUID ownerId, Pageable pageable) {
        return producoes.findByOwnerId(ownerId, pageable).map(p -> toResponse(p, List.of()));
    }

    @Transactional(readOnly = true)
    public ProducaoResponse buscar(UUID ownerId, UUID id) {
        Producao p = producoes.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produção"));
        List<BaixaResponse> baixas = movimentacoes.findByProducaoIdOrderByCriadoEm(id).stream()
                .map(m -> new BaixaResponse(m.tipoItem(), m.item().getId(), m.item().getNome(),
                        m.item().unidadeBase().name().toLowerCase(), m.getQuantidade().negate(), m.getSaldoApos()))
                .toList();
        return toResponse(p, baixas);
    }

    private static ProducaoResponse toResponse(Producao p, List<BaixaResponse> baixas) {
        Receita r = p.getReceita();
        return new ProducaoResponse(p.getId(), r.getId(), r.getNome(), p.getLotes(), p.getLotes() * r.getRendimento(),
                p.getCustoMateriais(), p.getCriadoEm(), baixas);
    }

    private Receita receitaAtiva(UUID ownerId, UUID receitaId) {
        return receitas.findByIdAndOwnerId(receitaId, ownerId)
                .filter(Receita::isAtivo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Receita"));
    }

    /** Converte os itens da receita em necessidades por lote e indexa as entidades para a baixa. */
    private static List<Necessidade> necessidades(Receita receita, Map<String, ItemEstoque> itens) {
        int rendimento = receita.getRendimento();
        List<Necessidade> lista = new ArrayList<>();
        receita.getInsumos().forEach(ri -> {
            ItemEstoque item = ri.getInsumo();
            itens.put(chave(TipoItem.INSUMO, item.getId()), item);
            lista.add(new Necessidade(TipoItem.INSUMO, item.getId(), item.getNome(),
                    item.unidadeBase().name().toLowerCase(), ri.getBase().quantidadePorLote(ri.getQuantidade(), rendimento),
                    item.getSaldo()));
        });
        receita.getEmbalagens().forEach(re -> {
            ItemEstoque item = re.getEmbalagem();
            itens.put(chave(TipoItem.EMBALAGEM, item.getId()), item);
            lista.add(new Necessidade(TipoItem.EMBALAGEM, item.getId(), item.getNome(), "un",
                    re.getBase().quantidadePorLote(re.getQuantidade(), rendimento), item.getSaldo()));
        });
        return lista;
    }

    private static String chave(TipoItem tipo, UUID id) {
        return tipo + ":" + id;
    }
}
