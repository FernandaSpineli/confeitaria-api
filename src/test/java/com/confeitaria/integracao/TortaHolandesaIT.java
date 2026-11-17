package com.confeitaria.integracao;

import static org.assertj.core.api.Assertions.assertThat;

import com.confeitaria.demo.ContaDemonstracao;
import com.confeitaria.auth.application.AuthService;
import com.confeitaria.configuracao.application.ConfiguracaoService;
import com.confeitaria.embalagem.application.EmbalagemService;
import com.confeitaria.estoque.application.EstoqueService;
import com.confeitaria.insumo.application.InsumoService;
import com.confeitaria.receita.application.ReceitaService;
import com.confeitaria.usuario.persistence.UsuarioRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Torta holandesa em três tamanhos: grande (receita inteira), pequena (meia receita) e pote (1 receita = 8 potes).
 * Receita inteira: produtos R$ 55, trabalho R$ 10, embalagem R$ 1 por peça, margem 40%.
 */
class TortaHolandesaIT extends AbstractIntegrationTest {

    @Autowired UsuarioRepository usuarios;
    @Autowired AuthService auth;
    @Autowired ConfiguracaoService configuracao;
    @Autowired InsumoService insumos;
    @Autowired EmbalagemService embalagens;
    @Autowired ReceitaService receitas;
    @Autowired EstoqueService estoque;

    private String token;

    private String criar(String path, String json) {
        Resp r = post(path, token, json);
        assertThat(r.status()).as(path + " -> " + r.body()).isEqualTo(201);
        return r.id();
    }

    private Map<String, String> tamanhos(String receitaId) {
        Resp r = get("/receitas/" + receitaId, token);
        List<String> nomes = r.json("$.tamanhos[*].nome");
        List<String> ids = r.json("$.tamanhos[*].id");
        return Map.of(nomes.get(0), ids.get(0), nomes.get(1), ids.get(1), nomes.get(2), ids.get(2));
    }

    @Test
    void precoEProducaoPorTamanho() {
        token = tokenDeNovaConfeiteira("tortas");
        put("/configuracao", token, """
                {"salarioDesejadoMes":1600,"horasTrabalhadasMes":160,"bandeiraVigente":"VERDE",
                 "criterioRateio":"HORAS","permitirSaldoNegativo":false}""");
        String produtos = criar("/insumos", """
                {"nome":"Produtos da torta","unidade":"UN","quantidadeEmbalagem":1,"precoEmbalagem":55,"estoqueMinimo":0}""");
        String caixa = criar("/embalagens", "{\"nome\":\"Caixa\",\"custoUnitario\":1,\"estoqueMinimo\":0}");
        String pote = criar("/embalagens", "{\"nome\":\"Pote\",\"custoUnitario\":1,\"estoqueMinimo\":0}");

        String torta = criar("/receitas", """
                {"nome":"Torta holandesa","rendimento":1,"tempoPreparoMin":60,"margemLucro":40,
                 "insumos":[{"insumoId":"%s","quantidade":1}],
                 "tamanhos":[
                   {"nome":"Torta grande","fracaoReceita":1,"rendimento":1,"embalagemId":"%s"},
                   {"nome":"Torta pequena","fracaoReceita":0.5,"rendimento":1,"embalagemId":"%s"},
                   {"nome":"Pote individual","fracaoReceita":1,"rendimento":8,"embalagemId":"%s","embalagensPorPeca":1}]}
                """.formatted(produtos, caixa, caixa, pote));
        Map<String, String> t = tamanhos(torta);

        Resp grande = get("/receitas/%s/preco?tamanhoId=%s".formatted(torta, t.get("Torta grande")), token);
        assertThat(grande.dec("$.composicaoLote.total")).isEqualByComparingTo("66.00");
        assertThat(grande.dec("$.precoBase.preco")).isEqualByComparingTo("110.00");
        assertThat((String) grande.json("$.tamanho.nome")).isEqualTo("Torta grande");

        Resp pequena = get("/receitas/%s/preco?tamanhoId=%s".formatted(torta, t.get("Torta pequena")), token);
        assertThat(pequena.dec("$.composicaoLote.insumos")).isEqualByComparingTo("27.50");
        assertThat(pequena.dec("$.composicaoLote.maoDeObra")).isEqualByComparingTo("5.00");
        assertThat(pequena.dec("$.composicaoLote.embalagens")).isEqualByComparingTo("1.00");
        assertThat(pequena.dec("$.precoBase.preco")).isEqualByComparingTo("55.83");

        Resp potes = get("/receitas/%s/preco?tamanhoId=%s".formatted(torta, t.get("Pote individual")), token);
        assertThat(potes.count("$.rendimento")).isEqualTo(8);
        assertThat(potes.dec("$.composicaoLote.total")).isEqualByComparingTo("73.00");
        assertThat(potes.dec("$.precoBase.preco")).isEqualByComparingTo("15.21");

        // simulação também respeita o tamanho
        Resp simulada = post("/receitas/" + torta + "/simulacao", token,
                "{\"margemLucro\":50,\"tamanhoId\":\"%s\"}".formatted(t.get("Torta pequena")));
        assertThat(simulada.dec("$.precoBase.preco")).isEqualByComparingTo("67.00");

        // estoque: 1 receita de produtos, 3 caixas, 8 potes
        post("/estoque/compras", token, "{\"insumoId\":\"%s\",\"quantidade\":1,\"valorPago\":55}".formatted(produtos));
        post("/estoque/compras", token, "{\"embalagemId\":\"%s\",\"quantidade\":3,\"valorPago\":3}".formatted(caixa));
        post("/estoque/compras", token, "{\"embalagemId\":\"%s\",\"quantidade\":8,\"valorPago\":8}".formatted(pote));

        Resp cap = get("/receitas/%s/capacidade-producao?tamanhoId=%s".formatted(torta, t.get("Torta pequena")), token);
        assertThat(cap.count("$.lotesPossiveis")).isEqualTo(2);
        assertThat((String) cap.json("$.tamanho")).isEqualTo("Torta pequena");

        // 2 tortas pequenas = 1 receita inteira + 2 caixas
        Resp producao = post("/producoes", token,
                "{\"receitaId\":\"%s\",\"lotes\":2,\"tamanhoId\":\"%s\"}".formatted(torta, t.get("Torta pequena")));
        assertThat(producao.status()).as(producao.body()).isEqualTo(201);
        assertThat(producao.count("$.unidadesProduzidas")).isEqualTo(2);
        assertThat((String) producao.json("$.tamanho")).isEqualTo("Torta pequena");
        assertThat(get("/insumos/" + produtos, token).dec("$.saldo")).isEqualByComparingTo("0");
        assertThat(get("/embalagens/" + caixa, token).dec("$.saldo")).isEqualByComparingTo("1");

        // sem produtos para os potes: nada é baixado e a falta aparece
        Resp semEstoque = post("/producoes", token,
                "{\"receitaId\":\"%s\",\"lotes\":1,\"tamanhoId\":\"%s\"}".formatted(torta, t.get("Pote individual")));
        assertThat(semEstoque.status()).isEqualTo(422);
        assertThat(get("/embalagens/" + pote, token).dec("$.saldo")).isEqualByComparingTo("8");

        Resp historico = get("/producoes", token);
        assertThat((String) historico.json("$.content[0].tamanho")).isEqualTo("Torta pequena");

        // embalagem usada por um tamanho não pode ser excluída
        assertThat(delete("/embalagens/" + pote, token).status()).isEqualTo(409);
    }

