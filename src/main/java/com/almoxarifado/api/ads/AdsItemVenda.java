package com.almoxarifado.api.ads;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Um item dentro de um pedido faturado. */
@JsonIgnoreProperties(ignoreUnknown = true)
record AdsItemVenda(AdsDivisao divisao, double quantidade, AdsValoresItem valores, AdsPeso peso) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AdsDivisao(String id, String descricao) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AdsValoresItem(double valorProduto) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AdsPeso(double bruto, double liquido) {
    }
}
