package com.confeitaria.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regras únicas de escala e arredondamento. Todo cálculo intermediário usa
 * {@link #ESCALA_CALCULO}; apenas valores apresentados ao usuário são
 * arredondados para {@link #ESCALA_MONETARIA}.
 */
public final class Decimais {

    public static final int ESCALA_CALCULO = 10;
    public static final int ESCALA_QUANTIDADE = 6;
    public static final int ESCALA_MONETARIA = 2;
    public static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_EVEN;

    public static final BigDecimal CEM = BigDecimal.valueOf(100);
    public static final BigDecimal MIL = BigDecimal.valueOf(1000);
    public static final BigDecimal SESSENTA = BigDecimal.valueOf(60);

    private Decimais() {
    }

    public static BigDecimal dividir(BigDecimal dividendo, BigDecimal divisor) {
        return dividendo.divide(divisor, ESCALA_CALCULO, ARREDONDAMENTO);
    }

    public static BigDecimal calculo(BigDecimal valor) {
        return valor.setScale(ESCALA_CALCULO, ARREDONDAMENTO);
    }

    public static BigDecimal monetario(BigDecimal valor) {
        return valor.setScale(ESCALA_MONETARIA, ARREDONDAMENTO);
    }

    public static BigDecimal quantidade(BigDecimal valor) {
        return valor.setScale(ESCALA_QUANTIDADE, ARREDONDAMENTO);
    }

    /** 30.00 (percentual informado pela usuária) vira 0.30. */
    public static BigDecimal percentualParaFracao(BigDecimal percentual) {
        return dividir(percentual, CEM);
    }

    /** 0.30 vira 30.00. */
    public static BigDecimal fracaoParaPercentual(BigDecimal fracao) {
        return monetario(fracao.multiply(CEM));
    }

    public static BigDecimal minutosParaHoras(int minutos) {
        return dividir(BigDecimal.valueOf(minutos), SESSENTA);
    }
}
