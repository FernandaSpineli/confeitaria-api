package com.confeitaria.precificacao.api;

import com.confeitaria.configuracao.persistence.Bandeira;
import com.confeitaria.precificacao.domain.ResultadoPrecificacao;
import com.confeitaria.precificacao.domain.ResultadoPrecificacao.ComposicaoCusto;
import com.confeitaria.precificacao.domain.ResultadoPrecificacao.LinhaCusto;
import com.confeitaria.precificacao.domain.ResultadoPrecificacao.PrecoCalculado;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class PrecificacaoDtos {

    private PrecificacaoDtos() {
    }

    @Schema(description = """
            Todos os campos são opcionais; o que não for informado usa o valor salvo.
            canalVendaId e/ou taxaPercentual restringem o resultado a esses canais.
            tamanhoId calcula o preço de um tamanho da receita (ex.: torta pequena).""")
    public record SimulacaoRequest(
            @PositiveOrZero @DecimalMax(value = "100.00", inclusive = false) @Digits(integer = 3, fraction = 2)
            BigDecimal margemLucro,
            Bandeira bandeira,
            @Positive @Digits(integer = 3, fraction = 6) BigDecimal tarifaKwh,
            UUID canalVendaId,
            @PositiveOrZero @DecimalMax(value = "100.00", inclusive = false) @Digits(integer = 3, fraction = 2)
            BigDecimal taxaPercentual,
            UUID tamanhoId) {

        public static SimulacaoRequest vazia() {
            return new SimulacaoRequest(null, null, null, null, null, null);
        }

        public static SimulacaoRequest doTamanho(UUID tamanhoId) {
            return new SimulacaoRequest(null, null, null, null, null, tamanhoId);
        }
    }

    @Schema(description = """
            rendimento: peças do lote calculado (do tamanho, quando houver). composicaoLote: custo desse lote.
            tamanho é nulo quando o preço é da receita inteira.""")
    public record PrecoResponse(UUID receitaId, String receita, boolean simulacao, Bandeira bandeira,
            TamanhoCalculado tamanho, int rendimento, BigDecimal margemLucro, ComposicaoCusto composicaoLote,
            List<LinhaCusto> detalhamento, BigDecimal custoUnitario, PrecoCalculado precoBase,
            List<PrecoCalculado> precosPorCanal) {

        public static PrecoResponse of(UUID receitaId, String receita, boolean simulacao, Bandeira bandeira,
                TamanhoCalculado tamanho, ResultadoPrecificacao r) {
            return new PrecoResponse(receitaId, receita, simulacao, bandeira, tamanho, r.rendimento(),
                    r.margemLucro(), r.composicaoLote(), r.detalhamento(), r.custoUnitario(), r.precoBase(),
                    r.precosPorCanal());
        }
    }

    public record TamanhoCalculado(UUID id, String nome, BigDecimal fracaoReceita, int rendimento) {
    }
}
