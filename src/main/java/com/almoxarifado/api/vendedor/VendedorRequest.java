package com.almoxarifado.api.vendedor;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record VendedorRequest(
        @NotBlank(message = "Informe o nome do vendedor") String nome,
        @NotEmpty(message = "Selecione ao menos um fornecedor") List<String> fornecedorIds,
        String email,
        String celular
) {
}
