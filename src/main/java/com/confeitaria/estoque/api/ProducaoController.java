package com.confeitaria.estoque.api;

import com.confeitaria.estoque.api.EstoqueDtos.CapacidadeResponse;
import com.confeitaria.estoque.api.EstoqueDtos.ProducaoRequest;
import com.confeitaria.estoque.api.EstoqueDtos.ProducaoResponse;
import com.confeitaria.estoque.application.ProducaoService;
import com.confeitaria.shared.api.PageResponse;
import com.confeitaria.shared.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Produção")
public class ProducaoController {

    private final ProducaoService service;

    public ProducaoController(ProducaoService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/producoes")
    @Operation(summary = "Registra a produção de N lotes e dá baixa nos insumos e embalagens")
    public ResponseEntity<ProducaoResponse> produzir(@AuthenticationPrincipal UsuarioAutenticado u,
            @Valid @RequestBody ProducaoRequest request) {
        ProducaoResponse p = service.produzir(u.id(), request);
        return ResponseEntity.created(URI.create("/api/v1/producoes/" + p.id())).body(p);
    }

    @GetMapping("/api/v1/producoes")
    public PageResponse<ProducaoResponse> listar(@AuthenticationPrincipal UsuarioAutenticado u,
            @PageableDefault(size = 20, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.of(service.listar(u.id(), pageable));
    }

    @GetMapping("/api/v1/producoes/{id}")
    public ProducaoResponse buscar(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable UUID id) {
        return service.buscar(u.id(), id);
    }

    @GetMapping("/api/v1/receitas/{receitaId}/capacidade-producao")
    @Operation(summary = "Quantos lotes desta receita consigo produzir com o estoque atual?")
    public CapacidadeResponse capacidade(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable UUID receitaId) {
        return service.capacidade(u.id(), receitaId);
    }
}
