package com.almoxarifado.api.vendedor;

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

/** CRUD de vendedores. Só admins acessam (ver SecurityConfig). */
@RestController
@RequestMapping("/api/vendedores")
public class VendedorController {

    private final VendedorRepository repository;

    public VendedorController(VendedorRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Vendedor> listar() {
        return repository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Vendedor criar(@Valid @RequestBody Vendedor vendedor) {
        vendedor.setId(null); // o id é sempre gerado pelo banco, nunca aceito do cliente
        garantirCodigoLivre(vendedor.getCodigo(), null);
        return repository.save(vendedor);
    }

    @PutMapping("/{id}")
    public Vendedor atualizar(@PathVariable String id, @Valid @RequestBody Vendedor vendedor) {
        Vendedor existente = buscarOuFalhar(id);
        garantirCodigoLivre(vendedor.getCodigo(), id);
        existente.setNome(vendedor.getNome());
        existente.setCodigo(vendedor.getCodigo());
        return repository.save(existente);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable String id) {
        buscarOuFalhar(id);
        repository.deleteById(id);
    }

    private void garantirCodigoLivre(String codigo, String idAtual) {
        repository.findByCodigoIgnoreCase(codigo).ifPresent(outro -> {
            if (!outro.getId().equals(idAtual)) {
                throw new RecursoJaExisteException("Já existe um vendedor com o código " + codigo);
            }
        });
    }

    private Vendedor buscarOuFalhar(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vendedor " + id + " não encontrado"));
    }
}
