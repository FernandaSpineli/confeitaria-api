package com.confeitaria.estoque.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Quanto de um item um lote da receita consome, e quanto há em estoque (unidade base). */
public record Necessidade(TipoItem tipo, UUID itemId, String nome, String unidade, BigDecimal quantidadePorLote,
        BigDecimal saldo) {

    /** Junta linhas repetidas do mesmo item (ex.: chocolate na massa e na cobertura). */
    public static List<Necessidade> agrupar(List<Necessidade> necessidades) {
        Map<String, Necessidade> porItem = new LinkedHashMap<>();
        for (Necessidade n : necessidades) {
            porItem.merge(n.tipo() + ":" + n.itemId(), n, (a, b) -> new Necessidade(a.tipo(), a.itemId(), a.nome(),
                    a.unidade(), a.quantidadePorLote().add(b.quantidadePorLote()), a.saldo()));
        }
        return new ArrayList<>(porItem.values());
    }
}
