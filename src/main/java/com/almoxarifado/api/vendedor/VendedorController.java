package com.almoxarifado.api.vendedor;

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

/** CRUD de vendedores. Só admins acessam (ver SecurityConfig). */
@RestController
@RequestMapping("/api/vendedores")
public class VendedorController {

    private final VendedorRepository repository;
    private final FornecedorRepository fornecedores;

    public VendedorController(VendedorRepository repository, FornecedorRepository fornecedores) {
        this.repository = repository;
        this.fornecedores = fornecedores;
    }

    @GetMapping
    public List<Vendedor> listar() {
        return repository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Vendedor criar(@Valid @RequestBody VendedorRequest corpo) {
        Vendedor vendedor = new Vendedor();
        preencher(vendedor, corpo);
        return repository.save(vendedor);
    }

    @PutMapping("/{id}")
    public Vendedor atualizar(@PathVariable String id, @Valid @RequestBody VendedorRequest corpo) {
        Vendedor existente = buscarOuFalhar(id);
        preencher(existente, corpo);
        return repository.save(existente);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable String id) {
        buscarOuFalhar(id);
        repository.deleteById(id);
    }

    private void preencher(Vendedor vendedor, VendedorRequest corpo) {
        vendedor.setNome(corpo.nome());
        vendedor.setEmail(corpo.email());
        vendedor.setCelular(corpo.celular());
        vendedor.setFornecedores(buscarFornecedores(corpo.fornecedorIds()));
    }

    private Set<Fornecedor> buscarFornecedores(List<String> ids) {
        List<Fornecedor> encontrados = fornecedores.findAllById(ids);
        if (encontrados.size() != new LinkedHashSet<>(ids).size()) {
            throw new RecursoNaoEncontradoException("Um ou mais fornecedores informados não existem");
        }
        return new LinkedHashSet<>(encontrados);
    }

    private Vendedor buscarOuFalhar(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vendedor " + id + " não encontrado"));
    }
}
