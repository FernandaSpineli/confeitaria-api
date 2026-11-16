package com.confeitaria.estoque.persistence;

public enum MotivoMovimentacao {
    COMPRA, PRODUCAO, PERDA, VENCIMENTO, INVENTARIO, OUTRO;

    public boolean permitidoEmAjuste() {
        return this != COMPRA && this != PRODUCAO;
    }
}
