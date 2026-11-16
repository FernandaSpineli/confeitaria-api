package com.confeitaria.custofixo.api;

import com.confeitaria.custofixo.persistence.CustoFixo;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public final class CustoFixoDtos {

    private CustoFixoDtos() {
    }

    public record CustoFixoRequest(
            @NotBlank @Size(max = 120) String descricao,
            @NotNull @PositiveOrZero @Digits(integer = 13, fraction = 2) BigDecimal valorMensal,
            Boolean ativo) {
    }

    public record CustoFixoResponse(UUID id, String descricao, BigDecimal valorMensal, boolean ativo) {

        public static CustoFixoResponse of(CustoFixo c) {
            return new CustoFixoResponse(c.getId(), c.getDescricao(), c.getValorMensal(), c.isAtivo());
        }
    }
}
