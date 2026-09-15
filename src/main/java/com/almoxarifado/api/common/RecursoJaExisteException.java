package com.almoxarifado.api.common;

/** Lançada ao tentar criar/atualizar um registro com um valor único (código, nome) já usado. */
public class RecursoJaExisteException extends RuntimeException {

    public RecursoJaExisteException(String mensagem) {
        super(mensagem);
    }
}
