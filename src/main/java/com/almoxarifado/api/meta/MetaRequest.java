package com.almoxarifado.api.meta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MetaRequest(
        @NotBlank(message = "Informe o nome da meta") String nome,
        @NotBlank(message = "Informe o fornecedor") String fornecedorId,
        @NotNull(message = "Informe a unidade de medida") UnidadeMeta unidade,
        String codigoAdsDivisao
) {
}
