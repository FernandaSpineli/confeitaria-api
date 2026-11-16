package com.confeitaria.canalvenda.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/** Canal de venda e sua taxa percentual (cartão, maquininha, marketplace). */
@Entity
@Table(name = "canal_venda")
public class CanalVenda {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String nome;

    /** Percentual (27.00 = 27%). */
    @Column(name = "taxa_percentual", nullable = false)
    private BigDecimal taxaPercentual;

    @Column(nullable = false)
    private boolean ativo = true;

    protected CanalVenda() {
    }

    public CanalVenda(UUID ownerId, String nome, BigDecimal taxaPercentual, boolean ativo) {
        this.ownerId = ownerId;
        this.nome = nome;
        this.taxaPercentual = taxaPercentual;
        this.ativo = ativo;
    }

    public void atualizar(String nome, BigDecimal taxaPercentual, boolean ativo) {
        this.nome = nome;
        this.taxaPercentual = taxaPercentual;
        this.ativo = ativo;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getTaxaPercentual() {
        return taxaPercentual;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
