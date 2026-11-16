package com.confeitaria.usuario.application;

import com.confeitaria.auth.persistence.RefreshTokenRepository;
import com.confeitaria.shared.error.RecursoNaoEncontradoException;
import com.confeitaria.shared.error.RegraNegocioException;
import com.confeitaria.usuario.api.UsuarioDtos.AtualizarUsuarioRequest;
import com.confeitaria.usuario.api.UsuarioDtos.UsuarioResponse;
import com.confeitaria.usuario.persistence.Usuario;
import com.confeitaria.usuario.persistence.UsuarioRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final RefreshTokenRepository refreshTokens;
    private final Clock clock;

    public UsuarioService(UsuarioRepository usuarios, RefreshTokenRepository refreshTokens, Clock clock) {
        this.usuarios = usuarios;
        this.refreshTokens = refreshTokens;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscar(UUID id) {
        return UsuarioResponse.of(obter(id));
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(String email, Pageable pageable) {
        String filtro = email == null ? "" : email.toLowerCase();
        return usuarios.findByEmailContaining(filtro, pageable).map(UsuarioResponse::of);
    }

    @Transactional
    public UsuarioResponse atualizar(UUID id, AtualizarUsuarioRequest request, UUID adminId) {
        Usuario usuario = obter(id);
        if (id.equals(adminId) && (Boolean.FALSE.equals(request.ativo()) || request.perfil() != null)) {
            throw new RegraNegocioException("Um administrador não pode desativar a si mesmo nem alterar o próprio perfil.");
        }
        if (request.perfil() != null) {
            usuario.setPerfil(request.perfil());
        }
        if (request.ativo() != null) {
            usuario.setAtivo(request.ativo());
            if (!request.ativo()) {
                refreshTokens.revogarTodosDoUsuario(id, clock.instant());
            }
        }
        return UsuarioResponse.of(usuario);
    }

    private Usuario obter(UUID id) {
        return usuarios.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Usuária"));
    }
}
