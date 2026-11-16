package com.confeitaria.usuario.api;

import com.confeitaria.shared.security.UsuarioAutenticado;
import com.confeitaria.usuario.api.UsuarioDtos.UsuarioResponse;
import com.confeitaria.usuario.application.UsuarioService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Usuária logada")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public UsuarioResponse me(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return service.buscar(usuario.id());
    }
}
