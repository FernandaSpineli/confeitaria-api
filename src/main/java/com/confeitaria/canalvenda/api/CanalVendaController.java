package com.confeitaria.canalvenda.api;

import com.confeitaria.canalvenda.api.CanalVendaDtos.CanalVendaRequest;
import com.confeitaria.canalvenda.api.CanalVendaDtos.CanalVendaResponse;
import com.confeitaria.canalvenda.application.CanalVendaService;
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
@RequestMapping("/api/v1/canais-venda")
@Tag(name = "Canais de venda")
public class CanalVendaController {

    private final CanalVendaService service;

    public CanalVendaController(CanalVendaService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<CanalVendaResponse> listar(@AuthenticationPrincipal UsuarioAutenticado u,
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return PageResponse.of(service.listar(u.id(), pageable));
    }

    @GetMapping("/{id}")
    public CanalVendaResponse buscar(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable UUID id) {
        return service.buscar(u.id(), id);
    }

    @PostMapping
    public ResponseEntity<CanalVendaResponse> criar(@AuthenticationPrincipal UsuarioAutenticado u,
            @Valid @RequestBody CanalVendaRequest request) {
        CanalVendaResponse criado = service.criar(u.id(), request);
        return ResponseEntity.created(URI.create("/api/v1/canais-venda/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public CanalVendaResponse atualizar(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable UUID id,
            @Valid @RequestBody CanalVendaRequest request) {
        return service.atualizar(u.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable UUID id) {
        service.remover(u.id(), id);
    }
}
