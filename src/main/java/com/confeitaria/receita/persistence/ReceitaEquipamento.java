package com.confeitaria.receita.persistence;

import com.confeitaria.equipamento.persistence.Equipamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "receita_equipamento")
public class ReceitaEquipamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receita_id")
    private Receita receita;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipamento_id")
    private Equipamento equipamento;

    /** Minutos de uso por lote. */
    @Column(name = "minutos_uso", nullable = false)
    private int minutosUso;

    protected ReceitaEquipamento() {
    }

    public ReceitaEquipamento(Equipamento equipamento, int minutosUso) {
        this.equipamento = equipamento;
        this.minutosUso = minutosUso;
    }

    void vincular(Receita receita) {
        this.receita = receita;
    }

    public Equipamento getEquipamento() {
        return equipamento;
    }

    public int getMinutosUso() {
        return minutosUso;
    }
}
