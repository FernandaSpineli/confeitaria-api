package com.confeitaria.precificacao.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/** margem + taxa ≥ 100%: o denominador da fórmula fica zero ou negativo. */
public class MargemInviavelException extends PrecificacaoException {

    public record CanalInviavel(String canal, BigDecimal margemPercentual, BigDecimal taxaPercentual,
            BigDecimal somaPercentual) {
    }

    private final transient List<CanalInviavel> canaisInvalidos;

    public MargemInviavelException(List<CanalInviavel> canaisInvalidos) {
        super(canaisInvalidos.stream()
                .map(c -> "margem (" + c.margemPercentual().toPlainString() + "%) + taxa do canal '" + c.canal()
                        + "' (" + c.taxaPercentual().toPlainString() + "%) = " + c.somaPercentual().toPlainString() + "%")
                .collect(Collectors.joining("; ", "A soma de margem e taxas deve ser menor que 100%: ", ".")));
        this.canaisInvalidos = List.copyOf(canaisInvalidos);
    }

    public List<CanalInviavel> canaisInvalidos() {
        return canaisInvalidos;
    }
}
