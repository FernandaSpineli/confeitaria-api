package com.confeitaria.embalagem.api;

import com.confeitaria.embalagem.persistence.Embalagem;
import com.confeitaria.shared.domain.Decimais;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public final class EmbalagemDtos {

    private EmbalagemDtos() {
    }

    public record EmbalagemRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotNull @PositiveOrZero @Digits(integer = 13, fraction = 4) BigDecimal custoUnitario,
            @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 0) BigDecimal estoqueMinimo) {
    }

    public record EmbalagemResponse(UUID id, String nome, BigDecimal custoUnitario, BigDecimal saldo,
            BigDecimal estoqueMinimo, boolean abaixoDoMinimo, boolean ativo) {

        public static EmbalagemResponse of(Embalagem e) {
            return new EmbalagemResponse(e.getId(), e.getNome(),
                    e.getCustoUnitario().setScale(6, Decimais.ARREDONDAMENTO), e.getSaldo(), e.getEstoqueMinimo(),
                    e.abaixoDoMinimo(), e.isAtivo());
        }
    }
}
