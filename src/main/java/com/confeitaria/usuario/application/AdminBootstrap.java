package com.confeitaria.usuario.application;

import com.confeitaria.usuario.persistence.Perfil;
import com.confeitaria.usuario.persistence.Usuario;
import com.confeitaria.usuario.persistence.UsuarioRepository;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Cria o primeiro ADMIN a partir de APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD, se ainda não existir. */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String email;
    private final String senha;

    public AdminBootstrap(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, Clock clock,
            @Value("${app.admin.email:}") String email, @Value("${app.admin.password:}") String senha) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.email = email;
        this.senha = senha;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || senha.isBlank() || usuarios.existsByEmail(email.toLowerCase())) {
            return;
        }
        usuarios.save(new Usuario("Administradora", email, passwordEncoder.encode(senha), Perfil.ADMIN,
                clock.instant()));
        log.info("Usuária ADMIN inicial criada: {}", email);
    }
}
