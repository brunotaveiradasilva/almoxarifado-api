package com.almoxarifado.api.auth;

import java.util.Comparator;
import java.util.List;

import com.almoxarifado.api.common.RecursoNaoEncontradoException;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Único endpoint aberto: todo o resto da API exige o token que sai daqui. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest corpo) {
        Usuario usuario = usuarios.findByUsuarioIgnoreCase(corpo.usuario())
                .orElseThrow(() -> new CredenciaisInvalidasException("Usuário ou senha inválidos"));

        if (!passwordEncoder.matches(corpo.senha(), usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException("Usuário ou senha inválidos");
        }

        return new LoginResponse(jwtService.gerar(usuario.getUsuario(), usuario.getRole()), usuario.getUsuario(), usuario.getRole());
    }

    /** Nomes de todos os logins (nunca as senhas/hashes). */
    @GetMapping("/usuarios")
    public List<String> listarUsuarios() {
        return usuarios.findAll().stream()
                .map(Usuario::getUsuario)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    /** Cria outro login. Exige estar autenticado — só quem já entra no sistema pode convidar mais gente. */
    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public void criarUsuario(@Valid @RequestBody CriarUsuarioRequest corpo) {
        usuarios.findByUsuarioIgnoreCase(corpo.usuario()).ifPresent(u -> {
            throw new UsuarioJaExisteException("Já existe um usuário com esse nome");
        });

        Usuario novo = new Usuario();
        novo.setUsuario(corpo.usuario());
        novo.setSenhaHash(passwordEncoder.encode(corpo.senha()));
        usuarios.save(novo);
    }

    /** Exclui um login. Nunca o último que resta — senão ninguém mais consegue entrar. */
    @DeleteMapping("/usuarios/{usuario}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirUsuario(@PathVariable String usuario) {
        Usuario alvo = usuarios.findByUsuarioIgnoreCase(usuario)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário " + usuario + " não encontrado"));

        if (usuarios.count() <= 1) {
            throw new UltimoUsuarioException("Não dá para excluir o único login que existe");
        }

        usuarios.delete(alvo);
    }

    /** Troca a própria senha. É assim que se recupera de uma senha gerada automaticamente. */
    @PatchMapping("/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trocarSenha(Authentication authentication, @Valid @RequestBody TrocarSenhaRequest corpo) {
        Usuario usuario = usuarios.findByUsuarioIgnoreCase(authentication.getName())
                .orElseThrow(() -> new CredenciaisInvalidasException("Usuário não encontrado"));

        if (!passwordEncoder.matches(corpo.senhaAtual(), usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException("Senha atual incorreta");
        }

        usuario.setSenhaHash(passwordEncoder.encode(corpo.novaSenha()));
        usuarios.save(usuario);
    }
}
