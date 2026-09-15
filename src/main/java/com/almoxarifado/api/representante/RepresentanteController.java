package com.almoxarifado.api.representante;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.almoxarifado.api.common.RecursoNaoEncontradoException;
import com.almoxarifado.api.fornecedor.Fornecedor;
import com.almoxarifado.api.fornecedor.FornecedorRepository;

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

/** CRUD de representantes. Só admins acessam (ver SecurityConfig). */
@RestController
@RequestMapping("/api/representantes")
public class RepresentanteController {

    private final RepresentanteRepository repository;
    private final FornecedorRepository fornecedores;

    public RepresentanteController(RepresentanteRepository repository, FornecedorRepository fornecedores) {
        this.repository = repository;
        this.fornecedores = fornecedores;
    }

    @GetMapping
    public List<Representante> listar() {
        return repository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Representante criar(@Valid @RequestBody RepresentanteRequest corpo) {
        Representante representante = new Representante();
        preencher(representante, corpo);
        return repository.save(representante);
    }

    @PutMapping("/{id}")
    public Representante atualizar(@PathVariable String id, @Valid @RequestBody RepresentanteRequest corpo) {
        Representante existente = buscarOuFalhar(id);
        preencher(existente, corpo);
        return repository.save(existente);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable String id) {
        buscarOuFalhar(id);
        repository.deleteById(id);
    }

    private void preencher(Representante representante, RepresentanteRequest corpo) {
        representante.setNome(corpo.nome());
        representante.setEmail(corpo.email());
        representante.setCelular(corpo.celular());
        representante.setFornecedores(buscarFornecedores(corpo.fornecedorIds()));
    }

    private Set<Fornecedor> buscarFornecedores(List<String> ids) {
        List<Fornecedor> encontrados = fornecedores.findAllById(ids);
        if (encontrados.size() != new LinkedHashSet<>(ids).size()) {
            throw new RecursoNaoEncontradoException("Um ou mais fornecedores informados não existem");
        }
        return new LinkedHashSet<>(encontrados);
    }

    private Representante buscarOuFalhar(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Representante " + id + " não encontrado"));
    }
}
