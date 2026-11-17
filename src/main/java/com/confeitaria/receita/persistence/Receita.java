package com.confeitaria.receita.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "receita")
public class Receita {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String nome;

    /** Quantas unidades/porções um lote produz. */
    @Column(nullable = false)
    private int rendimento;

    @Column(name = "tempo_preparo_min", nullable = false)
    private int tempoPreparoMin;

    /** Minutos de forno a gás por lote. */
    @Column(name = "minutos_gas", nullable = false)
    private int minutosGas;

    /** Percentual (30.00 = 30%). */
    @Column(name = "margem_lucro", nullable = false)
    private BigDecimal margemLucro;

    @Column(name = "vendas_estimadas_mes")
    private Integer vendasEstimadasMes;

    @Column(nullable = false)
    private boolean ativo = true;

    @Version
    private long version;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @OneToMany(mappedBy = "receita", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReceitaInsumo> insumos = new ArrayList<>();

    @OneToMany(mappedBy = "receita", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReceitaEmbalagem> embalagens = new ArrayList<>();

    @OneToMany(mappedBy = "receita", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReceitaEquipamento> equipamentos = new ArrayList<>();

    @OneToMany(mappedBy = "receita", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("posicao")
    private List<ReceitaTamanho> tamanhos = new ArrayList<>();

    protected Receita() {
    }

    public Receita(UUID ownerId, Instant criadoEm) {
        this.ownerId = ownerId;
        this.criadoEm = criadoEm;
    }

    public void atualizarDados(String nome, int rendimento, int tempoPreparoMin, int minutosGas,
            BigDecimal margemLucro, Integer vendasEstimadasMes) {
        this.nome = nome;
        this.rendimento = rendimento;
        this.tempoPreparoMin = tempoPreparoMin;
        this.minutosGas = minutosGas;
        this.margemLucro = margemLucro;
        this.vendasEstimadasMes = vendasEstimadasMes;
    }

    public void substituirItens(List<ReceitaInsumo> novosInsumos, List<ReceitaEmbalagem> novasEmbalagens,
            List<ReceitaEquipamento> novosEquipamentos) {
        insumos.clear();
        embalagens.clear();
        equipamentos.clear();
        novosInsumos.forEach(i -> i.vincular(this));
        novasEmbalagens.forEach(e -> e.vincular(this));
        novosEquipamentos.forEach(e -> e.vincular(this));
        insumos.addAll(novosInsumos);
        embalagens.addAll(novasEmbalagens);
        equipamentos.addAll(novosEquipamentos);
    }

    public void substituirTamanhos(List<ReceitaTamanho> novos) {
        tamanhos.clear();
        novos.forEach(t -> t.vincular(this));
        tamanhos.addAll(novos);
    }

    public Optional<ReceitaTamanho> tamanho(UUID tamanhoId) {
        return tamanhos.stream().filter(t -> t.getId().equals(tamanhoId)).findFirst();
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

    public int getRendimento() {
        return rendimento;
    }

    public int getTempoPreparoMin() {
        return tempoPreparoMin;
    }

    public int getMinutosGas() {
        return minutosGas;
    }

    public BigDecimal getMargemLucro() {
        return margemLucro;
    }

    public Integer getVendasEstimadasMes() {
        return vendasEstimadasMes;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public List<ReceitaInsumo> getInsumos() {
        return insumos;
    }

    public List<ReceitaEmbalagem> getEmbalagens() {
        return embalagens;
    }

    public List<ReceitaEquipamento> getEquipamentos() {
        return equipamentos;
    }

    public List<ReceitaTamanho> getTamanhos() {
        return tamanhos;
    }
}
