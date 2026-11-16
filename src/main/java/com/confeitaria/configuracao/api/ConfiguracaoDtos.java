package com.confeitaria.configuracao.api;

import com.confeitaria.configuracao.persistence.AdicionalBandeira;
import com.confeitaria.configuracao.persistence.Bandeira;
import com.confeitaria.configuracao.persistence.CriterioRateio;
import com.confeitaria.configuracao.persistence.TarifaEnergia;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class ConfiguracaoDtos {

    private ConfiguracaoDtos() {
    }

    public record ConfiguracaoRequest(
            @NotNull @PositiveOrZero @Digits(integer = 13, fraction = 2) BigDecimal salarioDesejadoMes,
            @NotNull @Positive @Digits(integer = 4, fraction = 2) BigDecimal horasTrabalhadasMes,
            @NotNull Bandeira bandeiraVigente,
            @Positive @Digits(integer = 13, fraction = 2) BigDecimal gasPrecoBotijao,
            @Positive @Digits(integer = 6, fraction = 2) BigDecimal gasDuracaoHoras,
            @NotNull CriterioRateio criterioRateio,
            boolean permitirSaldoNegativo) {

        @Schema(hidden = true)
        @AssertTrue(message = "gasPrecoBotijao e gasDuracaoHoras devem ser informados juntos")
        public boolean isGasConsistente() {
            return (gasPrecoBotijao == null) == (gasDuracaoHoras == null);
        }
    }

    public record ConfiguracaoResponse(BigDecimal salarioDesejadoMes, BigDecimal horasTrabalhadasMes,
            BigDecimal valorHora, Bandeira bandeiraVigente, BigDecimal gasPrecoBotijao, BigDecimal gasDuracaoHoras,
            BigDecimal custoGasPorHora, CriterioRateio criterioRateio, boolean permitirSaldoNegativo) {
    }

    public record TarifaRequest(
            @NotNull @Positive @Digits(integer = 3, fraction = 6) BigDecimal tarifaKwh,
            @NotNull LocalDate vigenciaInicio) {
    }

    public record TarifaResponse(UUID id, BigDecimal tarifaKwh, LocalDate vigenciaInicio) {

        public static TarifaResponse of(TarifaEnergia t) {
            return new TarifaResponse(t.getId(), t.getTarifaKwh(), t.getVigenciaInicio());
        }
    }

    public record AdicionalBandeiraRequest(
            @NotNull Bandeira bandeira,
            @NotNull @PositiveOrZero @Digits(integer = 3, fraction = 6) BigDecimal adicionalKwh,
            @NotNull LocalDate vigenciaInicio) {
    }

    public record AdicionalBandeiraResponse(UUID id, Bandeira bandeira, BigDecimal adicionalKwh,
            LocalDate vigenciaInicio) {

        public static AdicionalBandeiraResponse of(AdicionalBandeira a) {
            return new AdicionalBandeiraResponse(a.getId(), a.getBandeira(), a.getAdicionalKwh(),
                    a.getVigenciaInicio());
        }
    }
}
