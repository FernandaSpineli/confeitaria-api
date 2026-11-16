package com.confeitaria.insumo.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InsumoRepository extends JpaRepository<Insumo, UUID> {

    Optional<Insumo> findByIdAndOwnerId(UUID id, UUID ownerId);

    Page<Insumo> findByOwnerIdAndAtivoTrueAndNomeContainingIgnoreCase(UUID ownerId, String nome, Pageable pageable);

    @Query("select i from Insumo i where i.ownerId = :ownerId and i.ativo = true and i.saldo < i.estoqueMinimo "
            + "order by i.nome")
    List<Insumo> abaixoDoMinimo(@Param("ownerId") UUID ownerId);

    @Query("select count(ri) > 0 from ReceitaInsumo ri where ri.insumo.id = :id and ri.receita.ativo = true")
    boolean usadoEmReceitaAtiva(@Param("id") UUID id);
}
