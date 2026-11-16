package com.confeitaria.insumo.api;

import com.confeitaria.insumo.persistence.Insumo;
import com.confeitaria.shared.domain.Decimais;
import com.confeitaria.shared.domain.UnidadeBase;
import com.confeitaria.shared.domain.UnidadeMedida;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public final class InsumoDtos {

    private InsumoDtos() {
    }

    @Schema(description = "quantidadeEmbalagem e estoqueMinimo são informados na 'unidade' e convertidos para a unidade base.")
    public record InsumoRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotNull UnidadeMedida unidade,
            @NotNull @Positive @Digits(integer = 10, fraction = 6) BigDecimal quantidadeEmbalagem,
            @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal precoEmbalagem,
            @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 6) BigDecimal estoqueMinimo) {
    }

    public record InsumoResponse(UUID id, String nome, UnidadeBase unidadeBase, BigDecimal custoUnitario,
            BigDecimal ultimaQuantidadeEmbalagem, BigDecimal ultimoPrecoEmbalagem, BigDecimal saldo,
            BigDecimal estoqueMinimo, boolean abaixoDoMinimo, boolean ativo) {

        public static InsumoResponse of(Insumo i) {
            return new InsumoResponse(i.getId(), i.getNome(), i.unidadeBase(),
                    i.getCustoUnitario().setScale(6, Decimais.ARREDONDAMENTO), i.getUltimaQuantidadeEmbalagem(),
                    i.getUltimoPrecoEmbalagem(), i.getSaldo(), i.getEstoqueMinimo(), i.abaixoDoMinimo(), i.isAtivo());
        }
    }
}
