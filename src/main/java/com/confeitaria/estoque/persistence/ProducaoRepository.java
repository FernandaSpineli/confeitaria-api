package com.confeitaria.estoque.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProducaoRepository extends JpaRepository<Producao, UUID> {

    Optional<Producao> findByIdAndOwnerId(UUID id, UUID ownerId);

    Page<Producao> findByOwnerId(UUID ownerId, Pageable pageable);
}
