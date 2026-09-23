package com.almoxarifado.api.metarepresentante;

import jakarta.validation.constraints.NotBlank;

/** Copia os valores de meta do mês {@code de} pro mês {@code para}; {@code fornecedorId} opcional limita a um fornecedor. */
public record CopiarMetasRequest(
        @NotBlank(message = "Informe o mês de origem") String de,
        @NotBlank(message = "Informe o mês de destino") String para,
        String fornecedorId
) {
}
