package com.almoxarifado.api.fornecedor;

import java.util.List;

import com.almoxarifado.api.common.RecursoEmUsoException;
import com.almoxarifado.api.common.RecursoJaExisteException;
import com.almoxarifado.api.common.RecursoNaoEncontradoException;
import com.almoxarifado.api.meta.MetaRepository;
import com.almoxarifado.api.representante.Representante;
import com.almoxarifado.api.representante.RepresentanteRepository;

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

/** CRUD de fornecedores. Só admins acessam (ver SecurityConfig). */
@RestController
@RequestMapping("/api/fornecedores")
public class FornecedorController {

    private final FornecedorRepository repository;
    private final RepresentanteRepository representantes;
    private final MetaRepository metas;

    public FornecedorController(FornecedorRepository repository, RepresentanteRepository representantes, MetaRepository metas) {
        this.repository = repository;
        this.representantes = representantes;
        this.metas = metas;
    }

    @GetMapping
    public List<Fornecedor> listar() {
        return repository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Fornecedor criar(@Valid @RequestBody Fornecedor fornecedor) {
        fornecedor.setId(null); // o id é sempre gerado pelo banco, nunca aceito do cliente
        garantirNomeLivre(fornecedor.getNome(), null);
        return repository.save(fornecedor);
    }

    @PutMapping("/{id}")
    public Fornecedor atualizar(@PathVariable String id, @Valid @RequestBody Fornecedor fornecedor) {
        Fornecedor existente = buscarOuFalhar(id);
        garantirNomeLivre(fornecedor.getNome(), id);
        existente.setNome(fornecedor.getNome());
        return repository.save(existente);
    }

    /**
     * Exclui um fornecedor. Bloqueia se ele tiver metas (não dá pra deixar uma meta sem
     * fornecedor), mas desvincula sozinho dos representantes que trabalhavam para ele.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable String id) {
        Fornecedor fornecedor = buscarOuFalhar(id);

        if (metas.existsByFornecedorId(id)) {
            throw new RecursoEmUsoException("Este fornecedor tem metas cadastradas. Exclua ou mude as metas primeiro.");
        }

        for (Representante representante : representantes.findByFornecedoresContaining(fornecedor)) {
            representante.getFornecedores().remove(fornecedor);
            representantes.save(representante);
        }

        repository.delete(fornecedor);
    }

    private void garantirNomeLivre(String nome, String idAtual) {
        repository.findByNomeIgnoreCase(nome).ifPresent(outro -> {
            if (!outro.getId().equals(idAtual)) {
                throw new RecursoJaExisteException("Já existe um fornecedor chamado " + nome);
            }
        });
    }

    private Fornecedor buscarOuFalhar(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor " + id + " não encontrado"));
    }
}
