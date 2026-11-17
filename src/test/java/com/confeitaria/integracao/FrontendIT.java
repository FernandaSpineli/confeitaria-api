package com.confeitaria.integracao;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
        assertThat(page.body()).contains("Content-Security-Policy").contains("<div id=\"app\">");

        // The React build (Vite) references hashed bundles; every one of them must be served.
        Matcher m = Pattern.compile("(?:src|href)=\"(/assets/[^\"]+)\"").matcher(page.body());
        List<String> assets = new ArrayList<>();
        while (m.find()) {
            assets.add(m.group(1));
        }
        assertThat(assets).anyMatch(a -> a.endsWith(".js")).anyMatch(a -> a.endsWith(".css")).contains("/assets/favicon.svg");
        for (String asset : assets) {
            assertThat(send("GET", asset).statusCode()).as(asset).isEqualTo(200);
        }
        assertThat(page.body()).as("no inline scripts (CSP script-src self)").doesNotContain("<script>");
    }

    @Test
    void mantemFechadosCaminhosDesconhecidosEOutrosMetodos() throws Exception {
        assertThat(send("GET", "/qualquer-coisa").statusCode()).isEqualTo(401);
        assertThat(send("POST", "/assets/favicon.svg").statusCode()).isEqualTo(401);
        assertThat(send("GET", "/api/v1/insumos").statusCode()).isEqualTo(401);
    }
}
