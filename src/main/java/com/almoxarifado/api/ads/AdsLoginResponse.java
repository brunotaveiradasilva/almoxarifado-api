package com.almoxarifado.api.ads;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Resposta do POST /api/v1/login da ADS. `expiry` vem no formato que a própria ADS definir (não documentado — por isso o token é renovado de forma conservadora, ver {@link AdsTokenService}). */
@JsonIgnoreProperties(ignoreUnknown = true)
record AdsLoginResponse(String accessToken, Long expiry) {
}
