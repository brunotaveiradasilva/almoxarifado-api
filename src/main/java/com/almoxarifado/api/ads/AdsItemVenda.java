package com.almoxarifado.api.ads;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Um item dentro de um pedido faturado. */
@JsonIgnoreProperties(ignoreUnknown = true)
record AdsItemVenda(AdsProduto produto, AdsDivisao divisao, double quantidade, AdsValoresItem valores, AdsPeso peso) {

    /** O id vem como número na ADS (ex: 5085); descricao é o nome, ex: "WELLPET 400MG (20,1 A 40KG)". */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record AdsProduto(String id, String descricao) {
    }

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
