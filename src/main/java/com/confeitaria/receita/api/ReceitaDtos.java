package com.confeitaria.receita.api;

import com.confeitaria.shared.domain.BaseQuantidade;
import com.confeitaria.shared.domain.UnidadeBase;
import com.confeitaria.shared.domain.UnidadeMedida;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class ReceitaDtos {

    private ReceitaDtos() {
    }

    public record ReceitaRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotNull @Positive Integer rendimento,
            @NotNull @PositiveOrZero Integer tempoPreparoMin,
            @PositiveOrZero Integer minutosGas,
            @NotNull @PositiveOrZero @DecimalMax(value = "100.00", inclusive = false)
            @Digits(integer = 3, fraction = 2) BigDecimal margemLucro,
            @PositiveOrZero Integer vendasEstimadasMes,
            @Valid List<ItemInsumoRequest> insumos,
            @Valid List<ItemEmbalagemRequest> embalagens,
            @Valid List<ItemEquipamentoRequest> equipamentos) {

        public List<ItemInsumoRequest> insumosOuVazio() {
            return insumos == null ? List.of() : insumos;
        }

        public List<ItemEmbalagemRequest> embalagensOuVazio() {
            return embalagens == null ? List.of() : embalagens;
        }

        public List<ItemEquipamentoRequest> equipamentosOuVazio() {
            return equipamentos == null ? List.of() : equipamentos;
        }
    }

    @Schema(description = "unidade é opcional (padrão: unidade base do insumo). base: POR_LOTE (padrão) ou POR_UNIDADE.")
    public record ItemInsumoRequest(
            @NotNull UUID insumoId,
            @NotNull @Positive @Digits(integer = 10, fraction = 6) BigDecimal quantidade,
            UnidadeMedida unidade,
            BaseQuantidade base) {
    }

    @Schema(description = "base: POR_LOTE (padrão, ex.: 1 caixa para o bolo inteiro) ou POR_UNIDADE (ex.: 1 forminha por brigadeiro).")
    public record ItemEmbalagemRequest(
            @NotNull UUID embalagemId,
            @NotNull @Positive @Digits(integer = 10, fraction = 6) BigDecimal quantidade,
            BaseQuantidade base) {
    }

    public record ItemEquipamentoRequest(
            @NotNull UUID equipamentoId,
            @NotNull @Positive Integer minutosUso) {
    }

    public record ReceitaResponse(UUID id, String nome, int rendimento, int tempoPreparoMin, int minutosGas,
            BigDecimal margemLucro, Integer vendasEstimadasMes, boolean ativo, List<ItemInsumoResponse> insumos,
            List<ItemEmbalagemResponse> embalagens, List<ItemEquipamentoResponse> equipamentos) {
    }

    public record ItemInsumoResponse(UUID insumoId, String nome, BigDecimal quantidade, UnidadeBase unidade,
            BaseQuantidade base, BigDecimal quantidadePorLote) {
    }

    public record ItemEmbalagemResponse(UUID embalagemId, String nome, BigDecimal quantidade, BaseQuantidade base,
            BigDecimal quantidadePorLote) {
    }

    public record ItemEquipamentoResponse(UUID equipamentoId, String nome, BigDecimal potenciaWatts, int minutosUso) {
    }

    public record ReceitaResumoResponse(UUID id, String nome, int rendimento, BigDecimal margemLucro, boolean ativo) {
    }
}
