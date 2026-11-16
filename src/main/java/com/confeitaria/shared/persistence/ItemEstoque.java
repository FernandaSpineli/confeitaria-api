package com.confeitaria.shared.persistence;

import com.confeitaria.shared.domain.Decimais;
import com.confeitaria.shared.domain.UnidadeBase;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Base de insumos e embalagens: tudo que tem saldo e custo unitário.
 *
 * <p>O saldo não tem setter: só muda por {@link #registrarEntrada} e {@link #aplicarVariacao},
 * chamados exclusivamente pelos serviços de estoque, sempre junto com uma movimentação.
 * {@code @Version} garante lock otimista nas baixas concorrentes.
 */
@MappedSuperclass
public abstract class ItemEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String nome;

    /** R$ por unidade base (ex.: R$ por grama). */
    @Column(name = "custo_unitario", nullable = false)
    private BigDecimal custoUnitario;

    @Column(nullable = false)
    private BigDecimal saldo = BigDecimal.ZERO;

    @Column(name = "estoque_minimo", nullable = false)
    private BigDecimal estoqueMinimo;

    @Column(nullable = false)
    private boolean ativo = true;

    @Version
    private long version;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected ItemEstoque() {
    }

    protected ItemEstoque(UUID ownerId, String nome, BigDecimal custoUnitario, BigDecimal estoqueMinimo,
            Instant criadoEm) {
        this.ownerId = ownerId;
        this.nome = nome;
        this.custoUnitario = Decimais.calculo(custoUnitario);
        this.estoqueMinimo = Decimais.quantidade(estoqueMinimo);
        this.criadoEm = criadoEm;
    }

    public abstract UnidadeBase unidadeBase();

    protected void atualizarCadastroBase(String nome, BigDecimal custoUnitario, BigDecimal estoqueMinimo) {
        this.nome = nome;
        this.custoUnitario = Decimais.calculo(custoUnitario);
        this.estoqueMinimo = Decimais.quantidade(estoqueMinimo);
    }

    /** Entrada por compra: soma ao saldo e assume o novo custo médio ponderado. */
    public void registrarEntrada(BigDecimal quantidade, BigDecimal novoCustoUnitario) {
        this.saldo = Decimais.quantidade(saldo.add(quantidade));
        this.custoUnitario = Decimais.calculo(novoCustoUnitario);
    }

    /** Saída (delta negativo) ou ajuste (positivo ou negativo). Não altera o custo. */
    public void aplicarVariacao(BigDecimal delta) {
        this.saldo = Decimais.quantidade(saldo.add(delta));
    }

    public boolean abaixoDoMinimo() {
        return saldo.compareTo(estoqueMinimo) < 0;
    }

    public void desativar() {
        this.ativo = false;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getCustoUnitario() {
        return custoUnitario;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public BigDecimal getEstoqueMinimo() {
        return estoqueMinimo;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public long getVersion() {
        return version;
    }
}
