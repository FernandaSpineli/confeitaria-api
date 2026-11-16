package com.confeitaria.receita.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReceitaRepository extends JpaRepository<Receita, UUID> {

    Optional<Receita> findByIdAndOwnerId(UUID id, UUID ownerId);

    Page<Receita> findByOwnerIdAndAtivoTrueAndNomeContainingIgnoreCase(UUID ownerId, String nome, Pageable pageable);

    /** Unidades estimadas de venda no mês, somando todas as receitas ativas (rateio por VENDAS). */
    @Query("select coalesce(sum(r.vendasEstimadasMes), 0) from Receita r where r.ownerId = :ownerId and r.ativo = true")
    long totalVendasEstimadasMes(@Param("ownerId") UUID ownerId);
}
