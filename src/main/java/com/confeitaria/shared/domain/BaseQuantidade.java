package com.confeitaria.shared.domain;

import java.math.BigDecimal;

/**
 * Como a quantidade de um item da receita é informada.
 * <ul>
 *   <li>{@code POR_LOTE}: quantidade total para a receita inteira (ex.: 1 base de papelão por bolo).</li>
 *   <li>{@code POR_UNIDADE}: quantidade para cada unidade produzida (ex.: 1 forminha por brigadeiro);
 *       é multiplicada pelo rendimento.</li>
 * </ul>
 */
public enum BaseQuantidade {
    POR_LOTE,
    POR_UNIDADE;

    public BigDecimal quantidadePorLote(BigDecimal quantidade, int rendimento) {
        return this == POR_UNIDADE ? quantidade.multiply(BigDecimal.valueOf(rendimento)) : quantidade;
    }
}
