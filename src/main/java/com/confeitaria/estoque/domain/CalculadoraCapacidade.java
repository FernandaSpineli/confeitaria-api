package com.confeitaria.estoque.domain;

import java.math.RoundingMode;
import java.util.List;

/** "Quantos lotes consigo produzir com o estoque atual?" Java puro. */
public class CalculadoraCapacidade {

    public record Capacidade(long lotesPossiveis, Necessidade limitante) {
    }

    public Capacidade calcular(List<Necessidade> necessidades) {
        List<Necessidade> agrupadas = Necessidade.agrupar(necessidades).stream()
                .filter(n -> n.quantidadePorLote().signum() > 0)
                .toList();
        if (agrupadas.isEmpty()) {
            throw new EstoqueException("A receita não tem insumos nem embalagens; a capacidade não é limitada pelo estoque.");
        }
        Necessidade limitante = null;
        long minimo = Long.MAX_VALUE;
        for (Necessidade n : agrupadas) {
            long lotes = n.saldo().signum() <= 0 ? 0
                    : n.saldo().divide(n.quantidadePorLote(), 0, RoundingMode.FLOOR).longValueExact();
            if (lotes < minimo) {
                minimo = lotes;
                limitante = n;
            }
        }
        return new Capacidade(minimo, limitante);
    }
}
