package com.confeitaria.configuracao.persistence;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdicionalBandeiraRepository extends JpaRepository<AdicionalBandeira, UUID> {

    Optional<AdicionalBandeira> findFirstByOwnerIdAndBandeiraAndVigenciaInicioLessThanEqualOrderByVigenciaInicioDesc(
            UUID ownerId, Bandeira bandeira, LocalDate data);

    Page<AdicionalBandeira> findByOwnerId(UUID ownerId, Pageable pageable);

    Page<AdicionalBandeira> findByOwnerIdAndBandeira(UUID ownerId, Bandeira bandeira, Pageable pageable);

    Optional<AdicionalBandeira> findByIdAndOwnerId(UUID id, UUID ownerId);

    boolean existsByOwnerIdAndBandeiraAndVigenciaInicio(UUID ownerId, Bandeira bandeira, LocalDate vigenciaInicio);
}
