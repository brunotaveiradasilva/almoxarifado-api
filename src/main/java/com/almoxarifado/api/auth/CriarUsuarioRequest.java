package com.almoxarifado.api.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarUsuarioRequest(
        @NotBlank(message = "Informe o usuário") String usuario,
        @NotBlank(message = "Informe a senha")
        @Size(min = 4, message = "A senha precisa ter pelo menos 4 caracteres") String senha
) {
}
