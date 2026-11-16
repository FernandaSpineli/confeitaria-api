package com.confeitaria.insumo.persistence;

import com.confeitaria.shared.domain.Decimais;
import com.confeitaria.shared.domain.UnidadeBase;
import com.confeitaria.shared.persistence.ItemEstoque;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "insumo")
public class Insumo extends ItemEstoque {

    @Enumerated(EnumType.STRING)
    @Column(name = "unidade_base", nullable = false, updatable = false)
    private UnidadeBase unidadeBase;

    /** Última embalagem comprada, já na unidade base (ex.: 1000 g). */
    @Column(name = "ultima_qtd_embalagem", nullable = false)
    private BigDecimal ultimaQuantidadeEmbalagem;

    @Column(name = "ultimo_preco_embalagem", nullable = false)
    private BigDecimal ultimoPrecoEmbalagem;

    protected Insumo() {
    }

    public Insumo(UUID ownerId, String nome, UnidadeBase unidadeBase, BigDecimal quantidadeEmbalagemBase,
            BigDecimal precoEmbalagem, BigDecimal estoqueMinimoBase, Instant criadoEm) {
        super(ownerId, nome, Decimais.dividir(precoEmbalagem, quantidadeEmbalagemBase), estoqueMinimoBase, criadoEm);
        this.unidadeBase = unidadeBase;
        this.ultimaQuantidadeEmbalagem = Decimais.quantidade(quantidadeEmbalagemBase);
        this.ultimoPrecoEmbalagem = Decimais.monetario(precoEmbalagem);
    }

    /** Edição manual: recalcula o custo unitário a partir da embalagem informada. */
    public void atualizarCadastro(String nome, BigDecimal quantidadeEmbalagemBase, BigDecimal precoEmbalagem,
            BigDecimal estoqueMinimoBase) {
        atualizarCadastroBase(nome, Decimais.dividir(precoEmbalagem, quantidadeEmbalagemBase), estoqueMinimoBase);
        this.ultimaQuantidadeEmbalagem = Decimais.quantidade(quantidadeEmbalagemBase);
        this.ultimoPrecoEmbalagem = Decimais.monetario(precoEmbalagem);
    }

    public void registrarUltimaCompra(BigDecimal quantidadeBase, BigDecimal valorPago) {
        this.ultimaQuantidadeEmbalagem = Decimais.quantidade(quantidadeBase);
        this.ultimoPrecoEmbalagem = Decimais.monetario(valorPago);
    }

    @Override
    public UnidadeBase unidadeBase() {
        return unidadeBase;
    }

    public BigDecimal getUltimaQuantidadeEmbalagem() {
        return ultimaQuantidadeEmbalagem;
    }

    public BigDecimal getUltimoPrecoEmbalagem() {
        return ultimoPrecoEmbalagem;
    }
}
