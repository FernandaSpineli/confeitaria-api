package com.confeitaria.integracao;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

/** O frontend é conteúdo estático público; fora da API, todo o resto continua fechado. */
class FrontendIT extends AbstractIntegrationTest {

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> send(String method, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void servePaginaEArquivosSemAutenticacao() throws Exception {
        HttpResponse<String> page = send("GET", "/");
        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.headers().firstValue("Content-Type")).hasValueSatisfying(t -> assertThat(t).startsWith("text/html"));
        assertThat(page.body()).contains("/assets/js/app.js").contains("Content-Security-Policy");

        for (String asset : new String[] {"/assets/app.css", "/assets/js/app.js", "/assets/js/api.js",
                "/assets/js/views/receitas.js", "/assets/favicon.svg"}) {
            assertThat(send("GET", asset).statusCode()).as(asset).isEqualTo(200);
        }
    }

    @Test
    void mantemFechadosCaminhosDesconhecidosEOutrosMetodos() throws Exception {
        assertThat(send("GET", "/qualquer-coisa").statusCode()).isEqualTo(401);
        assertThat(send("POST", "/assets/app.css").statusCode()).isEqualTo(401);
        assertThat(send("GET", "/api/v1/insumos").statusCode()).isEqualTo(401);
    }
}
