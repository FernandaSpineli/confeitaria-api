package com.confeitaria.precificacao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.confeitaria.precificacao.domain.ParametrosPrecificacao.CanalTaxa;
import com.confeitaria.precificacao.domain.ParametrosPrecificacao.Energia;
import com.confeitaria.precificacao.domain.ParametrosPrecificacao.Gas;
import com.confeitaria.precificacao.domain.ParametrosPrecificacao.ItemCusto;
import com.confeitaria.precificacao.domain.ParametrosPrecificacao.MaoDeObra;
import com.confeitaria.precificacao.domain.ParametrosPrecificacao.RateioCustosFixos;
import com.confeitaria.precificacao.domain.ParametrosPrecificacao.RateioCustosFixos.Criterio;
import com.confeitaria.precificacao.domain.ParametrosPrecificacao.UsoEquipamento;
import com.confeitaria.precificacao.domain.ResultadoPrecificacao.Componente;
import com.confeitaria.precificacao.domain.ResultadoPrecificacao.LinhaCusto;
import com.confeitaria.precificacao.domain.ResultadoPrecificacao.PrecoCalculado;
import com.confeitaria.shared.domain.BaseQuantidade;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CalculadoraPrecoTest {

    private final CalculadoraPreco calculadora = new CalculadoraPreco();

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    private static ItemCusto item(String nome, String unidade, String qtd, BaseQuantidade base, String custo) {
        return new ItemCusto(nome, unidade, d(qtd), base, d(custo));
    }

    /** Bolo de chocolate 20 cm vendido em 10 fatias: o mesmo exemplo documentado no README. */
    static ParametrosPrecificacao bolo() {
        return new ParametrosPrecificacao(10, 150, 45, d("0.30"),
                List.of(
                        item("Farinha de trigo", "g", "300", BaseQuantidade.POR_LOTE, "0.006"),
                        item("Açúcar", "g", "250", BaseQuantidade.POR_LOTE, "0.005"),
                        item("Chocolate em pó 50%", "g", "80", BaseQuantidade.POR_LOTE, "0.045"),
                        item("Ovos", "un", "4", BaseQuantidade.POR_LOTE, "0.80"),
                        item("Leite", "ml", "240", BaseQuantidade.POR_LOTE, "0.005"),
                        item("Óleo", "ml", "120", BaseQuantidade.POR_LOTE, "0.01"),
                        item("Fermento", "g", "15", BaseQuantidade.POR_LOTE, "0.04"),
                        item("Creme de leite", "g", "400", BaseQuantidade.POR_LOTE, "0.02"),
                        item("Chocolate meio amargo", "g", "300", BaseQuantidade.POR_LOTE, "0.05")),
                List.of(
                        item("Base de papelão 20 cm", "un", "1", BaseQuantidade.POR_LOTE, "1.50"),
                        item("Embalagem de fatia", "un", "1", BaseQuantidade.POR_UNIDADE, "0.90"),
                        item("Etiqueta", "un", "1", BaseQuantidade.POR_UNIDADE, "0.20")),
                List.of(new UsoEquipamento("Batedeira", d("500"), 15), new UsoEquipamento("Micro-ondas", d("1200"), 5)),
                new MaoDeObra(d("4000"), d("160")),
                new Energia(d("0.80"), d("0.01885")),
                new Gas(d("120"), d("40")),
                new RateioCustosFixos(Criterio.HORAS, d("750"), d("160"), 0),
                List.of(new CanalTaxa(UUID.randomUUID(), "Balcão (Pix)", d("0")),
                        new CanalTaxa(UUID.randomUUID(), "Maquininha", d("0.04")),
                        new CanalTaxa(UUID.randomUUID(), "iFood", d("0.27"))));
    }

    /** Receita mínima: um insumo, sem mão de obra, energia, gás, fixos ou canais. */
    private static ParametrosPrecificacao simples(String custoUnitarioInsumo, String margem, List<CanalTaxa> canais) {
        return new ParametrosPrecificacao(1, 0, 0, d(margem),
                List.of(item("Insumo", "un", "1", BaseQuantidade.POR_LOTE, custoUnitarioInsumo)), List.of(), List.of(),
                new MaoDeObra(BigDecimal.ZERO, d("160")), null, null,
                new RateioCustosFixos(Criterio.HORAS, BigDecimal.ZERO, d("160"), 0), canais);
    }

    @Nested
    @DisplayName("Exemplo completo do bolo")
    class ExemploBolo {

        private final ResultadoPrecificacao r = calculadora.calcular(bolo());

        @Test
        void composicaoDoCustoDoLote() {
            var c = r.composicaoLote();
            assertThat(c.insumos()).isEqualByComparingTo("35.85");
            assertThat(c.embalagens()).isEqualByComparingTo("12.50");
            assertThat(c.maoDeObra()).isEqualByComparingTo("62.50");
            assertThat(c.energia()).isEqualByComparingTo("0.18");
            assertThat(c.gas()).isEqualByComparingTo("2.25");
            assertThat(c.custosFixos()).isEqualByComparingTo("11.72");
            assertThat(c.total()).isEqualByComparingTo("125.00");
            assertThat(r.custoUnitario()).isEqualByComparingTo("12.50");
        }

        @Test
        void precoBaseEPorCanal() {
            assertThat(r.precoBase().preco()).isEqualByComparingTo("17.86");
            assertThat(r.precoBase().lucroUnitario()).isEqualByComparingTo("5.36");

            assertThat(r.precosPorCanal()).extracting(PrecoCalculado::canal, PrecoCalculado::preco,
                            PrecoCalculado::valorTaxa, PrecoCalculado::lucroUnitario)
                    .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple("Balcão (Pix)", d("17.86"), d("0.00"), d("5.36")),
                            org.assertj.core.groups.Tuple.tuple("Maquininha", d("18.94"), d("0.76"), d("5.68")),
                            org.assertj.core.groups.Tuple.tuple("iFood", d("29.07"), d("7.85"), d("8.72")));
        }

        @Test
        void detalhamentoExplicaCadaComponente() {
            assertThat(r.detalhamento()).extracting(LinhaCusto::componente).contains(Componente.INSUMO,
                    Componente.EMBALAGEM, Componente.MAO_DE_OBRA, Componente.ENERGIA, Componente.GAS,
                    Componente.CUSTOS_FIXOS);
            LinhaCusto fatias = r.detalhamento().stream().filter(l -> l.item().equals("Embalagem de fatia")).findFirst()
                    .orElseThrow();
            assertThat(fatias.quantidade()).isEqualByComparingTo("10");
            assertThat(fatias.custo()).isEqualByComparingTo("9.00");
            LinhaCusto maoDeObra = r.detalhamento().stream().filter(l -> l.componente() == Componente.MAO_DE_OBRA)
                    .findFirst().orElseThrow();
            assertThat(maoDeObra.quantidade()).isEqualByComparingTo("2.5");
            assertThat(maoDeObra.custoUnitario()).isEqualByComparingTo("25.00");
        }

        @Test
        void precoArredondadoParaDuasCasas() {
            assertThat(r.precoBase().preco().scale()).isEqualTo(2);
            assertThat(r.custoUnitario().scale()).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("Usa margem sobre o preço, não markup: custo 10 e margem 30% dão 14,29 (e não 13,00)")
    void margemSobrePrecoNaoMarkup() {
        ResultadoPrecificacao r = calculadora.calcular(simples("10", "0.30", List.of()));
        assertThat(r.precoBase().preco()).isEqualByComparingTo("14.29");
        assertThat(r.precoBase().lucroUnitario()).isEqualByComparingTo("4.29");
    }

    @Test
    void taxaDoCanalEntraNoDenominador() {
        ResultadoPrecificacao r = calculadora.calcular(
                simples("10", "0.30", List.of(new CanalTaxa(null, "Cartão", d("0.05")))));
        // 10 / (1 - 0,30 - 0,05) = 15,3846...
        PrecoCalculado cartao = r.precosPorCanal().getFirst();
        assertThat(cartao.preco()).isEqualByComparingTo("15.38");
        assertThat(cartao.valorTaxa()).isEqualByComparingTo("0.77");
        assertThat(cartao.lucroUnitario()).isEqualByComparingTo("4.61");
    }

    @Test
    void itensPorUnidadeSaoMultiplicadosPeloRendimento() {
        var p = new ParametrosPrecificacao(30, 0, 0, d("0"),
                List.of(item("Leite condensado", "g", "395", BaseQuantidade.POR_LOTE, "0.02")),
                List.of(item("Forminha", "un", "1", BaseQuantidade.POR_UNIDADE, "0.05")), List.of(),
                new MaoDeObra(BigDecimal.ZERO, d("160")), null, null,
                new RateioCustosFixos(Criterio.HORAS, BigDecimal.ZERO, d("160"), 0), List.of());
        ResultadoPrecificacao r = calculadora.calcular(p);
        assertThat(r.composicaoLote().insumos()).isEqualByComparingTo("7.90");
        assertThat(r.composicaoLote().embalagens()).isEqualByComparingTo("1.50");
        assertThat(r.custoUnitario()).isEqualByComparingTo("0.31");
    }

    @Test
    void rateioPorVendasDivideOsFixosPelasUnidadesEstimadas() {
        var base = simples("0", "0", List.of());
        var p = new ParametrosPrecificacao(10, 0, 0, base.margemLucro(), base.insumos(), List.of(), List.of(),
                base.maoDeObra(), null, null, new RateioCustosFixos(Criterio.VENDAS, d("750"), d("160"), 300),
                List.of());
        ResultadoPrecificacao r = calculadora.calcular(p);
        // 750 / 300 un = 2,50 por unidade × 10 unidades
        assertThat(r.composicaoLote().custosFixos()).isEqualByComparingTo("25.00");
    }

    @Test
    void rateioPorVendasSemEstimativaFalha() {
        var base = simples("1", "0", List.of());
        var p = new ParametrosPrecificacao(1, 0, 0, base.margemLucro(), base.insumos(), List.of(), List.of(),
                base.maoDeObra(), null, null, new RateioCustosFixos(Criterio.VENDAS, d("750"), d("160"), 0), List.of());
        assertThatThrownBy(() -> calculadora.calcular(p)).isInstanceOf(PrecificacaoException.class)
                .hasMessageContaining("vendas estimadas");
    }

    @Test
    void semCustosFixosNaoHaRateio() {
        ResultadoPrecificacao r = calculadora.calcular(simples("5", "0", List.of()));
        assertThat(r.composicaoLote().custosFixos()).isEqualByComparingTo("0");
        assertThat(r.detalhamento()).extracting(LinhaCusto::componente).doesNotContain(Componente.CUSTOS_FIXOS);
    }

    @Test
    void bandeiraMaisCaraEncareceAEnergia() {
        var b = bolo();
        var vermelha = new ParametrosPrecificacao(b.rendimento(), b.tempoPreparoMin(), b.minutosGas(), b.margemLucro(),
                b.insumos(), b.embalagens(), b.equipamentos(), b.maoDeObra(), new Energia(d("0.80"), d("0.07877")),
                b.gas(), b.rateio(), b.canais());
        assertThat(calculadora.calcular(vermelha).composicaoLote().energia())
                .isGreaterThan(calculadora.calcular(b).composicaoLote().energia());
    }

    @Nested
    @DisplayName("Validação de margem + taxas")
    class Margem {

        @Test
        void margemMaisTaxaIgualA100PorCentoEhInviavel() {
            var p = simples("10", "0.30", List.of(new CanalTaxa(null, "Ok", d("0.6999")),
                    new CanalTaxa(null, "Marketplace", d("0.70"))));
            assertThatThrownBy(() -> calculadora.calcular(p))
                    .isInstanceOfSatisfying(MargemInviavelException.class, e -> {
                        assertThat(e.canaisInvalidos()).hasSize(1);
                        assertThat(e.canaisInvalidos().getFirst().canal()).isEqualTo("Marketplace");
                        assertThat(e.canaisInvalidos().getFirst().somaPercentual()).isEqualByComparingTo("100.00");
                    })
                    .hasMessageContaining("Marketplace").hasMessageContaining("menor que 100%");
        }

        @Test
        void margemMaisTaxaAcimaDe100ListaTodosOsCanaisInvalidos() {
            var p = simples("10", "0.50", List.of(new CanalTaxa(null, "A", d("0.60")), new CanalTaxa(null, "B", d("0.55")),
                    new CanalTaxa(null, "C", d("0.10"))));
            assertThatThrownBy(() -> calculadora.calcular(p))
                    .isInstanceOfSatisfying(MargemInviavelException.class, e -> assertThat(e.canaisInvalidos())
                            .extracting(MargemInviavelException.CanalInviavel::canal).containsExactly("A", "B"));
        }

        @Test
        void margemSozinhaDe100PorCentoEhInviavel() {
            assertThatThrownBy(() -> calculadora.calcular(simples("10", "1.00", List.of())))
                    .isInstanceOf(MargemInviavelException.class);
        }

        @Test
        void margemZeroVendePeloCusto() {
            ResultadoPrecificacao r = calculadora.calcular(simples("10", "0", List.of()));
            assertThat(r.precoBase().preco()).isEqualByComparingTo("10.00");
            assertThat(r.precoBase().lucroUnitario()).isEqualByComparingTo("0.00");
        }
    }

    @Nested
    @DisplayName("Dados obrigatórios conforme o uso")
    class DadosObrigatorios {

        @Test
        void equipamentoSemTarifaFalha() {
            var b = bolo();
            var p = new ParametrosPrecificacao(b.rendimento(), b.tempoPreparoMin(), b.minutosGas(), b.margemLucro(),
                    b.insumos(), b.embalagens(), b.equipamentos(), b.maoDeObra(), null, b.gas(), b.rateio(), b.canais());
            assertThatThrownBy(() -> calculadora.calcular(p)).isInstanceOf(PrecificacaoException.class)
                    .hasMessageContaining("tarifa");
        }

        @Test
        void fornoAGasSemConfiguracaoFalha() {
            var b = bolo();
            var p = new ParametrosPrecificacao(b.rendimento(), b.tempoPreparoMin(), b.minutosGas(), b.margemLucro(),
                    b.insumos(), b.embalagens(), b.equipamentos(), b.maoDeObra(), b.energia(), null, b.rateio(),
                    b.canais());
            assertThatThrownBy(() -> calculadora.calcular(p)).isInstanceOf(PrecificacaoException.class)
                    .hasMessageContaining("gás");
        }

        @Test
        void semEquipamentosNemGasNaoExigeTarifas() {
            ResultadoPrecificacao r = calculadora.calcular(simples("3", "0.2", List.of()));
            assertThat(r.composicaoLote().energia()).isEqualByComparingTo("0");
            assertThat(r.composicaoLote().gas()).isEqualByComparingTo("0");
        }

        @Test
        void rendimentoZeroFalha() {
            var s = simples("1", "0", List.of());
            var p = new ParametrosPrecificacao(0, 0, 0, s.margemLucro(), s.insumos(), List.of(), List.of(),
                    s.maoDeObra(), null, null, s.rateio(), List.of());
            assertThatThrownBy(() -> calculadora.calcular(p)).isInstanceOf(PrecificacaoException.class)
                    .hasMessageContaining("rendimento");
        }
    }

    @Nested
    @DisplayName("Arredondamento HALF_EVEN")
    class Arredondamento {

        @Test
        void meioArredondaParaOParMaisProximo() {
            assertThat(calculadora.calcular(simples("1.005", "0", List.of())).precoBase().preco())
                    .isEqualByComparingTo("1.00");
            assertThat(calculadora.calcular(simples("1.015", "0", List.of())).precoBase().preco())
                    .isEqualByComparingTo("1.02");
        }

        @Test
        void componentesSaoSomadosSemArredondarAntes() {
            // 3 itens de 0,004 = 0,012: arredondar cada um antes daria 0,00
            var p = new ParametrosPrecificacao(1, 0, 0, d("0"),
                    List.of(item("A", "g", "1", BaseQuantidade.POR_LOTE, "0.004"),
                            item("B", "g", "1", BaseQuantidade.POR_LOTE, "0.004"),
                            item("C", "g", "1", BaseQuantidade.POR_LOTE, "0.004")),
                    List.of(), List.of(), new MaoDeObra(BigDecimal.ZERO, d("160")), null, null,
                    new RateioCustosFixos(Criterio.HORAS, BigDecimal.ZERO, d("160"), 0), List.of());
            assertThat(calculadora.calcular(p).composicaoLote().insumos()).isEqualByComparingTo("0.01");
        }
    }

    @Nested
    @DisplayName("Tamanhos: torta holandesa grande, pequena e no pote")
    class Tamanhos {

        /** Receita inteira: produtos R$ 55, 1 h de trabalho a R$ 10/h, margem 40%, sem embalagem própria. */
        private ParametrosPrecificacao torta(RateioCustosFixos rateio) {
            return new ParametrosPrecificacao(1, 60, 0, d("0.40"),
                    List.of(item("Produtos", "un", "1", BaseQuantidade.POR_LOTE, "55.00")), List.of(), List.of(),
                    new MaoDeObra(d("1600"), d("160")), null, null, rateio, List.of());
        }

        private ParametrosPrecificacao torta() {
            return torta(new RateioCustosFixos(Criterio.HORAS, BigDecimal.ZERO, d("160"), 0));
        }

        private static ParametrosPrecificacao.Tamanho tamanho(String nome, String fracao, int pecas) {
            return new ParametrosPrecificacao.Tamanho(nome, d(fracao), pecas,
                    item("Embalagem", "un", "1", BaseQuantidade.POR_UNIDADE, "1.00"));
        }

        @Test
        void tortaGrandeUsaAReceitaInteiraMaisACaixa() {
            var r = calculadora.calcular(torta().comTamanho(tamanho("Grande", "1", 1)));
            assertThat(r.composicaoLote().insumos()).isEqualByComparingTo("55.00");
            assertThat(r.composicaoLote().maoDeObra()).isEqualByComparingTo("10.00");
            assertThat(r.composicaoLote().embalagens()).isEqualByComparingTo("1.00");
            assertThat(r.composicaoLote().total()).isEqualByComparingTo("66.00");
            assertThat(r.rendimento()).isEqualTo(1);
            assertThat(r.precoBase().preco()).isEqualByComparingTo("110.00");
        }

        @Test
        void tortaPequenaUsaMetadeDosProdutosEDoTrabalho() {
            var r = calculadora.calcular(torta().comTamanho(tamanho("Pequena", "0.5", 1)));
            assertThat(r.composicaoLote().insumos()).isEqualByComparingTo("27.50");
            assertThat(r.composicaoLote().maoDeObra()).isEqualByComparingTo("5.00");
            assertThat(r.composicaoLote().embalagens()).isEqualByComparingTo("1.00");
            assertThat(r.composicaoLote().total()).isEqualByComparingTo("33.50");
            assertThat(r.precoBase().preco()).isEqualByComparingTo("55.83");
        }

        @Test
        void potesDividemAReceitaPorOitoEUsamUmPoteCada() {
            var r = calculadora.calcular(torta().comTamanho(tamanho("Pote", "1", 8)));
            assertThat(r.composicaoLote().embalagens()).isEqualByComparingTo("8.00");
            assertThat(r.composicaoLote().total()).isEqualByComparingTo("73.00");
            assertThat(r.rendimento()).isEqualTo(8);
            // 73 / 8 = 9,125: exibido como 9,12 (HALF_EVEN), mas o preço usa o valor exato: 9,125 / 0,60 = 15,208
            assertThat(r.custoUnitario()).isEqualByComparingTo("9.12");
            assertThat(r.precoBase().preco()).isEqualByComparingTo("15.21");
            assertThat(r.detalhamento()).filteredOn(l -> l.componente() == Componente.EMBALAGEM)
                    .singleElement().satisfies(l -> assertThat(l.quantidade()).isEqualByComparingTo("8"));
        }

        @Test
        void semTamanhoOPrecoEDaReceitaInteiraSemEmbalagem() {
            var r = calculadora.calcular(torta());
            assertThat(r.composicaoLote().total()).isEqualByComparingTo("65.00");
            assertThat(r.precoBase().preco()).isEqualByComparingTo("108.33");
        }

        @Test
        void custosFixosPorVendasEntramUmaVezPorPeca() {
            // R$ 100 por mês / 50 vendas = R$ 2 por peça vendida
            var porVendas = torta(new RateioCustosFixos(Criterio.VENDAS, d("100"), d("160"), 50));
            assertThat(calculadora.calcular(porVendas.comTamanho(tamanho("Pote", "1", 8))).composicaoLote()
                    .custosFixos()).isEqualByComparingTo("16.00");
            assertThat(calculadora.calcular(porVendas.comTamanho(tamanho("Pequena", "0.5", 1))).composicaoLote()
                    .custosFixos()).isEqualByComparingTo("2.00");
        }

        @Test
        void custosFixosPorHorasAcompanhamAFracao() {
            // R$ 160 por mês / 160 h = R$ 1 por hora; meia receita = 30 min
            var porHoras = torta(new RateioCustosFixos(Criterio.HORAS, d("160"), d("160"), 0));
            assertThat(calculadora.calcular(porHoras.comTamanho(tamanho("Pequena", "0.5", 1))).composicaoLote()
                    .custosFixos()).isEqualByComparingTo("0.50");
        }

        @Test
        void tamanhoSemPecasEhRejeitado() {
            assertThatThrownBy(() -> calculadora.calcular(torta().comTamanho(tamanho("Vazio", "1", 0))))
                    .isInstanceOf(PrecificacaoException.class).hasMessageContaining("Vazio");
        }
    }
}
