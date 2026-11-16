package com.confeitaria.insumo.api;

import com.confeitaria.insumo.api.InsumoDtos.InsumoRequest;
import com.confeitaria.insumo.api.InsumoDtos.InsumoResponse;
import com.confeitaria.insumo.application.InsumoService;
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
@RequestMapping("/api/v1/insumos")
@Tag(name = "Insumos")
public class InsumoController {

    private final InsumoService service;

    public InsumoController(InsumoService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<InsumoResponse> listar(@AuthenticationPrincipal UsuarioAutenticado u,
            @RequestParam(required = false) String nome, @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return PageResponse.of(service.listar(u.id(), nome, pageable));
    }

    @GetMapping("/{id}")
    public InsumoResponse buscar(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable UUID id) {
        return service.buscar(u.id(), id);
    }

    @PostMapping
    public ResponseEntity<InsumoResponse> criar(@AuthenticationPrincipal UsuarioAutenticado u,
            @Valid @RequestBody InsumoRequest request) {
        InsumoResponse criado = service.criar(u.id(), request);
        return ResponseEntity.created(URI.create("/api/v1/insumos/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public InsumoResponse atualizar(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable UUID id,
            @Valid @RequestBody InsumoRequest request) {
        return service.atualizar(u.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable UUID id) {
        service.desativar(u.id(), id);
    }
}
