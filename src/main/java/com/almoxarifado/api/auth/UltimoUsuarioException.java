package com.almoxarifado.api.auth;

/** Lançada ao tentar excluir o único login que resta — ninguém mais conseguiria entrar depois. */
public class UltimoUsuarioException extends RuntimeException {

    public UltimoUsuarioException(String mensagem) {
        super(mensagem);
    }
}
