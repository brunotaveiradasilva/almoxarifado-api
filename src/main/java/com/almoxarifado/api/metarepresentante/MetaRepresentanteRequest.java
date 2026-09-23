package com.almoxarifado.api.metarepresentante;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * O valorRealizado não entra aqui de propósito: ele só chega pela sincronização com a ADS. Se o
 * corpo trouxer esse campo (versões antigas do front mandavam), ele é simplesmente ignorado.
 * {@code mes} vazio vale o mês atual.
 */
public record MetaRepresentanteRequest(
        @NotBlank(message = "Informe o representante") String representanteId,
        @NotBlank(message = "Informe a meta") String metaId,
        String mes,
        @NotNull(message = "Informe o valor da meta") @DecimalMin(value = "0", message = "A meta não pode ser negativa") Double valorMeta
) {
}
