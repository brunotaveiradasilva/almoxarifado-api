package com.almoxarifado.api.ads;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.almoxarifado.api.especialistapet.ClienteEspecialistaPet;
import com.almoxarifado.api.especialistapet.ClienteEspecialistaPetRepository;
import com.almoxarifado.api.metarepresentante.Mes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Realizado da campanha Especialista Pet, por cliente, a partir do histórico de vendas da ADS do mês
 * (de todos os representantes — o que vale é o cliente, não quem vendeu):
 *
 * <ul>
 *   <li>todos os SKUs: peso bruto de tudo da PremieR;</li>
 *   <li>produto foco: peso bruto só das divisões NATTU (pacoteira e sacaria), e à parte quanto disso
 *   é da linha NATTU WILD (produto com "WILD" no nome);</li>
 *   <li>em R$: todos os SKUs a preço de tabela (quantidade × precoTabela), que é a base do desconto,
 *   e à parte só o produto foco em R$ — o desconto do foco e o do resto são calculados separados.</li>
 * </ul>
 *
 * Esses critérios foram conferidos batendo com a aba "Resultado" da planilha de setembro/2026 da
 * PremieR. Venda soma, devolução desconta, bonificação não conta — o mesmo de
 * {@link AdsSincronizacaoService#sinal(AdsVenda)}.
 */
@Service
public class AdsEspecialistaPetService {

    private static final Logger log = LoggerFactory.getLogger(AdsEspecialistaPetService.class);

    static final String CNPJ_PREMIER = "46325254000180";
    /** 091 NATTU PACOTEIRA e 098 NATTU SACARIA. */
    static final Set<String> DIVISOES_FOCO = Set.of("091", "098");

    private final ClienteEspecialistaPetRepository clientes;
    private final AdsHistoricoVendasClient client;
    private final Map<String, Integer> progressoPorMes = new ConcurrentHashMap<>();

    public AdsEspecialistaPetService(ClienteEspecialistaPetRepository clientes, AdsHistoricoVendasClient client) {
        this.clientes = clientes;
        this.client = client;
    }

    /** Junto com a sincronização das metas: todo dia o mês atual, e nos primeiros dias também o anterior. */
    @Scheduled(cron = "0 10 6 * * *", zone = "America/Sao_Paulo")
    public void sincronizarAgendado() {
        YearMonth atual = Mes.atual();
        sincronizarMes(atual);
        if (LocalDate.now(Mes.FUSO).getDayOfMonth() <= 5) {
            sincronizarMes(atual.minusMonths(1));
        }
    }

    /**
     * Quanto (0 a 100) já foi feito da sincronização em andamento de cada mês ("2026-09"), pra tela
     * mostrar a barra enquanto espera. Só tem o mês enquanto ele está sincronizando.
     */
    public OptionalInt progresso(YearMonth mes) {
        Integer p = progressoPorMes.get(mes.toString());
        return p == null ? OptionalInt.empty() : OptionalInt.of(p);
    }

    /** Recalcula e salva o realizado dos clientes da campanha no mês. Mês sem planilha importada não chama a ADS. */
    public void sincronizarMes(YearMonth mes) {
        progressoPorMes.put(mes.toString(), 0);
        try {
            sincronizarMesComProgresso(mes);
        } finally {
            progressoPorMes.remove(mes.toString());
        }
    }

    private void sincronizarMesComProgresso(YearMonth mes) {
        LocalDate hoje = LocalDate.now(Mes.FUSO);
        LocalDate inicio = mes.atDay(1);
        if (inicio.isAfter(hoje)) return;
        LocalDate fim = mes.atEndOfMonth().isAfter(hoje) ? hoje : mes.atEndOfMonth();

        List<ClienteEspecialistaPet> participantes = clientes.findByMes(mes.toString());
        if (participantes.isEmpty()) return;

        List<AdsVenda> vendas;
        try {
            // Buscar na ADS é quase todo o tempo: vai até 99%, e o 100% é quando termina de gravar.
            vendas = client.buscarTudo(inicio, fim, null, (lidas, total) ->
                    progressoPorMes.put(mes.toString(), total > 0 ? Math.min(99, lidas * 100 / total) : 0));
        } catch (AdsApiException e) {
            log.warn("Não foi possível sincronizar a campanha Especialista Pet de {} com a ADS: {}", mes, e.getMessage());
            throw e;
        }

        Map<String, Realizado> porCliente = somarPorCliente(vendas);
        for (ClienteEspecialistaPet cliente : participantes) {
            Realizado r = porCliente.getOrDefault(cliente.getCodigoCliente(), new Realizado());
            cliente.setRealizadoFoco(r.foco);
            cliente.setRealizadoTotal(r.total);
            cliente.setRealizadoReais(r.reais);
            cliente.setRealizadoFocoReais(r.focoReais);
            cliente.setRealizadoFocoWild(r.focoWild);
        }
        clientes.saveAll(participantes);
    }

    static Map<String, Realizado> somarPorCliente(List<AdsVenda> vendas) {
        Map<String, Realizado> porCliente = new HashMap<>();
        for (AdsVenda venda : vendas) {
            if (venda.fornecedor() == null || !CNPJ_PREMIER.equals(venda.fornecedor().cnpj())) continue;
            if (venda.cliente() == null || venda.cliente().id() == null) continue;
            int sinal = AdsSincronizacaoService.sinal(venda);
            if (sinal == 0) continue;

            Realizado r = porCliente.computeIfAbsent(venda.cliente().id(), id -> new Realizado());
            for (AdsItemVenda item : venda.itens()) {
                r.total += sinal * item.peso().bruto();
                double reais = sinal * item.quantidade() * item.valores().precoTabela();
                r.reais += reais;
                if (item.divisao() != null && DIVISOES_FOCO.contains(item.divisao().id())) {
                    r.foco += sinal * item.peso().bruto();
                    r.focoReais += reais;
                    if (ehWild(item)) r.focoWild += sinal * item.peso().bruto();
                }
            }
        }
        return porCliente;
    }

    static final class Realizado {
        double foco;
        double total;
        double reais;
        double focoReais;
        double focoWild;
    }

    /** Linha NATTU WILD (ex.: "NATTU WILD CAES AD ABOBORA 12 KG"), dentro das divisões do foco. */
    private static boolean ehWild(AdsItemVenda item) {
        return item.produto() != null && item.produto().descricao() != null
                && item.produto().descricao().toUpperCase().contains("WILD");
    }
}
