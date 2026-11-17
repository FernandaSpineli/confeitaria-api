package com.confeitaria.estoque.persistence;

import com.confeitaria.receita.persistence.Receita;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "producao")
public class Producao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receita_id")
    private Receita receita;

    @Column(nullable = false)
    private int lotes;

    /** Nome do tamanho produzido (nulo = receita inteira), guardado como estava no momento da produção. */
    @Column
    private String tamanho;

    @Column(name = "unidades_produzidas", nullable = false)
    private int unidadesProduzidas;

    /** Custo de insumos + embalagens baixados, pelo custo unitário do momento da produção. */
    @Column(name = "custo_materiais", nullable = false)
    private BigDecimal custoMateriais;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected Producao() {
    }

    public Producao(UUID ownerId, Receita receita, String tamanho, int lotes, int unidadesProduzidas,
            BigDecimal custoMateriais, UUID usuarioId, Instant criadoEm) {
        this.ownerId = ownerId;
        this.receita = receita;
        this.tamanho = tamanho;
        this.lotes = lotes;
        this.unidadesProduzidas = unidadesProduzidas;
        this.custoMateriais = custoMateriais;
        this.usuarioId = usuarioId;
        this.criadoEm = criadoEm;
    }

    public UUID getId() {
        return id;
    }

    public Receita getReceita() {
        return receita;
    }

    public int getLotes() {
        return lotes;
    }

    public String getTamanho() {
        return tamanho;
    }

    public int getUnidadesProduzidas() {
        return unidadesProduzidas;
    }

    public BigDecimal getCustoMateriais() {
        return custoMateriais;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
