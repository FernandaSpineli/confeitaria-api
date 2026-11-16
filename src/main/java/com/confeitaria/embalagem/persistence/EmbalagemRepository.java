package com.confeitaria.embalagem.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmbalagemRepository extends JpaRepository<Embalagem, UUID> {

    Optional<Embalagem> findByIdAndOwnerId(UUID id, UUID ownerId);

    Page<Embalagem> findByOwnerIdAndAtivoTrueAndNomeContainingIgnoreCase(UUID ownerId, String nome, Pageable pageable);

    @Query("select e from Embalagem e where e.ownerId = :ownerId and e.ativo = true and e.saldo < e.estoqueMinimo "
            + "order by e.nome")
    List<Embalagem> abaixoDoMinimo(@Param("ownerId") UUID ownerId);

    @Query("select count(re) > 0 from ReceitaEmbalagem re where re.embalagem.id = :id and re.receita.ativo = true")
    boolean usadaEmReceitaAtiva(@Param("id") UUID id);
}
