package com.confeitaria.precificacao.api;

import com.confeitaria.precificacao.api.PrecificacaoDtos.PrecoResponse;
import com.confeitaria.precificacao.api.PrecificacaoDtos.SimulacaoRequest;
import com.confeitaria.precificacao.application.PrecificacaoService;
import com.confeitaria.shared.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/receitas/{receitaId}")
@Tag(name = "Precificação")
public class PrecificacaoController {

    private final PrecificacaoService service;

    public PrecificacaoController(PrecificacaoService service) {
        this.service = service;
    }

    @GetMapping("/preco")
    @Operation(summary = "Preço sugerido com detalhamento de custos e preço por canal de venda")
    public PrecoResponse preco(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable UUID receitaId) {
        return service.calcular(u.id(), receitaId);
    }

    @PostMapping("/simulacao")
    @Operation(summary = "Simula o preço com outra margem, bandeira, tarifa ou canal, sem salvar nada")
    public PrecoResponse simular(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable UUID receitaId,
            @Valid @RequestBody SimulacaoRequest request) {
        return service.simular(u.id(), receitaId, request);
    }
}
