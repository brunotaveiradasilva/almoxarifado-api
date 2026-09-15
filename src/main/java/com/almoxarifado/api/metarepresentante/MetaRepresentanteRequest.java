package com.almoxarifado.api.metarepresentante;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MetaRepresentanteRequest(
        @NotBlank(message = "Informe o representante") String representanteId,
        @NotBlank(message = "Informe a meta") String metaId,
        @NotNull(message = "Informe o valor da meta") @DecimalMin(value = "0", message = "A meta não pode ser negativa") Double valorMeta,
        @NotNull(message = "Informe o valor realizado") @DecimalMin(value = "0", message = "O realizado não pode ser negativo") Double valorRealizado
) {
}
