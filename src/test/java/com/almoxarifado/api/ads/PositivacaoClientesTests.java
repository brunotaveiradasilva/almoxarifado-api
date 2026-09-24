package com.almoxarifado.api.ads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.almoxarifado.api.meta.Meta;
import com.almoxarifado.api.meta.MetaRepository;
import com.almoxarifado.api.meta.UnidadeMeta;
import com.almoxarifado.api.metarepresentante.Mes;
import com.almoxarifado.api.metarepresentante.MetaRepresentante;
import com.almoxarifado.api.metarepresentante.MetaRepresentanteRepository;
import com.almoxarifado.api.metarepresentante.TotalVendidoMensalRepository;
import com.almoxarifado.api.representante.Representante;
import com.almoxarifado.api.representante.RepresentanteRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Metas CLIENTES contam clientes diferentes com saldo positivo, em vez de somar os itens. */
class PositivacaoClientesTests {

    static final String PREMIER = "46325254000180";
    static final String OUTRO = "20258278000685";

    MetaRepresentanteRepository atribuicoes = mock(MetaRepresentanteRepository.class);
    TotalVendidoMensalRepository totais = mock(TotalVendidoMensalRepository.class);
    RepresentanteRepository representantes = mock(RepresentanteRepository.class);
    AdsHistoricoVendasClient client = mock(AdsHistoricoVendasClient.class);
    AdsSincronizacaoService servico = new AdsSincronizacaoService(atribuicoes, totais, representantes, mock(MetaRepository.class), client);

    Representante marye;

    @BeforeEach
    void preparar() {
        marye = new Representante();
        marye.setId("rep-1");
        marye.setNome("Marye");
        marye.setCodigoAds("003");
        when(atribuicoes.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(totais.findByRepresentanteIdAndMes(anyString(), anyString())).thenReturn(Optional.empty());
    }

    @Test
    void contaClientesDiferentesDoFornecedor() {
        vendas(
                venda("VENDA DE MERCADORIA", PREMIER, "1", item("100", 50)),
                venda("VENDA DE MERCADORIA", PREMIER, "1", item("100", 30)), // mesmo cliente: conta uma vez
                venda("VENDA DE MERCADORIA", PREMIER, "2", item("101", 10)),
                venda("VENDA DE MERCADORIA", OUTRO, "3", item("120", 99))); // outro fornecedor: fora

        assertThat(realizado(metaPorCnpj(PREMIER))).isEqualTo(2);
    }

    @Test
    void devolucaoTotalEBonificacaoNaoPositivam() {
        vendas(
                venda("VENDA DE MERCADORIA", PREMIER, "1", item("100", 50)),
                venda("DEVOLUCAO DE VENDA", PREMIER, "1", item("100", 50)), // devolveu tudo
                venda("BONIFICACAO", PREMIER, "2", item("100", 40)), // só brinde
                venda("VENDA DE MERCADORIA", PREMIER, "3", item("100", 20)),
                venda("DEVOLUCAO DE VENDA", PREMIER, "3", item("100", 5))); // devolução parcial: segue positivado

        assertThat(realizado(metaPorCnpj(PREMIER))).isEqualTo(1);
    }

    @Test
    void porDivisaoSoContaQuemComprouNaDivisao() {
        vendas(
                venda("VENDA DE MERCADORIA", PREMIER, "1", item("100", 50), item("200", 10)),
                venda("VENDA DE MERCADORIA", PREMIER, "2", item("200", 10)),
                venda("VENDA DE MERCADORIA", PREMIER, "3", item("101", 10)));

        Meta meta = new Meta();
        meta.setUnidade(UnidadeMeta.CLIENTES);
        meta.setCodigoAdsDivisao("100, 101");
        assertThat(realizado(meta)).isEqualTo(2);
    }

    private Meta metaPorCnpj(String cnpj) {
        Meta meta = new Meta();
        meta.setUnidade(UnidadeMeta.CLIENTES);
        meta.setCnpjAdsFornecedor(cnpj);
        return meta;
    }

    private double realizado(Meta meta) {
        MetaRepresentante atribuicao = new MetaRepresentante();
        atribuicao.setRepresentante(marye);
        atribuicao.setMeta(meta);
        atribuicao.setMes(Mes.atual().toString());
        when(atribuicoes.findByMes(Mes.atual().toString())).thenReturn(List.of(atribuicao));
        return servico.sincronizarMes(Mes.atual()).get(0).getValorRealizado();
    }

    private void vendas(AdsVenda... vendas) {
        when(client.buscarTudo(any(), any(), anyString())).thenReturn(List.of(vendas));
    }

    private static AdsVenda venda(String operacao, String cnpjFornecedor, String clienteId, AdsItemVenda... itens) {
        return new AdsVenda(null, 1, operacao, new AdsVenda.AdsFornecedor(cnpjFornecedor),
                new AdsVenda.AdsCliente(clienteId), new AdsVenda.AdsRepresentante("003", null, "Marye"), List.of(itens));
    }

    private static AdsItemVenda item(String divisao, double valor) {
        return new AdsItemVenda(null, new AdsItemVenda.AdsDivisao(divisao, null), 1,
                new AdsItemVenda.AdsValoresItem(valor), new AdsItemVenda.AdsPeso(1, 1));
    }
}
