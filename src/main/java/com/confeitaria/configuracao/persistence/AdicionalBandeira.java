package com.confeitaria.configuracao.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Histórico do adicional (R$/kWh) de cada bandeira tarifária. */
@Entity
@Table(name = "adicional_bandeira")
public class AdicionalBandeira {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Bandeira bandeira;

    @Column(name = "adicional_kwh", nullable = false)
    private BigDecimal adicionalKwh;

    @Column(name = "vigencia_inicio", nullable = false)
    private LocalDate vigenciaInicio;

    protected AdicionalBandeira() {
    }

    public AdicionalBandeira(UUID ownerId, Bandeira bandeira, BigDecimal adicionalKwh, LocalDate vigenciaInicio) {
        this.ownerId = ownerId;
        this.bandeira = bandeira;
        this.adicionalKwh = adicionalKwh;
        this.vigenciaInicio = vigenciaInicio;
    }

    public UUID getId() {
        return id;
    }

    public Bandeira getBandeira() {
        return bandeira;
    }

    public BigDecimal getAdicionalKwh() {
        return adicionalKwh;
    }

    public LocalDate getVigenciaInicio() {
        return vigenciaInicio;
    }
}
