package com.confeitaria.integracao;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Sobe a aplicação inteira (HTTP real, Spring Security, Flyway) contra um PostgreSQL em container.
 * O container é único para toda a suíte, e o contexto do Spring é reaproveitado entre as classes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.jwt.secret=segredo-de-teste-com-mais-de-32-bytes-0123456789",
        "app.admin.email=" + AbstractIntegrationTest.ADMIN_EMAIL,
        "app.admin.password=" + AbstractIntegrationTest.ADMIN_SENHA})
public abstract class AbstractIntegrationTest {

    static final String ADMIN_EMAIL = "admin@teste.local";
    static final String ADMIN_SENHA = "admin-senha-123";
    static final String SENHA = "senha-segura-123";

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @LocalServerPort
    int port;

    record Resp(int status, String body) {

        <T> T json(String path) {
            return JsonPath.read(body, path);
        }

        String id() {
            return json("$.id");
        }

        BigDecimal dec(String path) {
            Object v = json(path);
            if (v instanceof java.util.List<?> lista) {
                v = lista.getFirst();
            }
            return new BigDecimal(String.valueOf(v));
        }

        long count(String path) {
            return ((Number) json(path)).longValue();
        }
    }

    Resp get(String path, String token) {
        return enviar("GET", path, token, null);
    }

    Resp post(String path, String token, String json) {
        return enviar("POST", path, token, json);
    }

    Resp put(String path, String token, String json) {
        return enviar("PUT", path, token, json);
    }

    Resp patch(String path, String token, String json) {
        return enviar("PATCH", path, token, json);
    }

    Resp delete(String path, String token) {
        return enviar("DELETE", path, token, null);
    }

    private Resp enviar(String metodo, String path, String token, String json) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path))
                .method(metodo, json == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json");
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
        try {
            HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
            return new Resp(r.statusCode(), r.body());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Cadastra uma confeiteira com e-mail único e devolve o resultado do login (access + refresh). */
    Resp novaConfeiteira(String apelido) {
        String email = apelido + "-" + UUID.randomUUID() + "@teste.local";
        Resp cadastro = post("/auth/register", null,
                "{\"nome\":\"%s\",\"email\":\"%s\",\"senha\":\"%s\"}".formatted(apelido, email, SENHA));
        if (cadastro.status() != 201) {
            throw new IllegalStateException("cadastro falhou: " + cadastro);
        }
        return login(email, SENHA);
    }

    String tokenDeNovaConfeiteira(String apelido) {
        return novaConfeiteira(apelido).json("$.accessToken");
    }

    Resp login(String email, String senha) {
        return post("/auth/login", null, "{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha));
    }
}
