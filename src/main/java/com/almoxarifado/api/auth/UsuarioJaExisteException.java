package com.almoxarifado.api.auth;

/** Lançada ao tentar criar um login com um nome de usuário que já existe. */
public class UsuarioJaExisteException extends RuntimeException {

    public UsuarioJaExisteException(String mensagem) {
        super(mensagem);
    }
}
