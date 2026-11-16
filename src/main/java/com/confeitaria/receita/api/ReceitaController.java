package com.confeitaria.receita.api;

import com.confeitaria.receita.api.ReceitaDtos.ReceitaRequest;
import com.confeitaria.receita.api.ReceitaDtos.ReceitaResponse;
import com.confeitaria.receita.api.ReceitaDtos.ReceitaResumoResponse;
import com.confeitaria.receita.application.ReceitaService;
import com.confeitaria.shared.api.PageResponse;
import com.confeitaria.shared.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/receitas")
@Tag(name = "Receitas")
public class ReceitaController {

    private final ReceitaService service;

    public ReceitaController(ReceitaService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<ReceitaResumoResponse> listar(@AuthenticationPrincipal UsuarioAutenticado u,
            @RequestParam(required = false) String nome, @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return PageResponse.of(service.listar(u.id(), nome, pageable));
    }

    @GetMapping("/{id}")
    public ReceitaResponse buscar(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable UUID id) {
        return service.buscar(u.id(), id);
    }

    @PostMapping
    public ResponseEntity<ReceitaResponse> criar(@AuthenticationPrincipal UsuarioAutenticado u,
            @Valid @RequestBody ReceitaRequest request) {
        ReceitaResponse criada = service.criar(u.id(), request);
        return ResponseEntity.created(URI.create("/api/v1/receitas/" + criada.id())).body(criada);
    }

    @PutMapping("/{id}")
    public ReceitaResponse atualizar(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable UUID id,
            @Valid @RequestBody ReceitaRequest request) {
        return service.atualizar(u.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable UUID id) {
        service.desativar(u.id(), id);
    }
}
