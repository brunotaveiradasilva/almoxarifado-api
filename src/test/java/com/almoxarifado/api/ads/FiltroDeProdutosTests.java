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

/** Produtos incluídos (só eles contam, ex: meta do Banni) e excluídos (ex: "Ourofino sem Wellpet") da meta. */
class FiltroDeProdutosTests {

    static final String OUROFINO = "57511234000148";

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

    @Test
    void incluidosSoContamEssesProdutos() {
        Meta meta = meta(UnidadeMeta.REAL, "");
        meta.setProdutosIncluidos("5085, 901");
        assertThat(realizado(meta)).isEqualTo(65.98 + 40, offset(0.001));
    }

    @Test
    void incluidosFuncionamSozinhosSemCnpjNemDivisao() {
        Meta meta = new Meta();
        meta.setUnidade(UnidadeMeta.UNIDADE);
        meta.setProdutosIncluidos("wellpet");
        assertThat(realizado(meta)).isEqualTo(2);
    }

    @Test
    void excluidoGanhaDoIncluido() {
        Meta meta = meta(UnidadeMeta.REAL, "5084");
        meta.setProdutosIncluidos("WELLPET");
        assertThat(realizado(meta)).isEqualTo(65.98, offset(0.001));
    }

    @Test
    void fatorMultiplicaUnidadesDoKit() {
        vendas(
                venda("1", item("4727", "BANNI 3 0,90 ML", "031", 2, 98.51)),
                venda("2", item("4931", "BANNI 3 0,30 ML C/ 3 FLACONETES", "031", 2, 204.78)),
                venda("3", item("4932", "BANNI 3 0,90 ML C/ 3 FLACONETES", "031", 1, 112.84)),
                venda("4", item("9999", "NEOPET CAES 0,67 ML", "031", 5, 100)));

        Meta unidades = new Meta();
        unidades.setUnidade(UnidadeMeta.UNIDADE);
        unidades.setProdutosIncluidos("4727, 4931*3, 4932x3");
        assertThat(realizado(unidades)).isEqualTo(2 + 2 * 3 + 3);

        Meta porNome = new Meta();
        porNome.setUnidade(UnidadeMeta.UNIDADE);
        porNome.setProdutosIncluidos("BANNI, FLACONETES*3");
        assertThat(realizado(porNome)).isEqualTo(11);
    }

    @Test
    void fatorNaoMudaReaisNemPositivacao() {
        vendas(
                venda("1", item("4931", "BANNI 3 0,30 ML C/ 3 FLACONETES", "031", 2, 204.78)),
                venda("2", item("4727", "BANNI 3 0,90 ML", "031", 1, 49.26)));

        Meta reais = new Meta();
        reais.setUnidade(UnidadeMeta.REAL);
        reais.setProdutosIncluidos("4931*3, 4727");
        assertThat(realizado(reais)).isEqualTo(204.78 + 49.26, offset(0.001));

        Meta clientes = new Meta();
        clientes.setUnidade(UnidadeMeta.CLIENTES);
        clientes.setProdutosIncluidos("4931*3, 4727");
        assertThat(realizado(clientes)).isEqualTo(2);
    }

    @Test
    void metaEmKgGuardaTambemORealizadoEmReais() {
        Meta kg = meta(UnidadeMeta.KG, "WELLPET");
        assertThat(sincronizar(kg).getValorRealizado()).isEqualTo(2);
        assertThat(sincronizar(kg).getRealizadoEmReais()).isEqualTo(140);

        assertThat(sincronizar(meta(UnidadeMeta.REAL, "")).getRealizadoEmReais()).isNull();
    }

    private Meta meta(UnidadeMeta unidade, String excluidos) {
        Meta meta = new Meta();
        meta.setUnidade(unidade);
        meta.setCnpjAdsFornecedor(OUROFINO);
        meta.setProdutosExcluidos(excluidos);
        return meta;
    }

    private double realizado(Meta meta) {
        return sincronizar(meta).getValorRealizado();
    }

    private MetaRepresentante sincronizar(Meta meta) {
        MetaRepresentante atribuicao = new MetaRepresentante();
        atribuicao.setRepresentante(marye);
        atribuicao.setMeta(meta);
        atribuicao.setMes(Mes.atual().toString());
        when(atribuicoes.findByMes(Mes.atual().toString())).thenReturn(List.of(atribuicao));
        return servico.sincronizarMes(Mes.atual()).get(0);
    }

    private void vendas(AdsVenda... vendas) {
        when(client.buscarTudo(any(), any(), anyString())).thenReturn(List.of(vendas));
    }

    private static AdsVenda venda(String clienteId, AdsItemVenda... itens) {
        return new AdsVenda(null, 1, "VENDA DE MERCADORIA", new AdsVenda.AdsFornecedor(OUROFINO),
                new AdsVenda.AdsCliente(clienteId), new AdsVenda.AdsRepresentante("003", null, "Marye"), List.of(itens));
    }

    private static AdsItemVenda item(String produtoId, String descricao, String divisao, double valor) {
        return item(produtoId, descricao, divisao, 1, valor);
    }

    private static AdsItemVenda item(String produtoId, String descricao, String divisao, double quantidade, double valor) {
        return new AdsItemVenda(new AdsItemVenda.AdsProduto(produtoId, descricao), new AdsItemVenda.AdsDivisao(divisao, null),
                quantidade, new AdsItemVenda.AdsValoresItem(valor), new AdsItemVenda.AdsPeso(1, 1));
    }
}
