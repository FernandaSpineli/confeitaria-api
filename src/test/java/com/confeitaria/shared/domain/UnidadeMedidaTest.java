package com.confeitaria.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class UnidadeMedidaTest {

    @Test
    void converteParaUnidadeBase() {
        assertThat(UnidadeMedida.KG.paraBase(new BigDecimal("1.5"))).isEqualByComparingTo("1500");
        assertThat(UnidadeMedida.L.paraBase(new BigDecimal("0.9"))).isEqualByComparingTo("900");
        assertThat(UnidadeMedida.G.paraBase(new BigDecimal("80"))).isEqualByComparingTo("80");
        assertThat(UnidadeMedida.UN.paraBase(new BigDecimal("30"))).isEqualByComparingTo("30");
    }

    @Test
    void compatibilidadeEntreUnidades() {
        assertThat(UnidadeMedida.KG.compativelCom(UnidadeBase.G)).isTrue();
        assertThat(UnidadeMedida.L.compativelCom(UnidadeBase.ML)).isTrue();
        assertThat(UnidadeMedida.KG.compativelCom(UnidadeBase.ML)).isFalse();
        assertThat(UnidadeMedida.UN.compativelCom(UnidadeBase.G)).isFalse();
    }

    @Test
    void basePorUnidadeMultiplicaPeloRendimento() {
        assertThat(BaseQuantidade.POR_UNIDADE.quantidadePorLote(BigDecimal.ONE, 30)).isEqualByComparingTo("30");
        assertThat(BaseQuantidade.POR_LOTE.quantidadePorLote(BigDecimal.ONE, 30)).isEqualByComparingTo("1");
    }
}
