package com.confeitaria.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/** O exemplo do bolo do README, de ponta a ponta pela API: preço, simulação, compra, produção e estoque. */
class FluxoBoloIT extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    private String token;

    private String criar(String path, String json) {
        Resp r = post(path, token, json);
        assertThat(r.status()).as(path + " -> " + r.body()).isEqualTo(201);
        return r.id();
    }

    private String insumo(String nome, String unidade, String qtd, String preco, String minimo) {
        return criar("/insumos", """
                {"nome":"%s","unidade":"%s","quantidadeEmbalagem":%s,"precoEmbalagem":%s,"estoqueMinimo":%s}"""
                .formatted(nome, unidade, qtd, preco, minimo));
    }

    private String embalagem(String nome, String custo) {
        return criar("/embalagens", "{\"nome\":\"%s\",\"custoUnitario\":%s,\"estoqueMinimo\":0}".formatted(nome, custo));
    }

    private void comprar(String campo, String id, String qtd, String unidade, String valor) {
        String un = unidade == null ? "" : ",\"unidade\":\"" + unidade + "\"";
        Resp r = post("/estoque/compras", token, "{\"%s\":\"%s\",\"quantidade\":%s%s,\"valorPago\":%s}"
                .formatted(campo, id, qtd, un, valor));
        assertThat(r.status()).as(r.body()).isEqualTo(201);
    }

    @Test
    void exemploCompletoDoBolo() {
        token = tokenDeNovaConfeiteira("bolos");

        // --- configuração ---
        assertThat(put("/configuracao", token, """
                {"salarioDesejadoMes":4000,"horasTrabalhadasMes":160,"bandeiraVigente":"AMARELA",
                 "gasPrecoBotijao":120,"gasDuracaoHoras":40,"criterioRateio":"HORAS","permitirSaldoNegativo":false}
                """).dec("$.valorHora")).isEqualByComparingTo("25.00");
        criar("/configuracao/tarifas", "{\"tarifaKwh\":0.80,\"vigenciaInicio\":\"2020-01-01\"}");
        criar("/configuracao/bandeiras", "{\"bandeira\":\"AMARELA\",\"adicionalKwh\":0.01885,\"vigenciaInicio\":\"2020-01-01\"}");
        for (var c : Map.of("Aluguel", "500", "Internet", "100", "MEI", "80", "Água", "70").entrySet()) {
            criar("/custos-fixos", "{\"descricao\":\"%s\",\"valorMensal\":%s}".formatted(c.getKey(), c.getValue()));
        }
        criar("/canais-venda", "{\"nome\":\"Balcão (Pix)\",\"taxaPercentual\":0}");
        criar("/canais-venda", "{\"nome\":\"Maquininha\",\"taxaPercentual\":4}");
        criar("/canais-venda", "{\"nome\":\"iFood\",\"taxaPercentual\":27}");
        String batedeira = criar("/equipamentos", "{\"nome\":\"Batedeira\",\"potenciaWatts\":500}");
        String microondas = criar("/equipamentos", "{\"nome\":\"Micro-ondas\",\"potenciaWatts\":1200}");

        // --- insumos e embalagens ---
        String farinha = insumo("Farinha de trigo", "KG", "1", "6.00", "1");
        String acucar = insumo("Açúcar", "KG", "1", "5.00", "0");
        String chocPo = insumo("Chocolate em pó 50%", "G", "200", "9.00", "100");
        String ovos = insumo("Ovos", "UN", "30", "24.00", "0");
        String leite = insumo("Leite", "L", "1", "5.00", "0");
        String oleo = insumo("Óleo", "ML", "900", "9.00", "0");
        String fermento = insumo("Fermento", "G", "100", "4.00", "0");
        String creme = insumo("Creme de leite", "G", "200", "4.00", "0");
        String chocMeio = insumo("Chocolate meio amargo", "KG", "1", "50.00", "0");
        String base = embalagem("Base de papelão 20 cm", "1.50");
        String fatia = embalagem("Embalagem de fatia", "0.90");
        String etiqueta = embalagem("Etiqueta", "0.20");

        assertThat(get("/insumos/" + farinha, token).dec("$.custoUnitario")).isEqualByComparingTo("0.006");

        // --- receita: base por lote, fatia e etiqueta por unidade ---
        String receita = criar("/receitas", """
                {"nome":"Bolo de chocolate 20 cm","rendimento":10,"tempoPreparoMin":150,"minutosGas":45,"margemLucro":30,
                 "insumos":[
                   {"insumoId":"%s","quantidade":300},{"insumoId":"%s","quantidade":250},
                   {"insumoId":"%s","quantidade":80},{"insumoId":"%s","quantidade":4},
                   {"insumoId":"%s","quantidade":0.24,"unidade":"L"},{"insumoId":"%s","quantidade":120},
                   {"insumoId":"%s","quantidade":15},{"insumoId":"%s","quantidade":400},
                   {"insumoId":"%s","quantidade":300,"unidade":"G"}],
                 "embalagens":[
                   {"embalagemId":"%s","quantidade":1,"base":"POR_LOTE"},
                   {"embalagemId":"%s","quantidade":1,"base":"POR_UNIDADE"},
                   {"embalagemId":"%s","quantidade":1,"base":"POR_UNIDADE"}],
                 "equipamentos":[{"equipamentoId":"%s","minutosUso":15},{"equipamentoId":"%s","minutosUso":5}]}
                """.formatted(farinha, acucar, chocPo, ovos, leite, oleo, fermento, creme, chocMeio, base, fatia,
                etiqueta, batedeira, microondas));

        // --- preço ---
        Resp preco = get("/receitas/" + receita + "/preco", token);
        assertThat(preco.status()).as(preco.body()).isEqualTo(200);
        assertThat(preco.dec("$.composicaoLote.insumos")).isEqualByComparingTo("35.85");
        assertThat(preco.dec("$.composicaoLote.embalagens")).isEqualByComparingTo("12.50");
        assertThat(preco.dec("$.composicaoLote.maoDeObra")).isEqualByComparingTo("62.50");
        assertThat(preco.dec("$.composicaoLote.energia")).isEqualByComparingTo("0.18");
        assertThat(preco.dec("$.composicaoLote.gas")).isEqualByComparingTo("2.25");
        assertThat(preco.dec("$.composicaoLote.custosFixos")).isEqualByComparingTo("11.72");
        assertThat(preco.dec("$.composicaoLote.total")).isEqualByComparingTo("125.00");
        assertThat(preco.dec("$.custoUnitario")).isEqualByComparingTo("12.50");
        assertThat(preco.dec("$.precoBase.preco")).isEqualByComparingTo("17.86");
        assertThat(preco.dec("$.precosPorCanal[?(@.canal=='iFood')].preco")).isEqualByComparingTo("29.07");
        assertThat(preco.dec("$.precosPorCanal[?(@.canal=='Maquininha')].preco")).isEqualByComparingTo("18.94");

        // --- simulação: nada é salvo ---
        Resp sim = post("/receitas/" + receita + "/simulacao", token, "{\"margemLucro\":40}");
        assertThat(sim.dec("$.precoBase.preco")).isEqualByComparingTo("20.83");
        assertThat((Boolean) sim.json("$.simulacao")).isTrue();
        Resp semBandeira = post("/receitas/" + receita + "/simulacao", token, "{\"bandeira\":\"VERMELHA_2\"}");
        assertThat(semBandeira.status()).isEqualTo(422);
        assertThat((String) semBandeira.json("$.detail")).contains("VERMELHA_2");
        Resp inviavel = post("/receitas/" + receita + "/simulacao", token, "{\"margemLucro\":80,\"taxaPercentual\":20}");
        assertThat(inviavel.status()).isEqualTo(422);
        assertThat(inviavel.<List<?>>json("$.canaisInvalidos")).hasSize(1);
        assertThat(get("/receitas/" + receita, token).dec("$.margemLucro")).isEqualByComparingTo("30");

        // --- compras (mesmo custo do cadastro, então o preço não muda) ---
        comprar("insumoId", farinha, "2", "KG", "12.00");
        comprar("insumoId", acucar, "1", "KG", "5.00");
        comprar("insumoId", chocPo, "200", null, "9.00");
        comprar("insumoId", ovos, "12", null, "9.60");
        comprar("insumoId", leite, "1", "L", "5.00");
        comprar("insumoId", oleo, "900", null, "9.00");
        comprar("insumoId", fermento, "100", null, "4.00");
        comprar("insumoId", creme, "1000", null, "20.00");
        comprar("insumoId", chocMeio, "1", "KG", "50.00");
        comprar("embalagemId", base, "5", null, "7.50");
        comprar("embalagemId", fatia, "30", null, "27.00");
        comprar("embalagemId", etiqueta, "30", null, "6.00");
        assertThat(get("/receitas/" + receita + "/preco", token).dec("$.precoBase.preco")).isEqualByComparingTo("17.86");

        // --- capacidade ---
        Resp cap = get("/receitas/" + receita + "/capacidade-producao", token);
        assertThat(cap.count("$.lotesPossiveis")).isEqualTo(2);
        assertThat(cap.count("$.unidadesPossiveis")).isEqualTo(20);
        assertThat((String) cap.json("$.fatorLimitante.item")).isEqualTo("Chocolate em pó 50%");

        // --- produção de 2 lotes ---
        Resp prod = post("/producoes", token, "{\"receitaId\":\"%s\",\"lotes\":2}".formatted(receita));
        assertThat(prod.status()).as(prod.body()).isEqualTo(201);
        assertThat(prod.dec("$.custoMateriais")).isEqualByComparingTo("96.70");
        assertThat(prod.count("$.unidadesProduzidas")).isEqualTo(20);
        assertThat(prod.<List<?>>json("$.baixas")).hasSize(12);
        assertThat(get("/insumos/" + farinha, token).dec("$.saldo")).isEqualByComparingTo("1400");
        assertThat(get("/insumos/" + ovos, token).dec("$.saldo")).isEqualByComparingTo("4");
        assertThat(get("/embalagens/" + fatia, token).dec("$.saldo")).isEqualByComparingTo("10");
        assertThat(get("/embalagens/" + base, token).dec("$.saldo")).isEqualByComparingTo("3");
        assertThat(get("/producoes/" + prod.id(), token).<List<?>>json("$.baixas")).hasSize(12);

        // --- terceiro lote: falta chocolate em pó e creme de leite ---
        Resp falta = post("/producoes", token, "{\"receitaId\":\"%s\",\"lotes\":1}".formatted(receita));
        assertThat(falta.status()).isEqualTo(422);
        assertThat(falta.<List<String>>json("$.itensFaltantes[*].item"))
                .containsExactlyInAnyOrder("Chocolate em pó 50%", "Creme de leite");
        assertThat(get("/insumos/" + farinha, token).dec("$.saldo")).isEqualByComparingTo("1400");

        // --- abaixo do mínimo ---
        assertThat(get("/estoque/abaixo-minimo", token).<List<String>>json("$[*].nome"))
                .containsExactly("Chocolate em pó 50%");

        // --- nova compra mais barata: custo médio ponderado e reflexo imediato no preço ---
        Resp compra = post("/estoque/compras", token,
                "{\"insumoId\":\"%s\",\"quantidade\":5,\"unidade\":\"KG\",\"valorPago\":27.50}".formatted(farinha));
        assertThat(compra.dec("$.custoUnitarioAnterior")).isEqualByComparingTo("0.006");
        assertThat(compra.dec("$.custoUnitarioAtual")).isEqualByComparingTo("0.005609");
        assertThat(compra.dec("$.saldoAtual")).isEqualByComparingTo("6400");
        assertThat(get("/receitas/" + receita + "/preco", token).dec("$.precoBase.preco")).isEqualByComparingTo("17.84");

        // --- histórico ---
        assertThat(get("/estoque/movimentacoes?tipo=SAIDA", token).count("$.totalElements")).isEqualTo(12);
        assertThat(get("/estoque/movimentacoes?tipo=ENTRADA&insumoId=" + farinha, token).count("$.totalElements"))
                .isEqualTo(2);

        // --- ajuste de perda e bloqueio de saldo negativo ---
        assertThat(post("/estoque/ajustes", token, "{\"insumoId\":\"%s\",\"quantidade\":-2,\"motivo\":\"PERDA\"}"
                .formatted(ovos)).status()).isEqualTo(201);
        assertThat(post("/estoque/ajustes", token, "{\"insumoId\":\"%s\",\"quantidade\":-3,\"motivo\":\"PERDA\"}"
                .formatted(ovos)).status()).isEqualTo(422);
        assertThat(post("/estoque/ajustes", token, "{\"insumoId\":\"%s\",\"quantidade\":1,\"motivo\":\"COMPRA\"}"
                .formatted(ovos)).status()).isEqualTo(400);
    }

    @Test
    void historicoDeMovimentacaoEhImutavelNoBanco() {
        token = tokenDeNovaConfeiteira("imutavel");
        String id = insumo("Sal", "G", "1000", "3.00", "0");
        comprar("insumoId", id, "1000", null, "3.00");

        assertThatThrownBy(() -> jdbc.update("update movimentacao_estoque set quantidade = 999999"))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("imutável");
        assertThatThrownBy(() -> jdbc.update("delete from movimentacao_estoque"))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("imutável");
    }
}
