package com.confeitaria.precificacao.domain;

/** Dados insuficientes ou inválidos para calcular o preço. */
public class PrecificacaoException extends RuntimeException {

    public PrecificacaoException(String mensagem) {
        super(mensagem);
    }
}
