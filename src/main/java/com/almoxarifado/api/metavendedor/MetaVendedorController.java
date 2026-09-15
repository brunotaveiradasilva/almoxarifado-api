package com.almoxarifado.api.metavendedor;

import java.util.List;

import com.almoxarifado.api.common.RecursoJaExisteException;
import com.almoxarifado.api.common.RecursoNaoEncontradoException;
import com.almoxarifado.api.meta.Meta;
import com.almoxarifado.api.meta.MetaRepository;
import com.almoxarifado.api.vendedor.Vendedor;
import com.almoxarifado.api.vendedor.VendedorRepository;

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

/** Valor de meta (alvo + realizado) atribuído a um vendedor. Só admins acessam (ver SecurityConfig). */
@RestController
@RequestMapping("/api/metas-vendedor")
public class MetaVendedorController {

    private final MetaVendedorRepository repository;
    private final VendedorRepository vendedores;
    private final MetaRepository metas;

    public MetaVendedorController(MetaVendedorRepository repository, VendedorRepository vendedores, MetaRepository metas) {
        this.repository = repository;
        this.vendedores = vendedores;
        this.metas = metas;
    }

    @GetMapping
    public List<MetaVendedor> listar() {
        return repository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MetaVendedor criar(@Valid @RequestBody MetaVendedorRequest corpo) {
        garantirAtribuicaoLivre(corpo.vendedorId(), corpo.metaId(), null);

        MetaVendedor atribuicao = new MetaVendedor();
        preencher(atribuicao, corpo);
        return repository.save(atribuicao);
    }

    @PutMapping("/{id}")
    public MetaVendedor atualizar(@PathVariable String id, @Valid @RequestBody MetaVendedorRequest corpo) {
        MetaVendedor existente = buscarOuFalhar(id);
        garantirAtribuicaoLivre(corpo.vendedorId(), corpo.metaId(), id);
        preencher(existente, corpo);
        return repository.save(existente);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable String id) {
        buscarOuFalhar(id);
        repository.deleteById(id);
    }

    private void preencher(MetaVendedor atribuicao, MetaVendedorRequest corpo) {
        atribuicao.setVendedor(buscarVendedor(corpo.vendedorId()));
        atribuicao.setMeta(buscarMeta(corpo.metaId()));
        atribuicao.setValorMeta(corpo.valorMeta());
        atribuicao.setValorRealizado(corpo.valorRealizado());
    }

    /** Só pode existir uma atribuição por par vendedor+meta — a segunda vira uma edição da primeira. */
    private void garantirAtribuicaoLivre(String vendedorId, String metaId, String idAtual) {
        repository.findByVendedorIdAndMetaId(vendedorId, metaId).ifPresent(outra -> {
            if (!outra.getId().equals(idAtual)) {
                throw new RecursoJaExisteException("Esse vendedor já tem um valor atribuído pra essa meta");
            }
        });
    }

    private Vendedor buscarVendedor(String id) {
        return vendedores.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vendedor " + id + " não encontrado"));
    }

    private Meta buscarMeta(String id) {
        return metas.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Meta " + id + " não encontrada"));
    }

    private MetaVendedor buscarOuFalhar(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Valor de meta " + id + " não encontrado"));
    }
}
