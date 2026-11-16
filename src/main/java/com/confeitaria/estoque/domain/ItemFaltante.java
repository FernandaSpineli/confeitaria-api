package com.confeitaria.estoque.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemFaltante(TipoItem tipo, UUID itemId, String item, String unidade, BigDecimal necessario,
        BigDecimal saldo, BigDecimal faltam) {
}
