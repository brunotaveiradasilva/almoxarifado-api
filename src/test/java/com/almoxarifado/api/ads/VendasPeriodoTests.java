package com.almoxarifado.api.ads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import com.almoxarifado.api.dados.ConsultaInvalidaException;
import com.almoxarifado.api.dados.VendasPeriodo;
import com.almoxarifado.api.dados.VendasPeriodo.VendasRepresentante;
import com.almoxarifado.api.fornecedor.Fornecedor;
import com.almoxarifado.api.meta.Meta;
import com.almoxarifado.api.meta.MetaRepository;
import com.almoxarifado.api.representante.Representante;
import com.almoxarifado.api.representante.RepresentanteRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Aba Dados: vendas de um período por representante do cadastro, com ou sem filtro de fornecedor. */
class VendasPeriodoTests {

    static final String PREMIER = "46325254000180";
    static final String OUTRO = "20258278000685";
    static final LocalDate INICIO = LocalDate.of(2025, 9, 1);
    static final LocalDate FIM = LocalDate.of(2025, 9, 30);

    AdsHistoricoVendasClient client = mock(AdsHistoricoVendasClient.class);
    RepresentanteRepository representantes = mock(RepresentanteRepository.class);
    MetaRepository metas = mock(MetaRepository.class);
    AdsVendasPeriodoService servico = new AdsVendasPeriodoService(client, representantes, metas);

    Fornecedor premier;

    @BeforeEach
    void preparar() {
        Representante semCodigo = representante("rep-3", "Sem ADS", "");
        when(representantes.findAll()).thenReturn(List.of(
                representante("rep-1", "Marye", "003"), representante("rep-2", "João", "007"), semCodigo));
        when(client.buscarTudo(any(), any(), anyString())).thenReturn(List.of());

        premier = new Fornecedor();
        premier.setId("forn-1");
        premier.setNome("PremieR");
    }

    @Test
    void buscaCadaRepresentanteDoCadastroPeloCodigoAds() {
        vendas("003",
                venda("VENDA DE MERCADORIA", PREMIER, "1", item("100", 100, 10)),
                venda("DEVOLUCAO DE VENDA", PREMIER, "1", item("100", 30, 3)),
                venda("BONIFICACAO", PREMIER, "2", item("100", 500, 50)));
        vendas("007", venda("VENDA DE MERCADORIA", OUTRO, "1", item("200", 40, 4)));

        VendasPeriodo periodo = servico.buscar(INICIO, FIM, null, null);

        assertThat(periodo.representantes()).extracting(VendasRepresentante::representanteId).containsExactly("rep-1", "rep-2");
        VendasRepresentante marye = linha(periodo, "rep-1");
        assertThat(marye.nome()).isEqualTo("Marye");
        assertThat(marye.valores().valor()).isEqualTo(70);
        assertThat(marye.valores().kg()).isEqualTo(7);
        assertThat(marye.valores().clientes()).isEqualTo(1);

        // Cliente 1 comprou dos dois: no total conta uma vez só.
        assertThat(periodo.total().valor()).isEqualTo(110);
        assertThat(periodo.total().clientes()).isEqualTo(1);
        verify(client, never()).buscarTudo(any(), any(), eq(""));
    }

    @Test
    void comRepresentanteEscolhidoSoBuscaEle() {
        vendas("007", venda("VENDA DE MERCADORIA", OUTRO, "1", item("200", 40, 4)));

        VendasPeriodo periodo = servico.buscar(INICIO, FIM, "rep-2", null);

        assertThat(periodo.representantes()).extracting(VendasRepresentante::nome).containsExactly("João");
        assertThat(periodo.total().valor()).isEqualTo(40);
        verify(client, never()).buscarTudo(any(), any(), eq("003"));
    }

    @Test
    void representanteSemCodigoAdsDaErroClaro() {
        assertThatThrownBy(() -> servico.buscar(INICIO, FIM, "rep-3", null)).isInstanceOf(ConsultaInvalidaException.class);
    }

    @Test
    void comFornecedorSoContaCnpjOuDivisaoDasMetasDele() {
        metas(metaDe(premier, PREMIER, null), metaDe(premier, null, "300"));
        vendas("003",
                venda("VENDA DE MERCADORIA", PREMIER, "1", item("100", 100, 10)),
                venda("VENDA DE MERCADORIA", OUTRO, "2", item("300", 20, 2), item("200", 999, 99)));

        VendasPeriodo periodo = servico.buscar(INICIO, FIM, null, "forn-1");

        assertThat(linha(periodo, "rep-1").valores().valor()).isEqualTo(120);
        assertThat(linha(periodo, "rep-1").valores().clientes()).isEqualTo(2);
    }

    @Test
    void fornecedorSemCodigoAdsNasMetasDaErroClaro() {
        metas(metaDe(premier, null, null));

        assertThatThrownBy(() -> servico.buscar(INICIO, FIM, null, "forn-1")).isInstanceOf(ConsultaInvalidaException.class);
    }

    @Test
    void falhaDaAdsEmUmRepresentanteDerrubaAConsulta() {
        when(client.buscarTudo(any(), any(), eq("007"))).thenThrow(new AdsApiException("fora do ar"));

        assertThatThrownBy(() -> servico.buscar(INICIO, FIM, null, null)).isInstanceOf(AdsApiException.class);
    }

    private VendasRepresentante linha(VendasPeriodo periodo, String representanteId) {
        return periodo.representantes().stream().filter(l -> l.representanteId().equals(representanteId)).findFirst().orElseThrow();
    }

    private void vendas(String codigoAds, AdsVenda... vendas) {
        when(client.buscarTudo(any(), any(), eq(codigoAds))).thenReturn(List.of(vendas));
    }

    private void metas(Meta... lista) {
        when(metas.findAll()).thenReturn(List.of(lista));
    }

    private static Representante representante(String id, String nome, String codigoAds) {
        Representante r = new Representante();
        r.setId(id);
        r.setNome(nome);
        r.setCodigoAds(codigoAds);
        return r;
    }

    private static Meta metaDe(Fornecedor fornecedor, String cnpj, String divisao) {
        Meta meta = new Meta();
        meta.setFornecedor(fornecedor);
        meta.setCnpjAdsFornecedor(cnpj);
        meta.setCodigoAdsDivisao(divisao);
        return meta;
    }

    private static AdsVenda venda(String operacao, String cnpjFornecedor, String clienteId, AdsItemVenda... itens) {
        return new AdsVenda(null, 1, operacao, new AdsVenda.AdsFornecedor(cnpjFornecedor),
                new AdsVenda.AdsCliente(clienteId), new AdsVenda.AdsRepresentante("3", null, "MARYE ADS"), List.of(itens));
    }

    private static AdsItemVenda item(String divisao, double valor, double kg) {
        return new AdsItemVenda(null, new AdsItemVenda.AdsDivisao(divisao, null), 1,
                new AdsItemVenda.AdsValoresItem(valor), new AdsItemVenda.AdsPeso(kg, kg));
    }
}
