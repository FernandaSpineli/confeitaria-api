package com.confeitaria.integracao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Garante que uma confeiteira nunca lê nem altera dados de outra. Toda tentativa responde 404
 * (e não 403), para não revelar que o recurso existe.
 */
class IsolamentoEntreUsuariasIT extends AbstractIntegrationTest {

    private String ana;
    private String bia;
    private String insumoAna;
    private String embalagemAna;
    private String equipamentoAna;
    private String custoFixoAna;
    private String canalAna;
    private String tarifaAna;
    private String receitaAna;
    private String producaoAna;

    @BeforeEach
    void dadosDaAna() {
        ana = tokenDeNovaConfeiteira("ana");
        bia = tokenDeNovaConfeiteira("bia");

        insumoAna = post("/insumos", ana, """
                {"nome":"Farinha","unidade":"KG","quantidadeEmbalagem":1,"precoEmbalagem":6.00,"estoqueMinimo":1}
                """).id();
        embalagemAna = post("/embalagens", ana, """
                {"nome":"Caixa","custoUnitario":2.00,"estoqueMinimo":5}""").id();
        equipamentoAna = post("/equipamentos", ana, """
                {"nome":"Batedeira","potenciaWatts":500}""").id();
        custoFixoAna = post("/custos-fixos", ana, """
                {"descricao":"Aluguel","valorMensal":500}""").id();
        canalAna = post("/canais-venda", ana, """
                {"nome":"iFood","taxaPercentual":27}""").id();
        tarifaAna = post("/configuracao/tarifas", ana, """
                {"tarifaKwh":0.80,"vigenciaInicio":"2020-01-01"}""").id();
        receitaAna = post("/receitas", ana, """
                {"nome":"Bolo","rendimento":1,"tempoPreparoMin":60,"margemLucro":30,
                 "insumos":[{"insumoId":"%s","quantidade":300}],
                 "embalagens":[{"embalagemId":"%s","quantidade":1}],
                 "equipamentos":[{"equipamentoId":"%s","minutosUso":10}]}""".formatted(insumoAna, embalagemAna,
                equipamentoAna)).id();
        assertThat(post("/estoque/compras", ana, """
                {"insumoId":"%s","quantidade":2,"unidade":"KG","valorPago":12.00}""".formatted(insumoAna)).status())
                .isEqualTo(201);
        assertThat(post("/estoque/compras", ana, """
                {"embalagemId":"%s","quantidade":10,"valorPago":20.00}""".formatted(embalagemAna)).status())
                .isEqualTo(201);
        producaoAna = post("/producoes", ana, """
                {"receitaId":"%s","lotes":1}""".formatted(receitaAna)).id();
    }

    @Test
    @DisplayName("Bia não lê, não altera e não exclui o insumo da Ana")
    void insumo() {
        assertThat(get("/insumos/" + insumoAna, bia).status()).isEqualTo(404);
        assertThat(put("/insumos/" + insumoAna, bia, """
                {"nome":"Hack","unidade":"G","quantidadeEmbalagem":1,"precoEmbalagem":0.01,"estoqueMinimo":0}""")
                .status()).isEqualTo(404);
        assertThat(delete("/insumos/" + insumoAna, bia).status()).isEqualTo(404);

        Resp daAna = get("/insumos/" + insumoAna, ana);
        assertThat(daAna.status()).isEqualTo(200);
        assertThat((String) daAna.json("$.nome")).isEqualTo("Farinha");
        assertThat((Boolean) daAna.json("$.ativo")).isTrue();
    }

    @Test
    @DisplayName("Bia não acessa embalagem, equipamento, custo fixo, canal nem tarifa da Ana")
    void demaisCadastros() {
        assertThat(get("/embalagens/" + embalagemAna, bia).status()).isEqualTo(404);
        assertThat(delete("/embalagens/" + embalagemAna, bia).status()).isEqualTo(404);
        assertThat(get("/equipamentos/" + equipamentoAna, bia).status()).isEqualTo(404);
        assertThat(put("/equipamentos/" + equipamentoAna, bia, "{\"nome\":\"X\",\"potenciaWatts\":1}").status())
                .isEqualTo(404);
        assertThat(get("/custos-fixos/" + custoFixoAna, bia).status()).isEqualTo(404);
        assertThat(delete("/custos-fixos/" + custoFixoAna, bia).status()).isEqualTo(404);
        assertThat(get("/canais-venda/" + canalAna, bia).status()).isEqualTo(404);
        assertThat(delete("/configuracao/tarifas/" + tarifaAna, bia).status()).isEqualTo(404);

        assertThat(get("/custos-fixos/" + custoFixoAna, ana).status()).isEqualTo(200);
        assertThat(get("/configuracao/tarifas/vigente", ana).status()).isEqualTo(200);
    }

    @Test
    @DisplayName("Bia não vê a receita, o preço, a simulação nem a capacidade de produção da Ana")
    void receitaEPrecificacao() {
        assertThat(get("/receitas/" + receitaAna, bia).status()).isEqualTo(404);
        assertThat(get("/receitas/" + receitaAna + "/preco", bia).status()).isEqualTo(404);
        assertThat(post("/receitas/" + receitaAna + "/simulacao", bia, "{\"margemLucro\":10}").status())
                .isEqualTo(404);
        assertThat(get("/receitas/" + receitaAna + "/capacidade-producao", bia).status()).isEqualTo(404);
        assertThat(delete("/receitas/" + receitaAna, bia).status()).isEqualTo(404);

        assertThat(get("/receitas/" + receitaAna + "/preco", ana).status()).isEqualTo(200);
    }

