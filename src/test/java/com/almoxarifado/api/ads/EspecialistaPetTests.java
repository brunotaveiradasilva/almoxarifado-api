package com.almoxarifado.api.ads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import com.almoxarifado.api.especialistapet.ClienteEspecialistaPet;
import com.almoxarifado.api.especialistapet.ClienteEspecialistaPetRepository;
import com.almoxarifado.api.metarepresentante.Mes;

import org.junit.jupiter.api.Test;

/** Realizado da campanha Especialista Pet: por cliente, só PremieR, com NATTU como produto foco. */
class EspecialistaPetTests {

    static final String PREMIER = AdsEspecialistaPetService.CNPJ_PREMIER;
    static final String OUTRO = "20258278000685";

    ClienteEspecialistaPetRepository clientes = mock(ClienteEspecialistaPetRepository.class);
    AdsHistoricoVendasClient client = mock(AdsHistoricoVendasClient.class);
    AdsEspecialistaPetService servico = new AdsEspecialistaPetService(clientes, client);

    @Test
    void somaPesoBrutoDaPremierESeparaONattu() {
        Map<String, AdsEspecialistaPetService.Realizado> r = AdsEspecialistaPetService.somarPorCliente(List.of(
                venda("VENDA DE MERCADORIA", PREMIER, "1",
                        item("091", 2, 2.5, 30), // NATTU pacoteira: 5 kg
                        item("098", 1, 10.1, 100), // NATTU sacaria: 10,1 kg
                        item("079", 1, 15, 120)), // outra linha PremieR: só no total
                venda("VENDA DE MERCADORIA", OUTRO, "1", item("091", 1, 99, 999)))); // outro fornecedor: fora

        assertThat(r.get("1").foco).isCloseTo(15.1, within(1e-9));
        assertThat(r.get("1").total).isCloseTo(30.1, within(1e-9));
        assertThat(r.get("1").reais).isCloseTo(2 * 30 + 100 + 120, within(1e-9));
        assertThat(r.get("1").focoReais).isCloseTo(2 * 30 + 100, within(1e-9));
        assertThat(r.get("1").focoWild).isZero();
    }

    @Test
    void separaALinhaNattuWildDentroDoFoco() {
        AdsItemVenda wild = new AdsItemVenda(new AdsItemVenda.AdsProduto("5196", "NATTU WILD CAES AD ABOBORA 12 KG"),
                new AdsItemVenda.AdsDivisao("098", null), 2, new AdsItemVenda.AdsValoresItem(0, 150),
                new AdsItemVenda.AdsPeso(24, 24));
        Map<String, AdsEspecialistaPetService.Realizado> r = AdsEspecialistaPetService.somarPorCliente(List.of(
                venda("VENDA DE MERCADORIA", PREMIER, "1", wild, item("091", 1, 2.5, 30))));

        assertThat(r.get("1").foco).isCloseTo(26.5, within(1e-9));
        assertThat(r.get("1").focoWild).isCloseTo(24, within(1e-9));
    }

    @Test
    void devolucaoDescontaEBonificacaoNaoConta() {
        Map<String, AdsEspecialistaPetService.Realizado> r = AdsEspecialistaPetService.somarPorCliente(List.of(
                venda("VENDA DE MERCADORIA", PREMIER, "1", item("079", 4, 15, 100)),
                venda("DEV. VENDA", PREMIER, "1", item("079", 1, 15, 100)),
                venda("BONIFICACAO CREDITO", PREMIER, "1", item("091", 1, 10.1, 80))));

        assertThat(r.get("1").total).isCloseTo(45, within(1e-9));
        assertThat(r.get("1").foco).isZero();
        assertThat(r.get("1").reais).isCloseTo(300, within(1e-9));
    }

    @Test
    void clienteSemVendaFicaZerado() {
        ClienteEspecialistaPet semVenda = cliente("2");
        semVenda.setRealizadoTotal(50); // de uma sincronização anterior
        when(clientes.findByMes(Mes.atual().toString())).thenReturn(List.of(semVenda));
        when(client.buscarTudo(any(), any(), isNull(), any()))
                .thenReturn(List.of(venda("VENDA DE MERCADORIA", PREMIER, "1", item("079", 1, 15, 100))));

        servico.sincronizarMes(Mes.atual());

        assertThat(semVenda.getRealizadoTotal()).isZero();
    }

    @Test
    void mesSemPlanilhaNaoChamaAAds() {
        when(clientes.findByMes(any())).thenReturn(List.of());
        servico.sincronizarMes(Mes.atual());
        verify(client, never()).buscarTudo(any(), any(), any(), any());
    }

    @Test
    void progressoAcompanhaAsPaginasESomeNoFim() {
        when(clientes.findByMes(Mes.atual().toString())).thenReturn(List.of(cliente("1")));
        List<Integer> vistos = new ArrayList<>();
        when(client.buscarTudo(any(), any(), isNull(), any())).thenAnswer(inv -> {
            BiConsumer<Integer, Integer> aoLerPagina = inv.getArgument(3);
            aoLerPagina.accept(100, 400);
            vistos.add(servico.progresso(Mes.atual()).getAsInt());
            aoLerPagina.accept(400, 400);
            vistos.add(servico.progresso(Mes.atual()).getAsInt());
            return List.of();
        });

        servico.sincronizarMes(Mes.atual());

        // Até 99% enquanto busca; terminado, o mês sai da lista de sincronizações em andamento.
        assertThat(vistos).containsExactly(25, 99);
        assertThat(servico.progresso(Mes.atual())).isEmpty();
    }

    private static ClienteEspecialistaPet cliente(String codigo) {
        ClienteEspecialistaPet c = new ClienteEspecialistaPet();
        c.setCodigoCliente(codigo);
        return c;
    }

    private static AdsVenda venda(String operacao, String cnpjFornecedor, String clienteId, AdsItemVenda... itens) {
        return new AdsVenda(null, 1, operacao, new AdsVenda.AdsFornecedor(cnpjFornecedor),
                new AdsVenda.AdsCliente(clienteId), null, List.of(itens));
    }

    /** `pesoUnitario` e `precoTabela` são por unidade; o peso bruto do item é o total, como a ADS manda. */
    private static AdsItemVenda item(String divisao, double quantidade, double pesoUnitario, double precoTabela) {
        return new AdsItemVenda(null, new AdsItemVenda.AdsDivisao(divisao, null), quantidade,
                new AdsItemVenda.AdsValoresItem(quantidade * precoTabela * 0.9, precoTabela),
                new AdsItemVenda.AdsPeso(quantidade * pesoUnitario, quantidade * pesoUnitario));
    }
}
