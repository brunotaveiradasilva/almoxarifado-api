package com.almoxarifado.api.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrocarSenhaRequest(
        @NotBlank(message = "Informe a senha atual") String senhaAtual,
        @NotBlank(message = "Informe a nova senha")
        @Size(min = 4, message = "A nova senha precisa ter pelo menos 4 caracteres") String novaSenha
) {
}
