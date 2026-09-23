package com.almoxarifado.api.metarepresentante;

/** Tentativa de mexer na meta de um mês que já acabou — vira 409 (ver TratadorDeErros). */
public class MesFechadoException extends RuntimeException {

    public MesFechadoException(String mensagem) {
        super(mensagem);
    }
}
