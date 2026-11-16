package com.confeitaria.receita.persistence;

import com.confeitaria.embalagem.persistence.Embalagem;
import com.confeitaria.shared.domain.BaseQuantidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "receita_embalagem")
public class ReceitaEmbalagem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receita_id")
    private Receita receita;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "embalagem_id")
    private Embalagem embalagem;

    @Column(nullable = false)
    private BigDecimal quantidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BaseQuantidade base;

    protected ReceitaEmbalagem() {
    }

    public ReceitaEmbalagem(Embalagem embalagem, BigDecimal quantidade, BaseQuantidade base) {
        this.embalagem = embalagem;
        this.quantidade = quantidade;
        this.base = base;
    }

    void vincular(Receita receita) {
        this.receita = receita;
    }

    public Embalagem getEmbalagem() {
        return embalagem;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public BaseQuantidade getBase() {
        return base;
    }
}
