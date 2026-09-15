package com.almoxarifado.api.common;

/** Lançada ao tentar excluir um registro que ainda é referenciado por outro (ex.: fornecedor com metas). */
public class RecursoEmUsoException extends RuntimeException {

    public RecursoEmUsoException(String mensagem) {
        super(mensagem);
    }
}
