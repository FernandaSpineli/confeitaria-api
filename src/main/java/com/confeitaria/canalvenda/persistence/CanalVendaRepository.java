package com.confeitaria.canalvenda.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CanalVendaRepository extends JpaRepository<CanalVenda, UUID> {

    Optional<CanalVenda> findByIdAndOwnerId(UUID id, UUID ownerId);

    Page<CanalVenda> findByOwnerId(UUID ownerId, Pageable pageable);

    List<CanalVenda> findByOwnerIdAndAtivoTrueOrderByNome(UUID ownerId);
}
