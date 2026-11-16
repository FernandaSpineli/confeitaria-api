package com.confeitaria.precificacao.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Valores monetários já arredondados para 2 casas (HALF_EVEN). */
public record ResultadoPrecificacao(
        ComposicaoCusto composicaoLote,
        List<LinhaCusto> detalhamento,
        int rendimento,
        BigDecimal custoUnitario,
        BigDecimal margemLucro,
        PrecoCalculado precoBase,
        List<PrecoCalculado> precosPorCanal) {

    public record ComposicaoCusto(BigDecimal insumos, BigDecimal embalagens, BigDecimal maoDeObra,
            BigDecimal energia, BigDecimal gas, BigDecimal custosFixos, BigDecimal total) {
    }

    public enum Componente { INSUMO, EMBALAGEM, MAO_DE_OBRA, ENERGIA, GAS, CUSTOS_FIXOS }

    /** Uma linha explicando um pedaço do custo do lote: quantidade × custo unitário = custo. */
    public record LinhaCusto(Componente componente, String item, BigDecimal quantidade, String unidade,
            BigDecimal custoUnitario, BigDecimal custo) {
    }

    public record PrecoCalculado(UUID canalId, String canal, BigDecimal taxaPercentual, BigDecimal preco,
            BigDecimal valorTaxa, BigDecimal lucroUnitario) {
    }
}
