package com.confeitaria.shared.error;

import java.util.Map;

/** Violação de regra de negócio (HTTP 422). */
public class RegraNegocioException extends RuntimeException {

    private final String tipo;
    private final String titulo;
    private final Map<String, Object> extras;

    public RegraNegocioException(String tipo, String titulo, String detalhe, Map<String, Object> extras) {
        super(detalhe);
        this.tipo = tipo;
        this.titulo = titulo;
        this.extras = Map.copyOf(extras);
    }

    public RegraNegocioException(String detalhe) {
        this("regra-negocio", "Regra de negócio violada", detalhe, Map.of());
    }

    public String tipo() {
        return tipo;
    }

    public String titulo() {
        return titulo;
    }

    public Map<String, Object> extras() {
        return extras;
    }
}
