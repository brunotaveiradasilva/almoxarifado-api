package com.almoxarifado.api.auth;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Garante que sempre exista pelo menos um login: na primeira vez que a API sobe sem o usuário
 * ADMIN_USERNAME cadastrado, cria um a partir de ADMIN_USERNAME/ADMIN_PASSWORD. Se a senha não
 * foi configurada, gera uma aleatória e só mostra ela uma vez, no log de inicialização.
 *
 * <p>Se alguém ficar trancado pra fora (perdeu a senha e não tem mais ninguém pra trocar por
 * ela), definir RESET_ADMIN_PASSWORD=true força esse login a ser recriado com a senha atual de
 * ADMIN_PASSWORD no próximo boot — só quem já tem acesso às variáveis de ambiente do serviço
 * (Railway, etc.) consegue fazer isso, então não é uma porta aberta pra qualquer um.</p>
 */
@Component
public class BootstrapUsuarioAdmin implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapUsuarioAdmin.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final String usuarioAdmin;
    private final String senhaAdmin;
    private final boolean forcarReset;

    public BootstrapUsuarioAdmin(
            UsuarioRepository usuarios,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.usuario:admin}") String usuarioAdmin,
            @Value("${app.admin.senha:}") String senhaAdmin,
            @Value("${app.admin.forcar-reset:false}") boolean forcarReset) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.usuarioAdmin = usuarioAdmin;
        this.senhaAdmin = senhaAdmin;
        this.forcarReset = forcarReset;
    }

    @Override
    public void run(ApplicationArguments args) {
        Optional<Usuario> existente = usuarios.findByUsuarioIgnoreCase(usuarioAdmin);

        if (existente.isPresent() && !forcarReset) {
            // Login já existe e não é pra redefinir a senha: só garante que continua ADMIN
            // (cobre logins criados antes do campo role existir, sem tocar na senha deles).
            Usuario usuario = existente.get();
            if (usuario.getRole() != Role.ADMIN) {
                usuario.setRole(Role.ADMIN);
                usuarios.save(usuario);
                log.info("Promovi '{}' a ADMIN (login já existia de antes do controle de permissões).", usuarioAdmin);
            }
            return;
        }

        boolean senhaFoiGerada = senhaAdmin.isBlank();
        String senha = senhaFoiGerada ? gerarSenhaAleatoria() : senhaAdmin;

        Usuario admin = existente.orElseGet(Usuario::new);
        admin.setUsuario(usuarioAdmin);
        admin.setSenhaHash(passwordEncoder.encode(senha));
        admin.setRole(Role.ADMIN);
        usuarios.save(admin);

        String acao = existente.isPresent() ? "redefini a senha do login" : "criei o login";
        if (senhaFoiGerada) {
            log.warn("{} '{}' com a senha gerada '{}' -- anote agora, ela nao aparece de novo. "
                    + "Defina ADMIN_PASSWORD para escolher a sua.", acao, usuarioAdmin, senha);
        } else {
            log.info("{} '{}' a partir de ADMIN_USERNAME/ADMIN_PASSWORD.", acao, usuarioAdmin);
        }

        if (existente.isPresent()) {
            log.warn("RESET_ADMIN_PASSWORD estava ativo -- desative essa variavel agora, "
                    + "senão a senha volta a ser redefinida a cada novo deploy.");
        }
    }

    private static String gerarSenhaAleatoria() {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
