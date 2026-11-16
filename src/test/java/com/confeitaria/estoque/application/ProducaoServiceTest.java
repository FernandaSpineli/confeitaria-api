package com.confeitaria.estoque.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.confeitaria.configuracao.application.ConfiguracaoService;
import com.confeitaria.configuracao.persistence.ConfiguracaoCustos;
import com.confeitaria.embalagem.persistence.Embalagem;
import com.confeitaria.estoque.api.EstoqueDtos.ProducaoRequest;
import com.confeitaria.estoque.api.EstoqueDtos.ProducaoResponse;
import com.confeitaria.estoque.domain.EstoqueInsuficienteException;
import com.confeitaria.estoque.persistence.MovimentacaoEstoque;
import com.confeitaria.estoque.persistence.MovimentacaoEstoqueRepository;
import com.confeitaria.estoque.persistence.Producao;
import com.confeitaria.estoque.persistence.ProducaoRepository;
import com.confeitaria.estoque.persistence.TipoMovimentacao;
import com.confeitaria.insumo.persistence.Insumo;
import com.confeitaria.receita.persistence.Receita;
import com.confeitaria.receita.persistence.ReceitaEmbalagem;
import com.confeitaria.receita.persistence.ReceitaInsumo;
import com.confeitaria.receita.persistence.ReceitaRepository;
import com.confeitaria.shared.domain.BaseQuantidade;
import com.confeitaria.shared.domain.UnidadeBase;
import com.confeitaria.shared.error.RecursoNaoEncontradoException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProducaoServiceTest {

    private static final UUID DONA = UUID.randomUUID();
    private static final Instant AGORA = Instant.parse("2026-11-16T12:00:00Z");

    @Mock ReceitaRepository receitas;
    @Mock ProducaoRepository producoes;
    @Mock MovimentacaoEstoqueRepository movimentacoes;
    @Mock ConfiguracaoService configuracaoService;
    @Mock ExecutorComRetry executor;

    private ProducaoService service;
    private Insumo farinha;
    private Insumo ovos;
    private Embalagem fatia;
    private Receita bolo;

    @BeforeEach
    void setUp() {
        service = new ProducaoService(receitas, producoes, movimentacoes, configuracaoService, executor,
                Clock.fixed(AGORA, ZoneOffset.UTC));

        farinha = comId(new Insumo(DONA, "Farinha", UnidadeBase.G, new BigDecimal("1000"), new BigDecimal("6.00"),
                BigDecimal.ZERO, AGORA));
        farinha.registrarEntrada(new BigDecimal("2000"), farinha.getCustoUnitario());
        ovos = comId(new Insumo(DONA, "Ovos", UnidadeBase.UN, new BigDecimal("30"), new BigDecimal("24.00"),
                BigDecimal.ZERO, AGORA));
        ovos.registrarEntrada(new BigDecimal("6"), ovos.getCustoUnitario());
        fatia = comId(new Embalagem(DONA, "Embalagem de fatia", new BigDecimal("0.90"), BigDecimal.ZERO, AGORA));
        fatia.registrarEntrada(new BigDecimal("100"), fatia.getCustoUnitario());

        bolo = new Receita(DONA, AGORA);
        ReflectionTestUtils.setField(bolo, "id", UUID.randomUUID());
        bolo.atualizarDados("Bolo", 10, 150, 0, new BigDecimal("30"), null);
        bolo.substituirItens(
                List.of(new ReceitaInsumo(farinha, new BigDecimal("300"), BaseQuantidade.POR_LOTE),
                        new ReceitaInsumo(ovos, new BigDecimal("4"), BaseQuantidade.POR_LOTE)),
                List.of(new ReceitaEmbalagem(fatia, BigDecimal.ONE, BaseQuantidade.POR_UNIDADE)), List.of());

        lenient().when(executor.executar(any())).thenAnswer(inv -> inv.<Supplier<?>>getArgument(0).get());
    }

    private static <T> T comId(T entidade) {
        ReflectionTestUtils.setField(entidade, "id", UUID.randomUUID());
        return entidade;
    }

    private void receitaEncontrada() {
        when(receitas.findByIdAndOwnerId(bolo.getId(), DONA)).thenReturn(Optional.of(bolo));
        when(configuracaoService.obterEntidade(DONA)).thenReturn(ConfiguracaoCustos.padrao(DONA));
    }

    @Test
    void produzirDaBaixaEmTodosOsItensERegistraHistorico() {
        receitaEncontrada();
        when(producoes.save(any())).thenAnswer(inv -> comId(inv.getArgument(0)));

        ProducaoResponse r = service.produzir(DONA, new ProducaoRequest(bolo.getId(), 1));

        assertThat(farinha.getSaldo()).isEqualByComparingTo("1700");
        assertThat(ovos.getSaldo()).isEqualByComparingTo("2");
        assertThat(fatia.getSaldo()).isEqualByComparingTo("90");
        assertThat(r.unidadesProduzidas()).isEqualTo(10);
        // 300 × 0,006 + 4 × 0,80 + 10 × 0,90
        assertThat(r.custoMateriais()).isEqualByComparingTo("14.00");

        ArgumentCaptor<MovimentacaoEstoque> mov = ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacoes, times(3)).save(mov.capture());
        assertThat(mov.getAllValues()).allSatisfy(m -> {
            assertThat(m.getTipo()).isEqualTo(TipoMovimentacao.SAIDA);
            assertThat(m.getQuantidade().signum()).isNegative();
            assertThat(m.getProducao()).isNotNull();
        });
        assertThat(mov.getAllValues()).extracting(MovimentacaoEstoque::getSaldoApos)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("1700"), new BigDecimal("2"), new BigDecimal("90"));
    }

    @Test
    void estoqueInsuficienteNaoAlteraNadaNemGravaHistorico() {
        receitaEncontrada();

        assertThatThrownBy(() -> service.produzir(DONA, new ProducaoRequest(bolo.getId(), 2)))
                .isInstanceOfSatisfying(EstoqueInsuficienteException.class, e -> assertThat(e.itensFaltantes())
                        .singleElement().satisfies(f -> {
                            assertThat(f.item()).isEqualTo("Ovos");
                            assertThat(f.faltam()).isEqualByComparingTo("2");
                        }));

        assertThat(farinha.getSaldo()).isEqualByComparingTo("2000");
        assertThat(ovos.getSaldo()).isEqualByComparingTo("6");
        verify(producoes, never()).save(any(Producao.class));
        verify(movimentacoes, never()).save(any());
    }

    @Test
    void receitaDeOutraUsuariaRetornaNaoEncontrada() {
        UUID outra = UUID.randomUUID();
        when(receitas.findByIdAndOwnerId(bolo.getId(), outra)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.produzir(outra, new ProducaoRequest(bolo.getId(), 1)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(movimentacoes, never()).save(any());
    }

    @Test
    void capacidadeIndicaOItemLimitante() {
        when(receitas.findByIdAndOwnerId(bolo.getId(), DONA)).thenReturn(Optional.of(bolo));

        var c = service.capacidade(DONA, bolo.getId());

        assertThat(c.lotesPossiveis()).isEqualTo(1);
        assertThat(c.unidadesPossiveis()).isEqualTo(10);
        assertThat(c.fatorLimitante().item()).isEqualTo("Ovos");
    }
}
