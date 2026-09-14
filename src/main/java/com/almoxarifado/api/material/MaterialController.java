package com.almoxarifado.api.material;

import java.util.List;

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

@RestController
@RequestMapping("/api/materiais")
public class MaterialController {

    private final MaterialRepository repository;

    public MaterialController(MaterialRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Material> listar() {
        return repository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Material criar(@Valid @RequestBody Material material) {
        material.setId(null); // o id é sempre gerado pelo banco, nunca aceito do cliente
        return repository.save(material);
    }

    @PutMapping("/{id}")
    public Material atualizar(@PathVariable String id, @Valid @RequestBody Material material) {
        Material existente = buscarOuFalhar(id);
        existente.setNome(material.getNome());
        existente.setCodigo(material.getCodigo());
        existente.setEstoque(material.getEstoque());
        existente.setObs(material.getObs());
        return repository.save(existente);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable String id) {
        buscarOuFalhar(id);
        repository.deleteById(id);
    }

    private Material buscarOuFalhar(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Material " + id + " não encontrado"));
    }
}
