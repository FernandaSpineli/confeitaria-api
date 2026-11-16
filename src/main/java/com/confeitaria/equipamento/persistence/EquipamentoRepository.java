package com.confeitaria.equipamento.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EquipamentoRepository extends JpaRepository<Equipamento, UUID> {

    Optional<Equipamento> findByIdAndOwnerId(UUID id, UUID ownerId);

    Page<Equipamento> findByOwnerIdAndAtivoTrue(UUID ownerId, Pageable pageable);

    @Query("select count(re) > 0 from ReceitaEquipamento re where re.equipamento.id = :id and re.receita.ativo = true")
    boolean usadoEmReceitaAtiva(@Param("id") UUID id);
}
