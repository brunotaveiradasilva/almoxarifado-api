package com.almoxarifado.api.especialistapet;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/** As linhas da aba CNPJ da planilha da PremieR, já lidas pelo front-end. */
public record ImportarEspecialistaPetRequest(
        @NotBlank(message = "Informe o mês") String mes,
        @NotEmpty(message = "A planilha não tem nenhum cliente") List<@Valid Linha> clientes) {

    public record Linha(
            @NotBlank(message = "Cliente sem código na planilha") String codigoCliente,
            @NotBlank(message = "Cliente sem nome na planilha") String nome,
            @NotBlank(message = "Cliente sem vendedor na planilha") String representante,
            String classificacao,
            @DecimalMin(value = "0", message = "A meta não pode ser negativa") double metaFoco,
            @DecimalMin(value = "0", message = "A meta não pode ser negativa") double metaTotal) {
    }
}
