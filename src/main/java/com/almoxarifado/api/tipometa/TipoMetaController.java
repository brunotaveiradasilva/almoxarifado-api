package com.almoxarifado.api.tipometa;

import java.util.List;

import com.almoxarifado.api.common.RecursoJaExisteException;
import com.almoxarifado.api.common.RecursoNaoEncontradoException;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CRUD de tipos de meta. Só admins acessam (ver SecurityConfig). */
@RestController
@RequestMapping("/api/tipos-meta")
public class TipoMetaController {

    private final TipoMetaRepository repository;

    public TipoMetaController(TipoMetaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<TipoMeta> listar() {
        return repository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TipoMeta criar(@Valid @RequestBody TipoMeta tipoMeta) {
        tipoMeta.setId(null); // o id é sempre gerado pelo banco, nunca aceito do cliente
        garantirNomeLivre(tipoMeta.getNome(), null);
        return repository.save(tipoMeta);
    }

    @PutMapping("/{id}")
    public TipoMeta atualizar(@PathVariable String id, @Valid @RequestBody TipoMeta tipoMeta) {
        TipoMeta existente = buscarOuFalhar(id);
        garantirNomeLivre(tipoMeta.getNome(), id);
        existente.setNome(tipoMeta.getNome());
        existente.setUnidade(tipoMeta.getUnidade());
        return repository.save(existente);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable String id) {
        buscarOuFalhar(id);
        repository.deleteById(id);
    }

    private void garantirNomeLivre(String nome, String idAtual) {
        repository.findByNomeIgnoreCase(nome).ifPresent(outro -> {
            if (!outro.getId().equals(idAtual)) {
                throw new RecursoJaExisteException("Já existe um tipo de meta chamado " + nome);
            }
        });
    }

    private TipoMeta buscarOuFalhar(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de meta " + id + " não encontrado"));
    }
}
