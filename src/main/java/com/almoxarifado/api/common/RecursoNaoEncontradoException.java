package com.almoxarifado.api.common;

/** Lançada quando um id pesquisado (material ou agendamento) não existe no banco. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
