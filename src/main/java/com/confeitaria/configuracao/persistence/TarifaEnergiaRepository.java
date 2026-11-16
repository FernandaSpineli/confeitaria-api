package com.confeitaria.configuracao.persistence;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TarifaEnergiaRepository extends JpaRepository<TarifaEnergia, UUID> {

    Optional<TarifaEnergia> findFirstByOwnerIdAndVigenciaInicioLessThanEqualOrderByVigenciaInicioDesc(
            UUID ownerId, LocalDate data);

    Page<TarifaEnergia> findByOwnerId(UUID ownerId, Pageable pageable);

    Optional<TarifaEnergia> findByIdAndOwnerId(UUID id, UUID ownerId);

    boolean existsByOwnerIdAndVigenciaInicio(UUID ownerId, LocalDate vigenciaInicio);
}
