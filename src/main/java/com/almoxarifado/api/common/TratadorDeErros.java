package com.almoxarifado.api.common;

import java.util.LinkedHashMap;
import java.util.Map;

import com.almoxarifado.api.auth.CredenciaisInvalidasException;
import com.almoxarifado.api.auth.UltimoUsuarioException;
import com.almoxarifado.api.auth.UsuarioJaExisteException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Converte as exceções da API em respostas JSON simples, em vez da página de erro padrão do Spring. */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, String>> naoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<Map<String, String>> credenciaisInvalidas(CredenciaisInvalidasException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(UsuarioJaExisteException.class)
    public ResponseEntity<Map<String, String>> usuarioJaExiste(UsuarioJaExisteException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(UltimoUsuarioException.class)
    public ResponseEntity<Map<String, String>> ultimoUsuario(UltimoUsuarioException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(RecursoJaExisteException.class)
    public ResponseEntity<Map<String, String>> recursoJaExiste(RecursoJaExisteException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(RecursoEmUsoException.class)
    public ResponseEntity<Map<String, String>> recursoEmUso(RecursoEmUsoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacaoFalhou(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(erro ->
                campos.put(erro.getField(), erro.getDefaultMessage()));

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("erro", "Dados inválidos");
        corpo.put("campos", campos);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(corpo);
    }
}
