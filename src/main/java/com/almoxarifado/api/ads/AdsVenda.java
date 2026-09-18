package com.almoxarifado.api.ads;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Um pedido faturado, como a ADS devolve em /historico-de-vendas. Só os campos que o cálculo de realizado usa hoje — o resto (cliente, nota fiscal, tributos, peso) a ADS manda mas ainda não é lido aqui. */
@JsonIgnoreProperties(ignoreUnknown = true)
record AdsVenda(
        OffsetDateTime dataFaturamento,
        long pedidoId,
        AdsFornecedor fornecedor,
        AdsRepresentante representante,
        List<AdsItemVenda> itens) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AdsFornecedor(String cnpj) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AdsRepresentante(String id, String cnpjCpf, String nome) {
    }
}
