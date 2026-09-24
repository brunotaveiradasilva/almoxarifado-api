package com.almoxarifado.api.dados;

/** Filtro da aba Dados que não dá pra atender (período invertido, fornecedor sem código ADS) — vira 400. */
public class ConsultaInvalidaException extends RuntimeException {

    public ConsultaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
