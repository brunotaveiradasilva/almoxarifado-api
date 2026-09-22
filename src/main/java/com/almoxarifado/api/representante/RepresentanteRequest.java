package com.almoxarifado.api.representante;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record RepresentanteRequest(
        @NotBlank(message = "Informe o nome do representante") String nome,
        @NotEmpty(message = "Selecione ao menos um fornecedor") List<String> fornecedorIds,
        String email,
        String celular,
        String codigoAds
) {
}
