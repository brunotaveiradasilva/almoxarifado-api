package com.almoxarifado.api.especialistapet;

import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.almoxarifado.api.ads.AdsEspecialistaPetService;
import com.almoxarifado.api.common.RecursoJaExisteException;
import com.almoxarifado.api.metarepresentante.Mes;

import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Campanha Especialista Pet (PremieR): os clientes e metas vêm da planilha mensal da PremieR, o
 * realizado vem da ADS. Só admins acessam (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/especialista-pet")
public class EspecialistaPetController {

    private final ClienteEspecialistaPetRepository repository;
    private final AdsEspecialistaPetService sincronizacao;

    public EspecialistaPetController(ClienteEspecialistaPetRepository repository, AdsEspecialistaPetService sincronizacao) {
        this.repository = repository;
        this.sincronizacao = sincronizacao;
    }

    /** Todos os meses, ou só um com ?mes=2026-09. */
    @GetMapping
    public List<ClienteEspecialistaPet> listar(@RequestParam(required = false) String mes) {
        if (mes == null || mes.isBlank()) return repository.findAllByOrderByRepresentanteAscNomeAsc();
        return repository.findByMesOrderByRepresentanteAscNomeAsc(Mes.ler(mes).toString());
    }

    /**
     * Troca os clientes e metas do mês pelos da planilha (a planilha nova substitui a antiga inteira).
     * O realizado começa zerado; o front-end chama /sincronizar logo depois.
     */
    @PostMapping("/importar")
    @Transactional
    public List<ClienteEspecialistaPet> importar(@Valid @RequestBody ImportarEspecialistaPetRequest corpo) {
        YearMonth mes = Mes.ler(corpo.mes());
        Set<String> codigos = new HashSet<>();
        for (ImportarEspecialistaPetRequest.Linha linha : corpo.clientes()) {
            if (!codigos.add(linha.codigoCliente().trim())) {
                throw new RecursoJaExisteException("O cliente " + linha.codigoCliente() + " aparece duas vezes na planilha");
            }
        }

        repository.deleteByMes(mes.toString());
        repository.flush();
        repository.saveAll(corpo.clientes().stream().map(linha -> {
            ClienteEspecialistaPet cliente = new ClienteEspecialistaPet();
            cliente.setMes(mes.toString());
            cliente.setCodigoCliente(linha.codigoCliente().trim());
            cliente.setNome(linha.nome().trim());
            cliente.setRepresentante(linha.representante().trim());
            cliente.setClassificacao(linha.classificacao() == null ? "" : linha.classificacao().trim().toUpperCase());
            cliente.setMetaFoco(linha.metaFoco());
            cliente.setMetaTotal(linha.metaTotal());
            return cliente;
        }).toList());
        return repository.findByMesOrderByRepresentanteAscNomeAsc(mes.toString());
    }

    /** Força agora o recálculo do realizado a partir da ADS, do mês pedido (?mes=2026-09) ou do atual. */
    @PostMapping("/sincronizar")
    public List<ClienteEspecialistaPet> sincronizar(@RequestParam(required = false) String mes) {
        YearMonth ym = Mes.ler(mes);
        sincronizacao.sincronizarMes(ym);
        return repository.findByMesOrderByRepresentanteAscNomeAsc(ym.toString());
    }
}
