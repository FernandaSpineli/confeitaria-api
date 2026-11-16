package com.confeitaria.estoque.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, UUID>,
        JpaSpecificationExecutor<MovimentacaoEstoque> {

    List<MovimentacaoEstoque> findByProducaoIdOrderByCriadoEm(UUID producaoId);
}
