package com.confeitaria.auth.api;

import com.confeitaria.auth.api.AuthDtos.LoginRequest;
import com.confeitaria.auth.api.AuthDtos.RefreshRequest;
import com.confeitaria.auth.api.AuthDtos.RegisterRequest;
import com.confeitaria.auth.api.AuthDtos.TokenResponse;
import com.confeitaria.auth.application.AuthService;
import com.confeitaria.shared.security.UsuarioAutenticado;
import com.confeitaria.usuario.api.UsuarioDtos.UsuarioResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @SecurityRequirements
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegisterRequest request) {
        UsuarioResponse criada = service.registrar(request);
        return ResponseEntity.created(URI.create("/api/v1/me")).body(criada);
    }

    @PostMapping("/login")
    @SecurityRequirements
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return service.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        service.logout(request.refreshToken(), usuario.id());
    }
}
