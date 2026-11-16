package com.confeitaria.estoque.api;

import com.confeitaria.estoque.domain.TipoItem;
import com.confeitaria.estoque.persistence.MotivoMovimentacao;
import com.confeitaria.estoque.persistence.MovimentacaoEstoque;
import com.confeitaria.estoque.persistence.TipoMovimentacao;
import com.confeitaria.shared.domain.UnidadeMedida;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class EstoqueDtos {

    private EstoqueDtos() {
    }

    @Schema(description = "Informe insumoId OU embalagemId. unidade é opcional (padrão: unidade base do item).")
    public record CompraRequest(
            UUID insumoId,
            UUID embalagemId,
            @NotNull @Positive @Digits(integer = 10, fraction = 6) BigDecimal quantidade,
            UnidadeMedida unidade,
            @NotNull @PositiveOrZero @Digits(integer = 13, fraction = 2) BigDecimal valorPago,
            @Size(max = 255) String observacao) {

        @Schema(hidden = true)
        @AssertTrue(message = "informe exatamente um entre insumoId e embalagemId")
        public boolean isItemUnico() {
            return (insumoId == null) != (embalagemId == null);
        }
    }

    @Schema(description = "quantidade com sinal: positiva soma ao saldo, negativa subtrai. "
            + "motivo: PERDA, VENCIMENTO, INVENTARIO ou OUTRO.")
    public record AjusteRequest(
            UUID insumoId,
            UUID embalagemId,
            @NotNull @Digits(integer = 10, fraction = 6) BigDecimal quantidade,
            UnidadeMedida unidade,
            @NotNull MotivoMovimentacao motivo,
            @Size(max = 255) String observacao) {

        @Schema(hidden = true)
        @AssertTrue(message = "informe exatamente um entre insumoId e embalagemId")
        public boolean isItemUnico() {
            return (insumoId == null) != (embalagemId == null);
        }

        @Schema(hidden = true)
        @AssertTrue(message = "quantidade deve ser diferente de zero")
        public boolean isQuantidadeNaoNula() {
            return quantidade == null || quantidade.signum() != 0;
        }

        @Schema(hidden = true)
        @AssertTrue(message = "motivo de ajuste deve ser PERDA, VENCIMENTO, INVENTARIO ou OUTRO")
        public boolean isMotivoDeAjuste() {
            return motivo == null || motivo.permitidoEmAjuste();
        }
    }

    public record MovimentacaoResponse(UUID id, TipoMovimentacao tipo, MotivoMovimentacao motivo, TipoItem tipoItem,
            UUID itemId, String item, BigDecimal quantidade, BigDecimal saldoApos, BigDecimal custoUnitario,
            BigDecimal valorTotal, String observacao, UUID producaoId, UUID usuarioId, Instant criadoEm) {

        public static MovimentacaoResponse of(MovimentacaoEstoque m) {
            return new MovimentacaoResponse(m.getId(), m.getTipo(), m.getMotivo(), m.tipoItem(), m.item().getId(),
                    m.item().getNome(), m.getQuantidade(), m.getSaldoApos(),
                    m.getCustoUnitario().setScale(6, java.math.RoundingMode.HALF_EVEN), m.getValorTotal(),
                    m.getObservacao(), m.getProducao() == null ? null : m.getProducao().getId(), m.getUsuarioId(),
                    m.getCriadoEm());
        }
    }

    public record CompraResponse(MovimentacaoResponse movimentacao, BigDecimal saldoAnterior, BigDecimal saldoAtual,
            BigDecimal custoUnitarioAnterior, BigDecimal custoUnitarioAtual) {
    }

    public record ItemAbaixoMinimoResponse(TipoItem tipo, UUID id, String nome, String unidade, BigDecimal saldo,
            BigDecimal estoqueMinimo, BigDecimal faltaParaMinimo) {
    }

    public record ProducaoRequest(@NotNull UUID receitaId, @NotNull @Positive @Max(1000) Integer lotes) {
    }

    public record BaixaResponse(TipoItem tipo, UUID itemId, String item, String unidade, BigDecimal quantidade,
            BigDecimal saldoAtual) {
    }

    public record ProducaoResponse(UUID id, UUID receitaId, String receita, int lotes, int unidadesProduzidas,
            BigDecimal custoMateriais, Instant criadoEm, List<BaixaResponse> baixas) {
    }

    public record CapacidadeResponse(UUID receitaId, String receita, long lotesPossiveis, long unidadesPossiveis,
            FatorLimitante fatorLimitante) {
    }

    public record FatorLimitante(TipoItem tipo, UUID itemId, String item, String unidade, BigDecimal necessarioPorLote,
            BigDecimal saldo) {
    }
}
