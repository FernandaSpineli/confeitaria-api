package com.confeitaria.configuracao.persistence;

/** Como os custos fixos mensais são distribuídos entre as receitas. */
public enum CriterioRateio {
    /** Proporcional ao tempo de preparo: (fixos / horas trabalhadas no mês) × horas da receita. */
    HORAS,
    /** Proporcional às vendas: (fixos / unidades estimadas no mês) × rendimento da receita. */
    VENDAS
}
