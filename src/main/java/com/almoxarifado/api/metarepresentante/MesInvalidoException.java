package com.almoxarifado.api.metarepresentante;

/** Mês fora do formato "2026-09" — vira 400 (ver TratadorDeErros). */
public class MesInvalidoException extends RuntimeException {

    public MesInvalidoException(String mensagem) {
        super(mensagem);
    }
}
