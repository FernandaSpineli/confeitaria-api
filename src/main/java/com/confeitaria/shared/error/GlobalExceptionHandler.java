package com.confeitaria.shared.error;

import com.confeitaria.estoque.domain.EstoqueException;
import com.confeitaria.estoque.domain.EstoqueInsuficienteException;
import com.confeitaria.precificacao.domain.MargemInviavelException;
import com.confeitaria.precificacao.domain.PrecificacaoException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Converte exceções em respostas RFC 9457 (application/problem+json). */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String TIPO_BASE = "https://api.confeitaria.local/problemas/";

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public static ProblemDetail problema(HttpStatus status, String tipo, String titulo, String detalhe) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detalhe);
        problem.setType(URI.create(TIPO_BASE + tipo));
        problem.setTitle(titulo);
        return problem;
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ProblemDetail naoEncontrado(RecursoNaoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "nao-encontrado", "Recurso não encontrado", ex.getMessage());
    }

    @ExceptionHandler(RegraNegocioException.class)
    ProblemDetail regraNegocio(RegraNegocioException ex) {
        ProblemDetail problem = problema(HttpStatus.UNPROCESSABLE_CONTENT, ex.tipo(), ex.titulo(), ex.getMessage());
        ex.extras().forEach(problem::setProperty);
        return problem;
    }

    @ExceptionHandler(MargemInviavelException.class)
    ProblemDetail margemInviavel(MargemInviavelException ex) {
        ProblemDetail problem = problema(HttpStatus.UNPROCESSABLE_CONTENT, "margem-inviavel",
                "Margem inviável", ex.getMessage());
        problem.setProperty("canaisInvalidos", ex.canaisInvalidos());
        return problem;
    }

    @ExceptionHandler(PrecificacaoException.class)
    ProblemDetail precificacao(PrecificacaoException ex) {
        return problema(HttpStatus.UNPROCESSABLE_CONTENT, "precificacao",
                "Não foi possível calcular o preço", ex.getMessage());
    }

    @ExceptionHandler(EstoqueInsuficienteException.class)
    ProblemDetail estoqueInsuficiente(EstoqueInsuficienteException ex) {
        ProblemDetail problem = problema(HttpStatus.UNPROCESSABLE_CONTENT, "estoque-insuficiente",
                "Estoque insuficiente", ex.getMessage());
        problem.setProperty("itensFaltantes", ex.itensFaltantes());
        return problem;
    }

    @ExceptionHandler(EstoqueException.class)
    ProblemDetail estoque(EstoqueException ex) {
        return problema(HttpStatus.UNPROCESSABLE_CONTENT, "estoque", "Operação de estoque inválida", ex.getMessage());
    }

    @ExceptionHandler(ConflitoException.class)
    ProblemDetail conflito(ConflitoException ex) {
        return problema(HttpStatus.CONFLICT, "conflito", "Conflito", ex.getMessage());
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    ProblemDetail credenciais(CredenciaisInvalidasException ex) {
        return problema(HttpStatus.UNAUTHORIZED, "nao-autenticado", "Não autenticado", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail acessoNegado(AccessDeniedException ex) {
        return problema(HttpStatus.FORBIDDEN, "acesso-negado", "Acesso negado",
                "Seu perfil não tem permissão para esta operação.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail inesperado(Exception ex) {
        log.error("Erro inesperado", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "erro-interno", "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente mais tarde.");
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> erros = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> erros.putIfAbsent(e.getField(), e.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(e -> erros.putIfAbsent(e.getObjectName(), e.getDefaultMessage()));
        ProblemDetail problem = problema(HttpStatus.BAD_REQUEST, "validacao", "Dados inválidos",
                "Um ou mais campos são inválidos.");
        problem.setProperty("erros", erros);
        return ResponseEntity.badRequest().body(problem);
    }
}
