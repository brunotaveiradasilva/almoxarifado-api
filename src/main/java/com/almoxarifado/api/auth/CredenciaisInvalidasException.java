package com.almoxarifado.api.auth;

/** Lançada quando o usuário não existe ou a senha não confere no login. */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException(String mensagem) {
        super(mensagem);
    }
}
