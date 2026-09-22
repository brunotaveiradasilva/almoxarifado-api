package com.almoxarifado.api.metarepresentante;

import java.util.List;

import com.almoxarifado.api.ads.AdsSincronizacaoService;
import com.almoxarifado.api.common.RecursoJaExisteException;
import com.almoxarifado.api.common.RecursoNaoEncontradoException;
import com.almoxarifado.api.meta.Meta;
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

/** Valor de meta (alvo + realizado) atribuído a um representante. Só admins acessam (ver SecurityConfig). */
@RestController
@RequestMapping("/api/metas-representante")
public class MetaRepresentanteController {

    private final MetaRepresentanteRepository repository;
    private final RepresentanteRepository representantes;
    private final MetaRepository metas;
    private final AdsSincronizacaoService sincronizacao;

    public MetaRepresentanteController(
            MetaRepresentanteRepository repository,
            RepresentanteRepository representantes,
            MetaRepository metas,
            AdsSincronizacaoService sincronizacao) {
        this.repository = repository;
        this.representantes = representantes;
        this.metas = metas;
        this.sincronizacao = sincronizacao;
    }

    @GetMapping
    public List<MetaRepresentante> listar() {
        return repository.findAll();
    }

    /** Força agora o recálculo do realizado a partir da ADS (o mesmo que roda sozinho todo dia). */
    @PostMapping("/sincronizar")
    public List<MetaRepresentante> sincronizar() {
        return sincronizacao.sincronizarTudo();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MetaRepresentante criar(@Valid @RequestBody MetaRepresentanteRequest corpo) {
        garantirAtribuicaoLivre(corpo.representanteId(), corpo.metaId(), null);

        MetaRepresentante atribuicao = new MetaRepresentante();
        preencher(atribuicao, corpo);
        return repository.save(atribuicao);
    }

    @PutMapping("/{id}")
    public MetaRepresentante atualizar(@PathVariable String id, @Valid @RequestBody MetaRepresentanteRequest corpo) {
        MetaRepresentante existente = buscarOuFalhar(id);
        garantirAtribuicaoLivre(corpo.representanteId(), corpo.metaId(), id);
        preencher(existente, corpo);
        return repository.save(existente);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable String id) {
        buscarOuFalhar(id);
        repository.deleteById(id);
    }

    private void preencher(MetaRepresentante atribuicao, MetaRepresentanteRequest corpo) {
        atribuicao.setRepresentante(buscarRepresentante(corpo.representanteId()));
        atribuicao.setMeta(buscarMeta(corpo.metaId()));
        atribuicao.setValorMeta(corpo.valorMeta());
        atribuicao.setValorRealizado(corpo.valorRealizado());
    }

    /** Só pode existir uma atribuição por par representante+meta — a segunda vira uma edição da primeira. */
    private void garantirAtribuicaoLivre(String representanteId, String metaId, String idAtual) {
        repository.findByRepresentanteIdAndMetaId(representanteId, metaId).ifPresent(outra -> {
            if (!outra.getId().equals(idAtual)) {
                throw new RecursoJaExisteException("Esse representante já tem um valor atribuído pra essa meta");
            }
        });
    }

    private Representante buscarRepresentante(String id) {
        return representantes.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Representante " + id + " não encontrado"));
    }

    private Meta buscarMeta(String id) {
        return metas.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Meta " + id + " não encontrada"));
    }

    private MetaRepresentante buscarOuFalhar(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Valor de meta " + id + " não encontrado"));
    }
}
