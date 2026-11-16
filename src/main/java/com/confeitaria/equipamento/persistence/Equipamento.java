package com.confeitaria.equipamento.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "equipamento")
public class Equipamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String nome;

    @Column(name = "potencia_watts", nullable = false)
    private BigDecimal potenciaWatts;

    @Column(nullable = false)
    private boolean ativo = true;

    protected Equipamento() {
    }

    public Equipamento(UUID ownerId, String nome, BigDecimal potenciaWatts) {
        this.ownerId = ownerId;
        this.nome = nome;
        this.potenciaWatts = potenciaWatts;
    }

    public void atualizar(String nome, BigDecimal potenciaWatts) {
        this.nome = nome;
        this.potenciaWatts = potenciaWatts;
    }

    public void desativar() {
        this.ativo = false;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getPotenciaWatts() {
        return potenciaWatts;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
