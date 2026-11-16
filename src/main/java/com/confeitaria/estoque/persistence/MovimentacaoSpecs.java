package com.confeitaria.estoque.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/** Filtros opcionais do histórico; o filtro por dona é sempre aplicado. */
public final class MovimentacaoSpecs {

    private MovimentacaoSpecs() {
    }

    public static Specification<MovimentacaoEstoque> filtrar(UUID ownerId, TipoMovimentacao tipo, UUID insumoId,
            UUID embalagemId, Instant de, Instant ate) {
        return (root, query, cb) -> {
            var p = cb.equal(root.get("ownerId"), ownerId);
            if (tipo != null) {
                p = cb.and(p, cb.equal(root.get("tipo"), tipo));
            }
            if (insumoId != null) {
                p = cb.and(p, cb.equal(root.get("insumo").get("id"), insumoId));
            }
            if (embalagemId != null) {
                p = cb.and(p, cb.equal(root.get("embalagem").get("id"), embalagemId));
            }
            if (de != null) {
                p = cb.and(p, cb.greaterThanOrEqualTo(root.get("criadoEm"), de));
            }
            if (ate != null) {
                p = cb.and(p, cb.lessThan(root.get("criadoEm"), ate));
            }
            return p;
        };
    }
}
