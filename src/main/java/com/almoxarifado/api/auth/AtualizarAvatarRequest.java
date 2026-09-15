package com.almoxarifado.api.auth;

import jakarta.validation.constraints.Size;

public record AtualizarAvatarRequest(
        @Size(max = 2_000_000, message = "Imagem muito grande") String avatar
) {
}
