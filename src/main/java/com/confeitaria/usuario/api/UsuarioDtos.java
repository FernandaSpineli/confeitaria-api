package com.confeitaria.usuario.api;

import com.confeitaria.usuario.persistence.Perfil;
import com.confeitaria.usuario.persistence.Usuario;
import java.time.Instant;
import java.util.UUID;

public final class UsuarioDtos {

    private UsuarioDtos() {
    }

    public record UsuarioResponse(UUID id, String nome, String email, Perfil perfil, boolean ativo, Instant criadoEm) {

        public static UsuarioResponse of(Usuario u) {
            return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getPerfil(), u.isAtivo(),
                    u.getCriadoEm());
        }
    }

    /** Campos nulos não são alterados. */
    public record AtualizarUsuarioRequest(Boolean ativo, Perfil perfil) {
    }
}
