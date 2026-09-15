package com.almoxarifado.api.auth;

import java.security.SecureRandom;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Garante que sempre exista pelo menos um login: na primeira vez que a API sobe (nenhum
 * usuário cadastrado ainda), cria um a partir de ADMIN_USERNAME/ADMIN_PASSWORD. Se a senha
 * não foi configurada, gera uma aleatória e só mostra ela uma vez, no log de inicialização.
 */
@Component
public class BootstrapUsuarioAdmin implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapUsuarioAdmin.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final String usuarioAdmin;
    private final String senhaAdmin;

    public BootstrapUsuarioAdmin(
            UsuarioRepository usuarios,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.usuario:admin}") String usuarioAdmin,
            @Value("${app.admin.senha:}") String senhaAdmin) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.usuarioAdmin = usuarioAdmin;
        this.senhaAdmin = senhaAdmin;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarios.count() > 0) return;

        boolean senhaFoiGerada = senhaAdmin.isBlank();
        String senha = senhaFoiGerada ? gerarSenhaAleatoria() : senhaAdmin;

        Usuario admin = new Usuario();
        admin.setUsuario(usuarioAdmin);
        admin.setSenhaHash(passwordEncoder.encode(senha));
        usuarios.save(admin);

        if (senhaFoiGerada) {
            log.warn("Nenhum usuario existia ainda: criei o login '{}' com a senha gerada '{}' -- "
                    + "anote agora, ela nao aparece de novo. Defina ADMIN_PASSWORD para escolher a sua.",
                    usuarioAdmin, senha);
        } else {
            log.info("Login inicial '{}' criado a partir de ADMIN_USERNAME/ADMIN_PASSWORD.", usuarioAdmin);
        }
    }

    private static String gerarSenhaAleatoria() {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
