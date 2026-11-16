package com.confeitaria.estoque.domain;

/** Operação de estoque inválida. */
public class EstoqueException extends RuntimeException {

    public EstoqueException(String mensagem) {
        super(mensagem);
    }
}
