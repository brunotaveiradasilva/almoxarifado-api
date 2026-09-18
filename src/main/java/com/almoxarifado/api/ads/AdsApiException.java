package com.almoxarifado.api.ads;

/** Erro falando com a API da ADS: login recusado, resposta de erro, ou falha de rede. */
public class AdsApiException extends RuntimeException {

    public AdsApiException(String message) {
        super(message);
    }

    public AdsApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
