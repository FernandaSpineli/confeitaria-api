package com.confeitaria.shared.error;

/**
 * Recurso inexistente ou pertencente a outra usuária. As duas situações geram
 * a mesma resposta (404) para não revelar a existência de dados alheios.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String recurso) {
        super(recurso + " não encontrado(a).");
    }
}
