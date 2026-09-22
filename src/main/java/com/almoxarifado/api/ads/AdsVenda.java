package com.almoxarifado.api.ads;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Um pedido faturado, como a ADS devolve em /historico-de-vendas. Só os campos que o cálculo de
 * realizado usa hoje — o resto (cliente, nota fiscal, tributos) a ADS manda mas ainda não é lido
 * aqui.
 *
 * dataFaturamento vem sem fuso (ex: "2026-09-01T00:00:00", sem Z nem offset) — por isso é
 * LocalDateTime, não OffsetDateTime; esse último quebra a desserialização de toda a resposta.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record AdsVenda(
        LocalDateTime dataFaturamento,
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
