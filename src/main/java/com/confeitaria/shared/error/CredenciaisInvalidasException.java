package com.confeitaria.shared.error;

/** Login, refresh ou logout com credenciais inválidas (HTTP 401). */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException(String detalhe) {
        super(detalhe);
    }
}
