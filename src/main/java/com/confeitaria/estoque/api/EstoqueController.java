package com.confeitaria.estoque.api;

import com.confeitaria.estoque.api.EstoqueDtos.AjusteRequest;
import com.confeitaria.estoque.api.EstoqueDtos.CompraRequest;
import com.confeitaria.estoque.api.EstoqueDtos.CompraResponse;
import com.confeitaria.estoque.api.EstoqueDtos.ItemAbaixoMinimoResponse;
import com.confeitaria.estoque.api.EstoqueDtos.MovimentacaoResponse;
import com.confeitaria.estoque.application.EstoqueService;
import com.confeitaria.estoque.persistence.TipoMovimentacao;
import com.confeitaria.shared.api.PageResponse;
import com.confeitaria.shared.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/estoque")
@Tag(name = "Estoque")
public class EstoqueController {

    private final EstoqueService service;

    public EstoqueController(EstoqueService service) {
        this.service = service;
    }

    @PostMapping("/compras")
    @ResponseStatus(HttpStatus.CREATED)
    public CompraResponse compra(@AuthenticationPrincipal UsuarioAutenticado u, @Valid @RequestBody CompraRequest r) {
        return service.registrarCompra(u.id(), r);
    }

    @PostMapping("/ajustes")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimentacaoResponse ajuste(@AuthenticationPrincipal UsuarioAutenticado u,
            @Valid @RequestBody AjusteRequest r) {
        return service.registrarAjuste(u.id(), r);
    }

    @GetMapping("/movimentacoes")
    public PageResponse<MovimentacaoResponse> movimentacoes(@AuthenticationPrincipal UsuarioAutenticado u,
            @RequestParam(required = false) TipoMovimentacao tipo,
            @RequestParam(required = false) UUID insumoId,
            @RequestParam(required = false) UUID embalagemId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @PageableDefault(size = 20, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.of(service.listarMovimentacoes(u.id(), tipo, insumoId, embalagemId, de, ate, pageable));
    }

    @GetMapping("/abaixo-minimo")
    public List<ItemAbaixoMinimoResponse> abaixoDoMinimo(@AuthenticationPrincipal UsuarioAutenticado u) {
        return service.abaixoDoMinimo(u.id());
    }
}
