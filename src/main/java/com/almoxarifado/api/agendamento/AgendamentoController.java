package com.almoxarifado.api.agendamento;

import java.util.List;

import com.almoxarifado.api.common.RecursoNaoEncontradoException;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agendamentos")
public class AgendamentoController {

    private final AgendamentoRepository repository;

    public AgendamentoController(AgendamentoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Agendamento> listar() {
        return repository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Agendamento criar(@Valid @RequestBody Agendamento agendamento) {
        agendamento.setId(null);
        return repository.save(agendamento);
    }

    @PutMapping("/{id}")
    public Agendamento atualizar(@PathVariable String id, @Valid @RequestBody Agendamento agendamento) {
        Agendamento existente = buscarOuFalhar(id);
        existente.setMaterialId(agendamento.getMaterialId());
        existente.setQtd(agendamento.getQtd());
        existente.setResponsavel(agendamento.getResponsavel());
        existente.setCliente(agendamento.getCliente());
        existente.setRetirada(agendamento.getRetirada());
        existente.setDevolucao(agendamento.getDevolucao());
        existente.setStatus(agendamento.getStatus());
        existente.setObs(agendamento.getObs());
        return repository.save(existente);
    }

    @PatchMapping("/{id}/status")
    public Agendamento definirStatus(@PathVariable String id, @Valid @RequestBody AtualizarStatusRequest corpo) {
        Agendamento existente = buscarOuFalhar(id);
        existente.setStatus(corpo.status());
        return repository.save(existente);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable String id) {
        buscarOuFalhar(id);
        repository.deleteById(id);
    }

    private Agendamento buscarOuFalhar(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento " + id + " não encontrado"));
    }
}
