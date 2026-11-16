package com.confeitaria.configuracao.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracaoCustosRepository extends JpaRepository<ConfiguracaoCustos, UUID> {
}
