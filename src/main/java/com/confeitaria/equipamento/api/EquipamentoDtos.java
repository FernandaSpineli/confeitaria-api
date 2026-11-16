package com.confeitaria.equipamento.api;

import com.confeitaria.equipamento.persistence.Equipamento;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public final class EquipamentoDtos {

    private EquipamentoDtos() {
    }

    public record EquipamentoRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotNull @Positive @Digits(integer = 6, fraction = 2) BigDecimal potenciaWatts) {
    }

    public record EquipamentoResponse(UUID id, String nome, BigDecimal potenciaWatts, boolean ativo) {

        public static EquipamentoResponse of(Equipamento e) {
            return new EquipamentoResponse(e.getId(), e.getNome(), e.getPotenciaWatts(), e.isAtivo());
        }
    }
}
