package com.confeitaria.custofixo.api;

import com.confeitaria.custofixo.api.CustoFixoDtos.CustoFixoRequest;
import com.confeitaria.custofixo.api.CustoFixoDtos.CustoFixoResponse;
import com.confeitaria.custofixo.application.CustoFixoService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/custos-fixos")
@Tag(name = "Custos fixos")
public class CustoFixoController {

    private final CustoFixoService service;

    public CustoFixoController(CustoFixoService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<CustoFixoResponse> listar(@AuthenticationPrincipal UsuarioAutenticado u,
            @PageableDefault(size = 20, sort = "descricao") Pageable pageable) {
        return PageResponse.of(service.listar(u.id(), pageable));
    }

    @GetMapping("/{id}")
    public CustoFixoResponse buscar(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable UUID id) {
        return service.buscar(u.id(), id);
    }

    @PostMapping
    public ResponseEntity<CustoFixoResponse> criar(@AuthenticationPrincipal UsuarioAutenticado u,
            @Valid @RequestBody CustoFixoRequest request) {
        CustoFixoResponse criado = service.criar(u.id(), request);
        return ResponseEntity.created(URI.create("/api/v1/custos-fixos/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public CustoFixoResponse atualizar(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable UUID id,
            @Valid @RequestBody CustoFixoRequest request) {
        return service.atualizar(u.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable UUID id) {
        service.remover(u.id(), id);
    }
}
