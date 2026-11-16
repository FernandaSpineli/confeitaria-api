package com.confeitaria.shared.security;

import com.confeitaria.usuario.persistence.Perfil;
import java.util.UUID;

/** Principal extraído do access token. É a única fonte do "dono" dos dados. */
public record UsuarioAutenticado(UUID id, String email, Perfil perfil) {
}
