package com.confeitaria.configuracao.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Histórico da tarifa do kWh. A vigente é a de maior vigência_inicio que já começou. */
@Entity
@Table(name = "tarifa_energia")
public class TarifaEnergia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "tarifa_kwh", nullable = false)
    private BigDecimal tarifaKwh;

    @Column(name = "vigencia_inicio", nullable = false)
    private LocalDate vigenciaInicio;

    protected TarifaEnergia() {
    }

    public TarifaEnergia(UUID ownerId, BigDecimal tarifaKwh, LocalDate vigenciaInicio) {
        this.ownerId = ownerId;
        this.tarifaKwh = tarifaKwh;
        this.vigenciaInicio = vigenciaInicio;
    }

    public UUID getId() {
        return id;
    }

    public BigDecimal getTarifaKwh() {
        return tarifaKwh;
    }

    public LocalDate getVigenciaInicio() {
        return vigenciaInicio;
    }
}
