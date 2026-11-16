package com.confeitaria.shared.error;

/** Conflito de estado (HTTP 409). */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String detalhe) {
        super(detalhe);
    }
}
