package com.confeitaria.canalvenda.api;

import com.confeitaria.canalvenda.persistence.CanalVenda;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public final class CanalVendaDtos {

    private CanalVendaDtos() {
    }

    public record CanalVendaRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotNull @PositiveOrZero @DecimalMax(value = "100.00", inclusive = false)
            @Digits(integer = 3, fraction = 2) BigDecimal taxaPercentual,
            Boolean ativo) {
    }

    public record CanalVendaResponse(UUID id, String nome, BigDecimal taxaPercentual, boolean ativo) {

        public static CanalVendaResponse of(CanalVenda c) {
            return new CanalVendaResponse(c.getId(), c.getNome(), c.getTaxaPercentual(), c.isAtivo());
        }
    }
}
