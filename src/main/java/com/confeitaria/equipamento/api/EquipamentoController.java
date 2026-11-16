package com.confeitaria.equipamento.api;

import com.confeitaria.equipamento.api.EquipamentoDtos.EquipamentoRequest;
import com.confeitaria.equipamento.api.EquipamentoDtos.EquipamentoResponse;
import com.confeitaria.equipamento.application.EquipamentoService;
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
@RequestMapping("/api/v1/equipamentos")
@Tag(name = "Equipamentos")
public class EquipamentoController {

    private final EquipamentoService service;

    public EquipamentoController(EquipamentoService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<EquipamentoResponse> listar(@AuthenticationPrincipal UsuarioAutenticado u,
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return PageResponse.of(service.listar(u.id(), pageable));
    }

    @GetMapping("/{id}")
    public EquipamentoResponse buscar(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable UUID id) {
        return service.buscar(u.id(), id);
    }

    @PostMapping
    public ResponseEntity<EquipamentoResponse> criar(@AuthenticationPrincipal UsuarioAutenticado u,
            @Valid @RequestBody EquipamentoRequest request) {
        EquipamentoResponse criado = service.criar(u.id(), request);
        return ResponseEntity.created(URI.create("/api/v1/equipamentos/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public EquipamentoResponse atualizar(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable UUID id,
            @Valid @RequestBody EquipamentoRequest request) {
        return service.atualizar(u.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable UUID id) {
        service.desativar(u.id(), id);
    }
}
