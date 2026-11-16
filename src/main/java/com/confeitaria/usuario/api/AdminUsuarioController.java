package com.confeitaria.usuario.api;

import com.confeitaria.shared.api.PageResponse;
import com.confeitaria.shared.security.UsuarioAutenticado;
import com.confeitaria.usuario.api.UsuarioDtos.AtualizarUsuarioRequest;
import com.confeitaria.usuario.api.UsuarioDtos.UsuarioResponse;
import com.confeitaria.usuario.application.UsuarioService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/usuarios")
@Tag(name = "Admin - usuárias")
public class AdminUsuarioController {

    private final UsuarioService service;

    public AdminUsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<UsuarioResponse> listar(@RequestParam(required = false) String email,
            @PageableDefault(size = 20, sort = "email") Pageable pageable) {
        return PageResponse.of(service.listar(email, pageable));
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @PatchMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable UUID id, @RequestBody AtualizarUsuarioRequest request,
            @AuthenticationPrincipal UsuarioAutenticado admin) {
        return service.atualizar(id, request, admin.id());
    }
}
