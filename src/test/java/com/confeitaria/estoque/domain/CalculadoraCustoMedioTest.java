package com.confeitaria.estoque.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CalculadoraCustoMedioTest {

    private final CalculadoraCustoMedio calculadora = new CalculadoraCustoMedio();

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    @Test
    void ponderaSaldoAtualECompra() {
        // 1400 g a R$ 0,006 (R$ 8,40) + 5000 g por R$ 27,50 = R$ 35,90 / 6400 g
        assertThat(calculadora.calcular(d("1400"), d("0.006"), d("5000"), d("27.50")))
                .isEqualByComparingTo("0.005609375");
    }

    @Test
    void semSaldoAssumeOCustoDaCompra() {
        assertThat(calculadora.calcular(BigDecimal.ZERO, d("0.006"), d("1000"), d("7.00")))
                .isEqualByComparingTo("0.007");
    }

    @Test
    void saldoNegativoNaoEntraNaPonderacao() {
        assertThat(calculadora.calcular(d("-200"), d("0.006"), d("1000"), d("7.00")))
                .isEqualByComparingTo("0.007");
    }

    @Test
    void compraGratuitaDiluiOCusto() {
        assertThat(calculadora.calcular(d("1000"), d("0.01"), d("1000"), BigDecimal.ZERO))
                .isEqualByComparingTo("0.005");
    }

    @Test
    void quantidadeDeveSerPositiva() {
        assertThatThrownBy(() -> calculadora.calcular(d("1"), d("1"), BigDecimal.ZERO, d("1")))
                .isInstanceOf(EstoqueException.class);
    }

    @Test
    void valorNaoPodeSerNegativo() {
        assertThatThrownBy(() -> calculadora.calcular(d("1"), d("1"), d("1"), d("-1")))
                .isInstanceOf(EstoqueException.class);
    }
}
