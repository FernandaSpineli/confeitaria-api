package com.confeitaria.receita.persistence;

import com.confeitaria.embalagem.persistence.Embalagem;
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
import java.util.UUID;

/**
 * Um jeito de vender a receita: usa {@code fracaoReceita} da receita (1 = inteira, 0,5 = meia) e rende
 * {@code rendimento} peças, cada uma com {@code embalagensPorPeca} unidades da embalagem (opcional).
 */
@Entity
@Table(name = "receita_tamanho")
public class ReceitaTamanho {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receita_id")
    private Receita receita;

    @Column(nullable = false)
    private String nome;

    @Column(name = "fracao_receita", nullable = false)
    private BigDecimal fracaoReceita;

    @Column(nullable = false)
    private int rendimento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "embalagem_id")
    private Embalagem embalagem;

    @Column(name = "embalagens_por_peca")
    private BigDecimal embalagensPorPeca;

    @Column(nullable = false)
    private int posicao;

    protected ReceitaTamanho() {
    }

    public ReceitaTamanho(String nome, BigDecimal fracaoReceita, int rendimento, Embalagem embalagem,
            BigDecimal embalagensPorPeca, int posicao) {
        this.nome = nome;
        this.fracaoReceita = fracaoReceita;
        this.rendimento = rendimento;
        this.embalagem = embalagem;
        this.embalagensPorPeca = embalagem == null ? null : embalagensPorPeca;
        this.posicao = posicao;
    }

    void vincular(Receita receita) {
        this.receita = receita;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getFracaoReceita() {
        return fracaoReceita;
    }

    public int getRendimento() {
        return rendimento;
    }

    public Embalagem getEmbalagem() {
        return embalagem;
    }

    public BigDecimal getEmbalagensPorPeca() {
        return embalagensPorPeca;
    }

    public int getPosicao() {
        return posicao;
    }
}
