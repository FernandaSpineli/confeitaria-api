package com.confeitaria.shared.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/** Respostas 401/403 geradas pelo filtro de segurança, no mesmo formato RFC 9457 do restante da API. */
final class ProblemDetailSecurityHandlers {

    private ProblemDetailSecurityHandlers() {
    }

    static AuthenticationEntryPoint naoAutenticado() {
        return (request, response, ex) -> escrever(response, 401, "nao-autenticado", "Não autenticado",
                "Envie um access token válido no header Authorization: Bearer <token>.");
    }

    static AccessDeniedHandler acessoNegado() {
        return (request, response, ex) -> escrever(response, 403, "acesso-negado", "Acesso negado",
                "Seu perfil não tem permissão para esta operação.");
    }

    private static void escrever(HttpServletResponse response, int status, String tipo, String titulo,
            String detalhe) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        if (status == 401) {
            response.setHeader("WWW-Authenticate", "Bearer");
        }
        response.getWriter().write("""
                {"type":"https://api.confeitaria.local/problemas/%s","title":"%s","status":%d,"detail":"%s"}"""
                .formatted(tipo, titulo, status, detalhe));
    }
}