    @Test
    void regrasDosTamanhos() {
        token = tokenDeNovaConfeiteira("regras");
        String insumo = criar("/insumos", """
                {"nome":"Farinha","unidade":"G","quantidadeEmbalagem":1000,"precoEmbalagem":6,"estoqueMinimo":0}""");
        String base = """
                {"nome":"Bolo","rendimento":1,"tempoPreparoMin":0,"margemLucro":30,
                 "insumos":[{"insumoId":"%s","quantidade":100}],"tamanhos":%s}""";

        Resp repetido = post("/receitas", token, base.formatted(insumo,
                "[{\"nome\":\"Fatia\",\"fracaoReceita\":0.1,\"rendimento\":1},{\"nome\":\"fatia\",\"fracaoReceita\":0.2,\"rendimento\":1}]"));
        assertThat(repetido.status()).isEqualTo(422);

        Resp invalido = post("/receitas", token, base.formatted(insumo,
                "[{\"nome\":\"Nada\",\"fracaoReceita\":0,\"rendimento\":1}]"));
        assertThat(invalido.status()).isEqualTo(400);

        String bolo = criar("/receitas", base.formatted(insumo, "[{\"nome\":\"Fatia\",\"fracaoReceita\":0.1,\"rendimento\":1}]"));
        String outra = criar("/receitas", base.formatted(insumo, "[{\"nome\":\"Inteiro\",\"fracaoReceita\":1,\"rendimento\":1}]"));
        String tamanhoDaOutra = get("/receitas/" + outra, token).json("$.tamanhos[0].id");
        assertThat(get("/receitas/%s/preco?tamanhoId=%s".formatted(bolo, tamanhoDaOutra), token).status()).isEqualTo(404);

        // editar substitui os tamanhos
        assertThat(put("/receitas/" + bolo, token, base.formatted(insumo,
                "[{\"nome\":\"Fatia\",\"fracaoReceita\":0.125,\"rendimento\":1},{\"nome\":\"Metade\",\"fracaoReceita\":0.5,\"rendimento\":1}]"))
                .status()).isEqualTo(200);
        assertThat(get("/receitas/" + bolo, token).count("$.tamanhos.length()")).isEqualTo(2);
        List<String> nomes = get("/receitas?nome=Bolo", token).json("$.content[0].tamanhos");
        assertThat(nomes).isNotEmpty();
    }

    @Test
    void contaDeDemonstracaoTemATortaComOsTresPrecos() {
        String email = "demo-" + UUID.randomUUID() + "@teste.local";
        ContaDemonstracao demo = new ContaDemonstracao(usuarios, auth, configuracao, insumos, embalagens, receitas,
                estoque, email, SENHA);
        assertThat(demo.criar(email, SENHA)).isTrue();
        assertThat(demo.criar(email, SENHA)).isFalse();

        token = login(email, SENHA).json("$.accessToken");
        String torta = get("/receitas", token).json("$.content[0].id");
        Map<String, String> t = tamanhos(torta);
        Map<String, String> esperado = Map.of("Torta grande", "110.00", "Torta pequena", "55.83", "Pote individual", "15.21");
        esperado.forEach((nome, preco) -> assertThat(
                get("/receitas/%s/preco?tamanhoId=%s".formatted(torta, t.get(nome)), token).dec("$.precoBase.preco"))
                .as(nome).isEqualByComparingTo(preco));
        assertThat(get("/receitas/%s/preco?tamanhoId=%s".formatted(torta, t.get("Torta pequena")), token)
                .dec("$.composicaoLote.total")).isEqualByComparingTo("33.50");

        // estoque inicial dá para 2 receitas: 4 tortas pequenas
        assertThat(get("/receitas/%s/capacidade-producao?tamanhoId=%s".formatted(torta, t.get("Torta pequena")), token)
                .count("$.lotesPossiveis")).isEqualTo(4);
    }
}