    @Test
    @DisplayName("Bia não usa o canal de venda da Ana numa simulação da própria receita")
    void canalAlheioNaSimulacao() {
        String insumoBia = post("/insumos", bia, """
                {"nome":"Açúcar","unidade":"G","quantidadeEmbalagem":1000,"precoEmbalagem":5,"estoqueMinimo":0}
                """).id();
        String receitaBia = post("/receitas", bia, """
                {"nome":"Doce","rendimento":1,"tempoPreparoMin":0,"margemLucro":20,
                 "insumos":[{"insumoId":"%s","quantidade":100}]}""".formatted(insumoBia)).id();

        assertThat(post("/receitas/" + receitaBia + "/simulacao", bia,
                "{\"canalVendaId\":\"%s\"}".formatted(canalAna)).status()).isEqualTo(404);
    }

    @Test
    @DisplayName("Bia não monta receita com insumos, embalagens ou equipamentos da Ana")
    void receitaComItensAlheios() {
        assertThat(post("/receitas", bia, """
                {"nome":"Roubo","rendimento":1,"tempoPreparoMin":10,"margemLucro":30,
                 "insumos":[{"insumoId":"%s","quantidade":100}]}""".formatted(insumoAna)).status()).isEqualTo(404);
        assertThat(post("/receitas", bia, """
                {"nome":"Roubo","rendimento":1,"tempoPreparoMin":10,"margemLucro":30,
                 "embalagens":[{"embalagemId":"%s","quantidade":1}]}""".formatted(embalagemAna)).status())
                .isEqualTo(404);
        assertThat(post("/receitas", bia, """
                {"nome":"Roubo","rendimento":1,"tempoPreparoMin":10,"margemLucro":30,
                 "equipamentos":[{"equipamentoId":"%s","minutosUso":5}]}""".formatted(equipamentoAna)).status())
                .isEqualTo(404);
    }

    @Test
    @DisplayName("Bia não movimenta o estoque da Ana nem produz a receita dela")
    void estoque() {
        String saldoAntes = get("/insumos/" + insumoAna, ana).dec("$.saldo").toPlainString();

        assertThat(post("/estoque/compras", bia, """
                {"insumoId":"%s","quantidade":1000,"valorPago":0.01}""".formatted(insumoAna)).status())
                .isEqualTo(404);
        assertThat(post("/estoque/ajustes", bia, """
                {"insumoId":"%s","quantidade":-100,"motivo":"PERDA"}""".formatted(insumoAna)).status())
                .isEqualTo(404);
        assertThat(post("/producoes", bia, """
                {"receitaId":"%s","lotes":1}""".formatted(receitaAna)).status()).isEqualTo(404);
        assertThat(get("/producoes/" + producaoAna, bia).status()).isEqualTo(404);

        Resp depois = get("/insumos/" + insumoAna, ana);
        assertThat(depois.dec("$.saldo").toPlainString()).isEqualTo(saldoAntes);
        assertThat(depois.dec("$.custoUnitario")).isEqualByComparingTo("0.006");
    }

    @Test
    @DisplayName("Listagens e relatórios só mostram dados da própria usuária")
    void listagens() {
        assertThat(get("/insumos", bia).count("$.totalElements")).isZero();
        assertThat(get("/embalagens", bia).count("$.totalElements")).isZero();
        assertThat(get("/receitas", bia).count("$.totalElements")).isZero();
        assertThat(get("/producoes", bia).count("$.totalElements")).isZero();
        assertThat(get("/estoque/movimentacoes", bia).count("$.totalElements")).isZero();
        assertThat(get("/estoque/movimentacoes?insumoId=" + insumoAna, bia).count("$.totalElements")).isZero();
        assertThat(get("/configuracao/tarifas", bia).count("$.totalElements")).isZero();
        assertThat(get("/estoque/abaixo-minimo", bia).body()).isEqualTo("[]");

        assertThat(get("/insumos", ana).count("$.totalElements")).isEqualTo(1);
        assertThat(get("/estoque/movimentacoes", ana).count("$.totalElements")).isEqualTo(4);
    }

    @Test
    @DisplayName("Configuração de custos é individual")
    void configuracaoIndividual() {
        assertThat(put("/configuracao", ana, """
                {"salarioDesejadoMes":5000,"horasTrabalhadasMes":100,"bandeiraVigente":"AMARELA",
                 "criterioRateio":"HORAS","permitirSaldoNegativo":true}""").status()).isEqualTo(200);

        Resp daBia = get("/configuracao", bia);
        assertThat(daBia.dec("$.salarioDesejadoMes")).isEqualByComparingTo("0");
        assertThat((String) daBia.json("$.bandeiraVigente")).isEqualTo("VERDE");
        assertThat((Boolean) daBia.json("$.permitirSaldoNegativo")).isFalse();
    }

    @Test
    @DisplayName("Perfis: confeiteira não acessa /admin, admin não acessa dados de negócio, anônimo recebe 401")
    void perfis() {
        assertThat(get("/admin/usuarios", ana).status()).isEqualTo(403);

        String admin = login(ADMIN_EMAIL, ADMIN_SENHA).json("$.accessToken");
        assertThat(get("/admin/usuarios", admin).status()).isEqualTo(200);
        assertThat(get("/insumos", admin).status()).isEqualTo(403);
        assertThat(get("/insumos/" + insumoAna, admin).status()).isEqualTo(403);

        Resp anonimo = get("/insumos", null);
        assertThat(anonimo.status()).isEqualTo(401);
        assertThat((String) anonimo.json("$.title")).isEqualTo("Não autenticado");
        assertThat(get("/insumos", "token.invalido.aqui").status()).isEqualTo(401);
    }
}
