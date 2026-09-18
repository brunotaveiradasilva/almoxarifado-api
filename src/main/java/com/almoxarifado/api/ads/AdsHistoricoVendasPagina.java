package com.almoxarifado.api.ads;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Uma página de resultados do GET /historico-de-vendas da ADS. */
@JsonIgnoreProperties(ignoreUnknown = true)
record AdsHistoricoVendasPagina(List<AdsVenda> items, int page, int pageSize, int total, boolean hasNext) {
}
