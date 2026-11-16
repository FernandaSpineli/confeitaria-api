package com.confeitaria.integracao;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/**
 * 20 produções simultâneas disputando um estoque que só comporta 10 lotes.
 * O lock otimista (@Version) com retry garante que o saldo nunca fica inconsistente nem negativo.
 */
class ConcorrenciaProducaoIT extends AbstractIntegrationTest {

    @Test
    void producoesSimultaneasNaoCorrompemOSaldo() throws Exception {
        String token = tokenDeNovaConfeiteira("concorrencia");
        String insumo = post("/insumos", token, """
                {"nome":"Leite condensado","unidade":"G","quantidadeEmbalagem":395,"precoEmbalagem":7.90,"estoqueMinimo":0}
                """).id();
        post("/estoque/compras", token, "{\"insumoId\":\"%s\",\"quantidade\":1000,\"valorPago\":20}".formatted(insumo));
        String receita = post("/receitas", token, """
                {"nome":"Brigadeiro","rendimento":25,"tempoPreparoMin":40,"margemLucro":30,
                 "insumos":[{"insumoId":"%s","quantidade":100}]}""".formatted(insumo)).id();

        int threads = 20;
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Integer>> resultados = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            for (int i = 0; i < threads; i++) {
                resultados.add(pool.submit(() -> {
                    largada.await();
                    return post("/producoes", token, "{\"receitaId\":\"%s\",\"lotes\":1}".formatted(receita)).status();
                }));
            }
            largada.countDown();
        }

        List<Integer> status = new ArrayList<>();
        for (Future<Integer> f : resultados) {
            status.add(f.get());
        }
        long sucessos = status.stream().filter(s -> s == 201).count();
        long semEstoque = status.stream().filter(s -> s == 422).count();
        long conflitos = status.stream().filter(s -> s == 409).count();

        assertThat(sucessos + semEstoque + conflitos).as("status: " + status).isEqualTo(threads);
        assertThat(sucessos).isBetween(1L, 10L);

        BigDecimal saldo = get("/insumos/" + insumo, token).dec("$.saldo");
        assertThat(saldo).isEqualByComparingTo(BigDecimal.valueOf(1000 - 100 * sucessos));
        assertThat(saldo.signum()).isGreaterThanOrEqualTo(0);
        assertThat(get("/estoque/movimentacoes?tipo=SAIDA", token).count("$.totalElements")).isEqualTo(sucessos);
        assertThat(get("/producoes", token).count("$.totalElements")).isEqualTo(sucessos);
    }
}
