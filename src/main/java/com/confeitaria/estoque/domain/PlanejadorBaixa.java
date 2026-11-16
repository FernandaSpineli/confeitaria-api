package com.confeitaria.estoque.domain;

import com.confeitaria.shared.domain.Decimais;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Decide as baixas de uma produção de N lotes. Java puro.
 * Ou devolve todas as baixas, ou lança {@link EstoqueInsuficienteException} listando
 * <b>todos</b> os itens faltantes (nada é baixado parcialmente).
 */
public class PlanejadorBaixa {

    public List<Baixa> planejar(List<Necessidade> necessidades, int lotes, boolean permitirSaldoNegativo) {
        if (lotes <= 0) {
            throw new EstoqueException("A quantidade de lotes deve ser maior que zero.");
        }
        List<Necessidade> agrupadas = Necessidade.agrupar(necessidades);
        if (agrupadas.isEmpty()) {
            throw new EstoqueException("A receita não tem insumos nem embalagens para dar baixa.");
        }
        BigDecimal fator = BigDecimal.valueOf(lotes);
        List<Baixa> baixas = new ArrayList<>();
        List<ItemFaltante> faltantes = new ArrayList<>();
        for (Necessidade n : agrupadas) {
            BigDecimal necessario = Decimais.quantidade(n.quantidadePorLote().multiply(fator));
            BigDecimal saldoPosterior = Decimais.quantidade(n.saldo().subtract(necessario));
            if (saldoPosterior.signum() < 0 && !permitirSaldoNegativo) {
                faltantes.add(new ItemFaltante(n.tipo(), n.itemId(), n.nome(), n.unidade(), necessario, n.saldo(),
                        saldoPosterior.negate()));
            }
            baixas.add(new Baixa(n.tipo(), n.itemId(), n.nome(), n.unidade(), necessario, n.saldo(), saldoPosterior));
        }
        if (!faltantes.isEmpty()) {
            throw new EstoqueInsuficienteException(faltantes);
        }
        return baixas;
    }
}
