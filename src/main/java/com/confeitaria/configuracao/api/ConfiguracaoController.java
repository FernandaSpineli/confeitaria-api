package com.confeitaria.configuracao.api;

import com.confeitaria.configuracao.api.ConfiguracaoDtos.AdicionalBandeiraRequest;
import com.confeitaria.configuracao.api.ConfiguracaoDtos.AdicionalBandeiraResponse;
import com.confeitaria.configuracao.api.ConfiguracaoDtos.ConfiguracaoRequest;
import com.confeitaria.configuracao.api.ConfiguracaoDtos.ConfiguracaoResponse;
import com.confeitaria.configuracao.api.ConfiguracaoDtos.TarifaRequest;
import com.confeitaria.configuracao.api.ConfiguracaoDtos.TarifaResponse;
import com.confeitaria.configuracao.application.ConfiguracaoService;
import com.confeitaria.configuracao.persistence.Bandeira;
import com.confeitaria.shared.api.PageResponse;
import com.confeitaria.shared.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/v1/configuracao")
@Tag(name = "Configuração de custos")
public class ConfiguracaoController {

    private final ConfiguracaoService service;

    public ConfiguracaoController(ConfiguracaoService service) {
        this.service = service;
    }

    @GetMapping
    public ConfiguracaoResponse obter(@AuthenticationPrincipal UsuarioAutenticado u) {
        return service.obter(u.id());
    }

    @PutMapping
    public ConfiguracaoResponse atualizar(@AuthenticationPrincipal UsuarioAutenticado u,
            @Valid @RequestBody ConfiguracaoRequest request) {
        return service.atualizar(u.id(), request);
    }

    @GetMapping("/tarifas")
    public PageResponse<TarifaResponse> listarTarifas(@AuthenticationPrincipal UsuarioAutenticado u,
            @PageableDefault(size = 20, sort = "vigenciaInicio", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.of(service.listarTarifas(u.id(), pageable));
    }

    @GetMapping("/tarifas/vigente")
    public TarifaResponse tarifaVigente(@AuthenticationPrincipal UsuarioAutenticado u) {
        return service.tarifaVigente(u.id());
    }

    @PostMapping("/tarifas")
    @ResponseStatus(HttpStatus.CREATED)
    public TarifaResponse registrarTarifa(@AuthenticationPrincipal UsuarioAutenticado u,
            @Valid @RequestBody TarifaRequest request) {
        return service.registrarTarifa(u.id(), request);
    }

    @DeleteMapping("/tarifas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerTarifa(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable UUID id) {
        service.removerTarifa(u.id(), id);
    }

    @GetMapping("/bandeiras")
    public PageResponse<AdicionalBandeiraResponse> listarAdicionais(@AuthenticationPrincipal UsuarioAutenticado u,
            @RequestParam(required = false) Bandeira bandeira,
            @PageableDefault(size = 20, sort = "vigenciaInicio", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.of(service.listarAdicionais(u.id(), bandeira, pageable));
    }

    @GetMapping("/bandeiras/vigentes")
    public List<AdicionalBandeiraResponse> adicionaisVigentes(@AuthenticationPrincipal UsuarioAutenticado u) {
        return service.adicionaisVigentes(u.id());
    }

    @PostMapping("/bandeiras")
    @ResponseStatus(HttpStatus.CREATED)
    public AdicionalBandeiraResponse registrarAdicional(@AuthenticationPrincipal UsuarioAutenticado u,
            @Valid @RequestBody AdicionalBandeiraRequest request) {
        return service.registrarAdicional(u.id(), request);
    }

    @DeleteMapping("/bandeiras/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerAdicional(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable UUID id) {
        service.removerAdicional(u.id(), id);
    }
}
