package com.confeitaria.integracao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AutenticacaoIT extends AbstractIntegrationTest {

    @Test
    void cadastroDuplicadoRetorna409() {
        String email = "dup-" + UUID.randomUUID() + "@teste.local";
        String corpo = "{\"nome\":\"X\",\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, SENHA);
        assertThat(post("/auth/register", null, corpo).status()).isEqualTo(201);
        assertThat(post("/auth/register", null, corpo.replace(email, email.toUpperCase())).status()).isEqualTo(409);
    }

    @Test
    void validacaoRetornaProblemDetailComCampos() {
        Resp r = post("/auth/register", null, "{\"nome\":\"\",\"email\":\"invalido\",\"senha\":\"123\"}");
        assertThat(r.status()).isEqualTo(400);
        assertThat(r.<Object>json("$.erros.email")).isNotNull();
        assertThat(r.<Object>json("$.erros.senha")).isNotNull();
        assertThat(r.<Object>json("$.erros.nome")).isNotNull();
    }

    @Test
    void loginComSenhaErradaRetorna401() {
        String email = "x-" + UUID.randomUUID() + "@teste.local";
        post("/auth/register", null, "{\"nome\":\"X\",\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, SENHA));
        assertThat(login(email, "errada-123").status()).isEqualTo(401);
        assertThat(login(email, SENHA).status()).isEqualTo(200);
    }

    @Test
    void refreshRotacionaEReusoRevogaTodasAsSessoes() {
        Resp sessao = novaConfeiteira("carla");
        String refresh1 = sessao.json("$.refreshToken");

        Resp renovada = post("/auth/refresh", null, "{\"refreshToken\":\"%s\"}".formatted(refresh1));
        assertThat(renovada.status()).isEqualTo(200);
        String refresh2 = renovada.json("$.refreshToken");
        assertThat(refresh2).isNotEqualTo(refresh1);
        assertThat(get("/me", renovada.json("$.accessToken")).status()).isEqualTo(200);

        // reuso do token antigo: indício de roubo, encerra tudo
        assertThat(post("/auth/refresh", null, "{\"refreshToken\":\"%s\"}".formatted(refresh1)).status())
                .isEqualTo(401);
        assertThat(post("/auth/refresh", null, "{\"refreshToken\":\"%s\"}".formatted(refresh2)).status())
                .isEqualTo(401);
    }

    @Test
    void logoutRevogaORefreshToken() {
        Resp sessao = novaConfeiteira("dani");
        String access = sessao.json("$.accessToken");
        String refresh = sessao.json("$.refreshToken");

        assertThat(post("/auth/logout", access, "{\"refreshToken\":\"%s\"}".formatted(refresh)).status())
                .isEqualTo(204);
        assertThat(post("/auth/refresh", null, "{\"refreshToken\":\"%s\"}".formatted(refresh)).status())
                .isEqualTo(401);
    }

    @Test
    void adminDesativaUsuariaQueNaoConsegueMaisLogarNemRenovar() {
        String email = "eva-" + UUID.randomUUID() + "@teste.local";
        String id = post("/auth/register", null,
                "{\"nome\":\"Eva\",\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, SENHA)).id();
        String refresh = login(email, SENHA).json("$.refreshToken");
        String admin = login(ADMIN_EMAIL, ADMIN_SENHA).json("$.accessToken");

        Resp r = patch("/admin/usuarios/" + id, admin, "{\"ativo\":false}");
        assertThat(r.status()).isEqualTo(200);
        assertThat((Boolean) r.json("$.ativo")).isFalse();

        assertThat(login(email, SENHA).status()).isEqualTo(401);
        assertThat(post("/auth/refresh", null, "{\"refreshToken\":\"%s\"}".formatted(refresh)).status())
                .isEqualTo(401);
    }
}
