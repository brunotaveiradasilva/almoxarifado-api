package com.almoxarifado.api.ads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
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

/** Aba Dados: vendas de um período agrupadas por representante, com ou sem filtro de fornecedor. */
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
        Representante marye = new Representante();
        marye.setId("rep-1");
        marye.setNome("Marye");
        marye.setCodigoAds("003");
        when(representantes.findAll()).thenReturn(List.of(marye));

        premier = new Fornecedor();
        premier.setId("forn-1");
        premier.setNome("PremieR");
    }

    @Test
    void somaPorRepresentanteDescontandoDevolucaoESemBonificacao() {
        vendas(
                venda("3", "Marye", "VENDA DE MERCADORIA", PREMIER, "1", item("100", 100, 10)),
                venda("3", "Marye", "DEVOLUCAO DE VENDA", PREMIER, "1", item("100", 30, 3)),
                venda("3", "Marye", "BONIFICACAO", PREMIER, "2", item("100", 500, 50)),
                venda("7", "João ADS", "VENDA DE MERCADORIA", OUTRO, "9", item("200", 40, 4)));

        VendasPeriodo periodo = servico.buscar(INICIO, FIM, null);

        VendasRepresentante marye = linha(periodo, "3");
        assertThat(marye.representanteId()).isEqualTo("rep-1"); // "003" no cadastro casa com "3" da ADS
        assertThat(marye.nome()).isEqualTo("Marye");
        assertThat(marye.valores().valor()).isEqualTo(70);
        assertThat(marye.valores().kg()).isEqualTo(7);
        assertThat(marye.valores().clientes()).isEqualTo(1);

        VendasRepresentante foraDoCadastro = linha(periodo, "7");
        assertThat(foraDoCadastro.representanteId()).isNull();
        assertThat(foraDoCadastro.nome()).isEqualTo("João ADS");

        assertThat(periodo.total().valor()).isEqualTo(110);
        assertThat(periodo.total().clientes()).isEqualTo(2);
    }

    @Test
    void comFornecedorSoContaCnpjOuDivisaoDasMetasDele() {
        metas(metaDe(premier, PREMIER, null), metaDe(premier, null, "300"));
        vendas(
                venda("3", "Marye", "VENDA DE MERCADORIA", PREMIER, "1", item("100", 100, 10)),
                venda("3", "Marye", "VENDA DE MERCADORIA", OUTRO, "2", item("300", 20, 2), item("200", 999, 99)));

        VendasPeriodo periodo = servico.buscar(INICIO, FIM, "forn-1");

        assertThat(linha(periodo, "3").valores().valor()).isEqualTo(120);
        assertThat(linha(periodo, "3").valores().clientes()).isEqualTo(2);
    }

    @Test
    void fornecedorSemCodigoAdsNasMetasDaErroClaro() {
        metas(metaDe(premier, null, null));

        assertThatThrownBy(() -> servico.buscar(INICIO, FIM, "forn-1")).isInstanceOf(ConsultaInvalidaException.class);
    }

    private VendasRepresentante linha(VendasPeriodo periodo, String codigoAds) {
        return periodo.representantes().stream().filter(l -> l.codigoAds().equals(codigoAds)).findFirst().orElseThrow();
    }

    private void vendas(AdsVenda... vendas) {
        when(client.buscarTudo(any(), any(), isNull())).thenReturn(List.of(vendas));
    }

    private void metas(Meta... lista) {
        when(metas.findAll()).thenReturn(List.of(lista));
    }

    private static Meta metaDe(Fornecedor fornecedor, String cnpj, String divisao) {
        Meta meta = new Meta();
        meta.setFornecedor(fornecedor);
        meta.setCnpjAdsFornecedor(cnpj);
        meta.setCodigoAdsDivisao(divisao);
        return meta;
    }

    private static AdsVenda venda(String reprId, String reprNome, String operacao, String cnpjFornecedor, String clienteId,
            AdsItemVenda... itens) {
        return new AdsVenda(null, 1, operacao, new AdsVenda.AdsFornecedor(cnpjFornecedor),
                new AdsVenda.AdsCliente(clienteId), new AdsVenda.AdsRepresentante(reprId, null, reprNome), List.of(itens));
    }

    private static AdsItemVenda item(String divisao, double valor, double kg) {
        return new AdsItemVenda(null, new AdsItemVenda.AdsDivisao(divisao, null), 1,
                new AdsItemVenda.AdsValoresItem(valor), new AdsItemVenda.AdsPeso(kg, kg));
    }
}
