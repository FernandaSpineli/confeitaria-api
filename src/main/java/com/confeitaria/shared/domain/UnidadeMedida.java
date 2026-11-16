package com.confeitaria.shared.domain;

import java.math.BigDecimal;

/** Unidades aceitas na API e sua conversão para a unidade base. */
public enum UnidadeMedida {
    G(UnidadeBase.G, BigDecimal.ONE),
    KG(UnidadeBase.G, Decimais.MIL),
    ML(UnidadeBase.ML, BigDecimal.ONE),
    L(UnidadeBase.ML, Decimais.MIL),
    UN(UnidadeBase.UN, BigDecimal.ONE);

    private final UnidadeBase base;
    private final BigDecimal fator;

    UnidadeMedida(UnidadeBase base, BigDecimal fator) {
        this.base = base;
        this.fator = fator;
    }

    public UnidadeBase base() {
        return base;
    }

    public boolean compativelCom(UnidadeBase outra) {
        return base == outra;
    }

    public BigDecimal paraBase(BigDecimal quantidade) {
        return quantidade.multiply(fator);
    }
}
