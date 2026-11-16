package com.confeitaria.auth.application;

import com.confeitaria.auth.api.AuthDtos.LoginRequest;
import com.confeitaria.auth.api.AuthDtos.RegisterRequest;
import com.confeitaria.auth.api.AuthDtos.TokenResponse;
import com.confeitaria.auth.persistence.RefreshToken;
import com.confeitaria.auth.persistence.RefreshTokenRepository;
import com.confeitaria.configuracao.application.ConfiguracaoService;
import com.confeitaria.shared.error.ConflitoException;
import com.confeitaria.shared.error.CredenciaisInvalidasException;
import com.confeitaria.shared.security.JwtProperties;
import com.confeitaria.shared.security.TokenService;
import com.confeitaria.usuario.api.UsuarioDtos.UsuarioResponse;
import com.confeitaria.usuario.persistence.Perfil;
import com.confeitaria.usuario.persistence.Usuario;
import com.confeitaria.usuario.persistence.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastro, login e ciclo de vida dos refresh tokens.
 *
 * <p>Refresh com rotação: cada uso revoga o token apresentado e emite outro. Se um token já
 * revogado for reapresentado, assume-se vazamento e todas as sessões da usuária são revogadas.
 */
@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String INVALIDO = "Refresh token inválido ou expirado.";

    private final UsuarioRepository usuarios;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final JwtProperties jwtProperties;
    private final ConfiguracaoService configuracaoService;
    private final Clock clock;

    public AuthService(UsuarioRepository usuarios, RefreshTokenRepository refreshTokens,
            PasswordEncoder passwordEncoder, TokenService tokenService, JwtProperties jwtProperties,
            ConfiguracaoService configuracaoService, Clock clock) {
        this.usuarios = usuarios;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.jwtProperties = jwtProperties;
        this.configuracaoService = configuracaoService;
        this.clock = clock;
    }

    @Transactional
    public UsuarioResponse registrar(RegisterRequest request) {
        String email = request.email().toLowerCase();
        if (usuarios.existsByEmail(email)) {
            throw new ConflitoException("Já existe uma conta com este e-mail.");
        }
        Usuario usuario = usuarios.save(new Usuario(request.nome(), email,
                passwordEncoder.encode(request.senha()), Perfil.CONFEITEIRA, clock.instant()));
        configuracaoService.criarPadrao(usuario.getId());
        return UsuarioResponse.of(usuario);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Usuario usuario = usuarios.findByEmail(request.email().toLowerCase())
                .filter(u -> passwordEncoder.matches(request.senha(), u.getSenhaHash()))
                .orElseThrow(() -> new CredenciaisInvalidasException("E-mail ou senha inválidos."));
        if (!usuario.isAtivo()) {
            throw new CredenciaisInvalidasException("Conta desativada.");
        }
        return emitirTokens(usuario);
    }

    /** noRollbackFor: a revogação em massa por reuso precisa ser gravada mesmo com o 401. */
    @Transactional(noRollbackFor = CredenciaisInvalidasException.class)
    public TokenResponse refresh(String tokenApresentado) {
        Instant agora = clock.instant();
        RefreshToken atual = refreshTokens.findByTokenHash(hash(tokenApresentado))
                .orElseThrow(() -> new CredenciaisInvalidasException(INVALIDO));
        if (atual.revogado()) {
            refreshTokens.revogarTodosDoUsuario(atual.getUsuarioId(), agora);
            throw new CredenciaisInvalidasException("Refresh token reutilizado. Todas as sessões foram encerradas.");
        }
        if (atual.expirado(agora)) {
            throw new CredenciaisInvalidasException(INVALIDO);
        }
        Usuario usuario = usuarios.findById(atual.getUsuarioId())
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new CredenciaisInvalidasException(INVALIDO));
        atual.revogar(agora);
        return emitirTokens(usuario);
    }

    @Transactional
    public void logout(String tokenApresentado, UUID usuarioId) {
        refreshTokens.findByTokenHash(hash(tokenApresentado))
                .filter(t -> t.getUsuarioId().equals(usuarioId))
                .ifPresent(t -> t.revogar(clock.instant()));
    }

    private TokenResponse emitirTokens(Usuario usuario) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant agora = clock.instant();
        refreshTokens.save(new RefreshToken(usuario.getId(), hash(refresh), agora,
                agora.plus(jwtProperties.refreshTokenTtl())));
        return new TokenResponse(tokenService.gerarAccessToken(usuario), "Bearer",
                tokenService.accessTokenTtlSegundos(), refresh);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
