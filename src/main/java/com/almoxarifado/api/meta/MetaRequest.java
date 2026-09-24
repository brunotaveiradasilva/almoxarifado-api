package com.almoxarifado.api.meta;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** {@code diaInicio} e {@code diaFim} vêm os dois ou nenhum (mês inteiro), com o início antes do fim. */
public record MetaRequest(
        @NotBlank(message = "Informe o nome da meta") String nome,
        @NotBlank(message = "Informe o fornecedor") String fornecedorId,
        @NotNull(message = "Informe a unidade de medida") UnidadeMeta unidade,
        String codigoAdsDivisao,
        String cnpjAdsFornecedor,
        String produtosExcluidos,
        String produtosIncluidos,
        @Size(max = 500, message = "A descrição pode ter até 500 caracteres") String descricao,
        @Min(value = 1, message = "O dia inicial vai de 1 a 31") @Max(value = 31, message = "O dia inicial vai de 1 a 31") Integer diaInicio,
        @Min(value = 1, message = "O dia final vai de 1 a 31") @Max(value = 31, message = "O dia final vai de 1 a 31") Integer diaFim
) {

    @AssertTrue(message = "Informe o dia inicial e o final do período, com o inicial antes do final")
    public boolean isPeriodoValido() {
        if (diaInicio == null && diaFim == null) return true;
        return diaInicio != null && diaFim != null && diaInicio <= diaFim;
    }
}
