package com.almoxarifado.api.meta;

import java.util.List;

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

/** CRUD de metas. Só admins acessam (ver SecurityConfig). */
@RestController
@RequestMapping("/api/metas")
public class MetaController {

    private final MetaRepository repository;
    private final FornecedorRepository fornecedores;

    public MetaController(MetaRepository repository, FornecedorRepository fornecedores) {
        this.repository = repository;
        this.fornecedores = fornecedores;
    }

    @GetMapping
    public List<Meta> listar() {
        return repository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Meta criar(@Valid @RequestBody MetaRequest corpo) {
        Meta meta = new Meta();
        preencher(meta, corpo);
        return repository.save(meta);
    }

    @PutMapping("/{id}")
    public Meta atualizar(@PathVariable String id, @Valid @RequestBody MetaRequest corpo) {
        Meta existente = buscarOuFalhar(id);
        preencher(existente, corpo);
        return repository.save(existente);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable String id) {
        buscarOuFalhar(id);
        repository.deleteById(id);
    }

    private void preencher(Meta meta, MetaRequest corpo) {
        meta.setNome(corpo.nome());
        meta.setUnidade(corpo.unidade());
        meta.setCodigoAdsDivisao(corpo.codigoAdsDivisao());
        meta.setCnpjAdsFornecedor(corpo.cnpjAdsFornecedor());
        meta.setProdutosExcluidos(corpo.produtosExcluidos());
        meta.setProdutosIncluidos(corpo.produtosIncluidos());
        meta.setFornecedor(buscarFornecedor(corpo.fornecedorId()));
    }

    private Fornecedor buscarFornecedor(String fornecedorId) {
        return fornecedores.findById(fornecedorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor " + fornecedorId + " não encontrado"));
    }

    private Meta buscarOuFalhar(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Meta " + id + " não encontrada"));
    }
}
