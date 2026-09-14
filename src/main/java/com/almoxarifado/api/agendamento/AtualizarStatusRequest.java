package com.almoxarifado.api.agendamento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Corpo do PATCH que só muda o status de um agendamento (botões "retirar" / "devolver" da tela). */
public record AtualizarStatusRequest(
        @NotBlank(message = "Informe o status")
        @Pattern(regexp = "agendado|retirado|devolvido", message = "Status deve ser agendado, retirado ou devolvido")
        String status
) {
}
