package com.confeitaria.estoque.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Saída planejada de um item. {@code quantidade} é positiva; o saldo diminui nesse valor. */
public record Baixa(TipoItem tipo, UUID itemId, String nome, String unidade, BigDecimal quantidade,
        BigDecimal saldoAnterior, BigDecimal saldoPosterior) {
}
