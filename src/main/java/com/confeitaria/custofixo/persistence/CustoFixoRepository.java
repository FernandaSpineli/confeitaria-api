package com.confeitaria.custofixo.persistence;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustoFixoRepository extends JpaRepository<CustoFixo, UUID> {

    Optional<CustoFixo> findByIdAndOwnerId(UUID id, UUID ownerId);

    Page<CustoFixo> findByOwnerId(UUID ownerId, Pageable pageable);

    @Query("select coalesce(sum(c.valorMensal), 0) from CustoFixo c where c.ownerId = :ownerId and c.ativo = true")
    BigDecimal totalMensalAtivo(@Param("ownerId") UUID ownerId);
}
