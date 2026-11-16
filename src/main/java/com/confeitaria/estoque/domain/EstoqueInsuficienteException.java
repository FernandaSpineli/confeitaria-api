package com.confeitaria.estoque.domain;

import java.util.List;
import java.util.stream.Collectors;

public class EstoqueInsuficienteException extends EstoqueException {

    private final transient List<ItemFaltante> itensFaltantes;

    public EstoqueInsuficienteException(List<ItemFaltante> itensFaltantes) {
        super(itensFaltantes.stream()
                .map(i -> i.item() + " (faltam " + i.faltam().stripTrailingZeros().toPlainString() + " " + i.unidade() + ")")
                .collect(Collectors.joining(", ", "Estoque insuficiente: ", ".")));
        this.itensFaltantes = List.copyOf(itensFaltantes);
    }

    public List<ItemFaltante> itensFaltantes() {
        return itensFaltantes;
    }
}
