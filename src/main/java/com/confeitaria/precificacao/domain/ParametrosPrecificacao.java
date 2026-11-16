package com.confeitaria.precificacao.domain;

import com.confeitaria.shared.domain.BaseQuantidade;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Fotografia imutável de tudo que o cálculo precisa. Percentuais já vêm como fração (0.30 = 30%).
 * {@code energia} e {@code gas} podem ser nulos quando não configurados; o cálculo só falha se a
 * receita de fato precisar deles.
 */
public record ParametrosPrecificacao(
        int rendimento,
        int tempoPreparoMin,
        int minutosGas,
        BigDecimal margemLucro,
        List<ItemCusto> insumos,
        List<ItemCusto> embalagens,
        List<UsoEquipamento> equipamentos,
        MaoDeObra maoDeObra,
        Energia energia,
        Gas gas,
        RateioCustosFixos rateio,
        List<CanalTaxa> canais) {

    public ParametrosPrecificacao {
        insumos = List.copyOf(insumos);
        embalagens = List.copyOf(embalagens);
        equipamentos = List.copyOf(equipamentos);
        canais = List.copyOf(canais);
    }

    /** Quantidade na unidade base, custo em R$ por unidade base. */
    public record ItemCusto(String nome, String unidade, BigDecimal quantidade, BaseQuantidade base,
            BigDecimal custoUnitario) {
    }

    public record UsoEquipamento(String nome, BigDecimal potenciaWatts, int minutos) {
    }

    public record MaoDeObra(BigDecimal salarioMensal, BigDecimal horasMes) {
    }

    public record Energia(BigDecimal tarifaKwh, BigDecimal adicionalBandeiraKwh) {
    }

    public record Gas(BigDecimal precoBotijao, BigDecimal duracaoHoras) {
    }

    public record RateioCustosFixos(Criterio criterio, BigDecimal totalMensal, BigDecimal horasMes,
            long vendasEstimadasMes) {

        public enum Criterio { HORAS, VENDAS }
    }

    /** {@code id} pode ser nulo (ex.: taxa avulsa de simulação). */
    public record CanalTaxa(UUID id, String nome, BigDecimal taxa) {
    }
}
