package com.confeitaria.estoque.persistence;

import com.confeitaria.embalagem.persistence.Embalagem;
import com.confeitaria.estoque.domain.TipoItem;
import com.confeitaria.insumo.persistence.Insumo;
import com.confeitaria.shared.persistence.ItemEstoque;
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
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Histórico imutável de estoque. Só existe INSERT: a entidade é {@code @Immutable} e um trigger
 * no PostgreSQL rejeita UPDATE/DELETE.
 */
@Entity
@Immutable
@Table(name = "movimentacao_estoque")
public class MovimentacaoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimentacao tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MotivoMovimentacao motivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insumo_id")
    private Insumo insumo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "embalagem_id")
    private Embalagem embalagem;

    /** Com sinal: positiva aumenta o saldo, negativa diminui. Na unidade base. */
    @Column(nullable = false)
    private BigDecimal quantidade;

    @Column(name = "saldo_apos", nullable = false)
    private BigDecimal saldoApos;

    @Column(name = "custo_unitario", nullable = false)
    private BigDecimal custoUnitario;

    @Column(name = "valor_total")
    private BigDecimal valorTotal;

    private String observacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producao_id")
    private Producao producao;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected MovimentacaoEstoque() {
    }

    public MovimentacaoEstoque(ItemEstoque item, TipoMovimentacao tipo, MotivoMovimentacao motivo,
            BigDecimal quantidade, BigDecimal custoUnitario, BigDecimal valorTotal, String observacao,
            Producao producao, UUID usuarioId, Instant criadoEm) {
        this.ownerId = item.getOwnerId();
        if (item instanceof Insumo i) {
            this.insumo = i;
        } else {
            this.embalagem = (Embalagem) item;
        }
        this.tipo = tipo;
        this.motivo = motivo;
        this.quantidade = quantidade;
        this.saldoApos = item.getSaldo();
        this.custoUnitario = custoUnitario;
        this.valorTotal = valorTotal;
        this.observacao = observacao;
        this.producao = producao;
        this.usuarioId = usuarioId;
        this.criadoEm = criadoEm;
    }

    public ItemEstoque item() {
        return insumo != null ? insumo : embalagem;
    }

    public TipoItem tipoItem() {
        return insumo != null ? TipoItem.INSUMO : TipoItem.EMBALAGEM;
    }

    public UUID getId() {
        return id;
    }

    public TipoMovimentacao getTipo() {
        return tipo;
    }

    public MotivoMovimentacao getMotivo() {
        return motivo;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public BigDecimal getSaldoApos() {
        return saldoApos;
    }

    public BigDecimal getCustoUnitario() {
        return custoUnitario;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public String getObservacao() {
        return observacao;
    }

    public Producao getProducao() {
        return producao;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
