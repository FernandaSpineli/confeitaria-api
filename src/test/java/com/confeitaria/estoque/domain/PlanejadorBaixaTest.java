package com.confeitaria.estoque.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlanejadorBaixaTest {

    private final PlanejadorBaixa planejador = new PlanejadorBaixa();

    private static final UUID FARINHA = UUID.randomUUID();
    private static final UUID OVOS = UUID.randomUUID();
    private static final UUID CAIXA = UUID.randomUUID();

    private static Necessidade n(TipoItem tipo, UUID id, String nome, String porLote, String saldo) {
        return new Necessidade(tipo, id, nome, tipo == TipoItem.INSUMO ? "g" : "un", new BigDecimal(porLote),
                new BigDecimal(saldo));
    }

    @Test
    void baixaProporcionalAoNumeroDeLotes() {
        List<Baixa> baixas = planejador.planejar(List.of(
                n(TipoItem.INSUMO, FARINHA, "Farinha", "300", "2000"),
                n(TipoItem.EMBALAGEM, CAIXA, "Caixa", "10", "50")), 2, false);

        assertThat(baixas).hasSize(2);
        assertThat(baixas.get(0).quantidade()).isEqualByComparingTo("600");
        assertThat(baixas.get(0).saldoPosterior()).isEqualByComparingTo("1400");
        assertThat(baixas.get(1).quantidade()).isEqualByComparingTo("20");
        assertThat(baixas.get(1).saldoPosterior()).isEqualByComparingTo("30");
    }

    @Test
    void saldoExatoZeraOEstoqueSemErro() {
        List<Baixa> baixas = planejador.planejar(List.of(n(TipoItem.INSUMO, OVOS, "Ovos", "4", "8")), 2, false);
        assertThat(baixas.getFirst().saldoPosterior()).isEqualByComparingTo("0");
    }

    @Test
    void estoqueInsuficienteListaTodosOsItensFaltantes() {
        assertThatThrownBy(() -> planejador.planejar(List.of(
                n(TipoItem.INSUMO, FARINHA, "Farinha", "300", "2000"),
                n(TipoItem.INSUMO, OVOS, "Ovos", "4", "6"),
                n(TipoItem.EMBALAGEM, CAIXA, "Caixa", "1", "0")), 2, false))
                .isInstanceOfSatisfying(EstoqueInsuficienteException.class, e -> {
                    assertThat(e.itensFaltantes()).extracting(ItemFaltante::item).containsExactly("Ovos", "Caixa");
                    ItemFaltante ovos = e.itensFaltantes().getFirst();
                    assertThat(ovos.necessario()).isEqualByComparingTo("8");
                    assertThat(ovos.saldo()).isEqualByComparingTo("6");
                    assertThat(ovos.faltam()).isEqualByComparingTo("2");
                })
                .hasMessageContaining("Ovos (faltam 2 g)").hasMessageContaining("Caixa (faltam 2 un)");
    }

    @Test
    void permitirSaldoNegativoLiberaAProducao() {
        List<Baixa> baixas = planejador.planejar(List.of(n(TipoItem.INSUMO, OVOS, "Ovos", "4", "6")), 2, true);
        assertThat(baixas.getFirst().saldoPosterior()).isEqualByComparingTo("-2");
    }

    @Test
    void itensRepetidosNaReceitaSaoSomados() {
        List<Baixa> baixas = planejador.planejar(List.of(
                n(TipoItem.INSUMO, FARINHA, "Farinha", "300", "1000"),
                n(TipoItem.INSUMO, FARINHA, "Farinha", "50", "1000")), 2, false);
        assertThat(baixas).singleElement().satisfies(b -> {
            assertThat(b.quantidade()).isEqualByComparingTo("700");
            assertThat(b.saldoPosterior()).isEqualByComparingTo("300");
        });
    }

    @Test
    void somaDosRepetidosTambemEhValidadaContraOSaldo() {
        assertThatThrownBy(() -> planejador.planejar(List.of(
                n(TipoItem.INSUMO, FARINHA, "Farinha", "300", "500"),
                n(TipoItem.INSUMO, FARINHA, "Farinha", "300", "500")), 1, false))
                .isInstanceOf(EstoqueInsuficienteException.class);
    }

    @Test
    void mesmoIdComTiposDiferentesNaoSaoAgrupados() {
        UUID id = UUID.randomUUID();
        List<Baixa> baixas = planejador.planejar(List.of(
                n(TipoItem.INSUMO, id, "X", "1", "10"), n(TipoItem.EMBALAGEM, id, "Y", "1", "10")), 1, false);
        assertThat(baixas).hasSize(2);
    }

    @Test
    void quantidadesFracionadasMantemPrecisao() {
        List<Baixa> baixas = planejador.planejar(List.of(n(TipoItem.INSUMO, FARINHA, "Fermento", "0.333333", "1")), 3,
                false);
        assertThat(baixas.getFirst().quantidade()).isEqualByComparingTo("0.999999");
        assertThat(baixas.getFirst().saldoPosterior()).isEqualByComparingTo("0.000001");
    }

    @Test
    void lotesDevemSerPositivos() {
        assertThatThrownBy(() -> planejador.planejar(List.of(n(TipoItem.INSUMO, FARINHA, "Farinha", "1", "1")), 0, false))
                .isInstanceOf(EstoqueException.class).hasMessageContaining("lotes");
    }

    @Test
    void receitaSemItensNaoPodeSerBaixada() {
        assertThatThrownBy(() -> planejador.planejar(List.of(), 1, false)).isInstanceOf(EstoqueException.class);
    }
}
