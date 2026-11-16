package com.confeitaria.custofixo.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/** Custo mensal recorrente (aluguel, internet, MEI...). */
@Entity
@Table(name = "custo_fixo")
public class CustoFixo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String descricao;

    @Column(name = "valor_mensal", nullable = false)
    private BigDecimal valorMensal;

    @Column(nullable = false)
    private boolean ativo = true;

    protected CustoFixo() {
    }

    public CustoFixo(UUID ownerId, String descricao, BigDecimal valorMensal, boolean ativo) {
        this.ownerId = ownerId;
        this.descricao = descricao;
        this.valorMensal = valorMensal;
        this.ativo = ativo;
    }

    public void atualizar(String descricao, BigDecimal valorMensal, boolean ativo) {
        this.descricao = descricao;
        this.valorMensal = valorMensal;
        this.ativo = ativo;
    }

    public UUID getId() {
        return id;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getValorMensal() {
        return valorMensal;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
