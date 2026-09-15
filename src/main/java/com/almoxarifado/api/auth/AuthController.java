package com.almoxarifado.api.auth;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
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

        return new LoginResponse(jwtService.gerar(usuario.getUsuario()), usuario.getUsuario());
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
}
