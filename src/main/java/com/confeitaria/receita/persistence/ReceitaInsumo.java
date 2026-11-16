package com.confeitaria.receita.persistence;

import com.confeitaria.insumo.persistence.Insumo;
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
@Table(name = "receita_insumo")
public class ReceitaInsumo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receita_id")
    private Receita receita;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insumo_id")
    private Insumo insumo;

    /** Na unidade base do insumo; por lote ou por unidade conforme {@link #base}. */
    @Column(nullable = false)
    private BigDecimal quantidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BaseQuantidade base;

    protected ReceitaInsumo() {
    }

    public ReceitaInsumo(Insumo insumo, BigDecimal quantidade, BaseQuantidade base) {
        this.insumo = insumo;
        this.quantidade = quantidade;
        this.base = base;
    }

    void vincular(Receita receita) {
        this.receita = receita;
    }

    public Insumo getInsumo() {
        return insumo;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public BaseQuantidade getBase() {
        return base;
    }
}
