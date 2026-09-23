package com.almoxarifado.api.ads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.almoxarifado.api.meta.Meta;
import com.almoxarifado.api.meta.UnidadeMeta;
import com.almoxarifado.api.metarepresentante.Mes;
import com.almoxarifado.api.metarepresentante.MetaRepresentante;
import com.almoxarifado.api.metarepresentante.MetaRepresentanteRepository;
import com.almoxarifado.api.metarepresentante.TotalVendidoMensalRepository;
import com.almoxarifado.api.representante.Representante;
import com.almoxarifado.api.representante.RepresentanteRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Produtos excluídos da meta (ex: "Ourofino sem Wellpet") não contam no realizado. */
class ProdutosExcluidosTests {

    static final String OUROFINO = "57511234000148";

    MetaRepresentanteRepository atribuicoes = mock(MetaRepresentanteRepository.class);
    TotalVendidoMensalRepository totais = mock(TotalVendidoMensalRepository.class);
    RepresentanteRepository representantes = mock(RepresentanteRepository.class);
    AdsHistoricoVendasClient client = mock(AdsHistoricoVendasClient.class);
    AdsSincronizacaoService servico = new AdsSincronizacaoService(atribuicoes, totais, representantes, client);

    Representante marye;

    @BeforeEach
    void preparar() {
        marye = new Representante();
        marye.setId("rep-1");
        marye.setNome("Marye");
        marye.setCodigoAds("003");
        when(atribuicoes.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(totais.findByRepresentanteIdAndMes(anyString(), anyString())).thenReturn(Optional.empty());
        vendas(
                venda("1", item("5085", "WELLPET 400MG (20,1 A 40KG)", "120", 65.98)),
                venda("1", item("5084", "Wellpet 200MG (10,1 A 20KG)", "120", 57.68), item("900", "OUROVET X", "121", 100)),
                venda("2", item("901", "OUROVET Y", "121", 40)));
    }

    @Test
    void semExclusaoContaTudo() {
        assertThat(realizado(meta(UnidadeMeta.REAL, ""))).isEqualTo(65.98 + 57.68 + 100 + 40, offset(0.001));
    }

    @Test
    void excluiPorTrechoDoNomeSemDiferenciarMaiuscula() {
        assertThat(realizado(meta(UnidadeMeta.REAL, "wellpet"))).isEqualTo(140);
    }

    @Test
    void excluiPorCodigoDoProduto() {
        assertThat(realizado(meta(UnidadeMeta.REAL, "5085, 900"))).isEqualTo(57.68 + 40, offset(0.001));
    }

    @Test
    void codigoNaoBateComPedacoDeOutroCodigo() {
        assertThat(realizado(meta(UnidadeMeta.UNIDADE, "90"))).isEqualTo(4);
    }

    @Test
    void positivacaoIgnoraClienteQueSoComprouOExcluido() {
        vendas(
                venda("1", item("5085", "WELLPET 400MG", "120", 65.98)),
                venda("2", item("901", "OUROVET Y", "121", 40)));
        assertThat(realizado(meta(UnidadeMeta.CLIENTES, "WELLPET"))).isEqualTo(1);
    }

    @Test
    void valeTambemPraMetaPorDivisao() {
        Meta meta = new Meta();
        meta.setUnidade(UnidadeMeta.REAL);
        meta.setCodigoAdsDivisao("120,121");
        meta.setProdutosExcluidos("WELLPET");
        assertThat(realizado(meta)).isEqualTo(140);
    }

    private Meta meta(UnidadeMeta unidade, String excluidos) {
        Meta meta = new Meta();
        meta.setUnidade(unidade);
        meta.setCnpjAdsFornecedor(OUROFINO);
        meta.setProdutosExcluidos(excluidos);
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

    private static AdsVenda venda(String clienteId, AdsItemVenda... itens) {
        return new AdsVenda(null, 1, "VENDA DE MERCADORIA", new AdsVenda.AdsFornecedor(OUROFINO),
                new AdsVenda.AdsCliente(clienteId), new AdsVenda.AdsRepresentante("003", null, "Marye"), List.of(itens));
    }

    private static AdsItemVenda item(String produtoId, String descricao, String divisao, double valor) {
        return new AdsItemVenda(new AdsItemVenda.AdsProduto(produtoId, descricao), new AdsItemVenda.AdsDivisao(divisao, null), 1,
                new AdsItemVenda.AdsValoresItem(valor), new AdsItemVenda.AdsPeso(1, 1));
    }
}
