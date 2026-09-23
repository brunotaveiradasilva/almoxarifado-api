package com.almoxarifado.api.meta;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

/** Ids das metas na ordem em que devem aparecer na tela. Metas que ficarem de fora vão pro fim. */
public record OrdenarMetasRequest(
        @NotEmpty(message = "Informe a ordem das metas") List<String> ids
) {
}
