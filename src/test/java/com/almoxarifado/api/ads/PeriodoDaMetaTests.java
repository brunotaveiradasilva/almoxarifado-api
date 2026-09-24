package com.almoxarifado.api.ads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import com.almoxarifado.api.meta.Meta;
import com.almoxarifado.api.meta.MetaRepository;
import com.almoxarifado.api.meta.UnidadeMeta;
import com.almoxarifado.api.metarepresentante.MetaRepresentante;
import com.almoxarifado.api.metarepresentante.MetaRepresentanteRepository;
import com.almoxarifado.api.metarepresentante.TotalVendidoMensalRepository;
import com.almoxarifado.api.representante.Representante;
import com.almoxarifado.api.representante.RepresentanteRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Meta com período (ex: dia 1 a 19 e 20 a 31): só as vendas faturadas nesses dias contam. */
class PeriodoDaMetaTests {

    static final String OUROFINO = "57511234000148";
    /** Mês fechado com 30 dias, pra testar o "até 31" e não depender do dia de hoje. */
    static final YearMonth SETEMBRO = YearMonth.of(2025, 9);

    MetaRepresentanteRepository atribuicoes = mock(MetaRepresentanteRepository.class);
    TotalVendidoMensalRepository totais = mock(TotalVendidoMensalRepository.class);
    RepresentanteRepository representantes = mock(RepresentanteRepository.class);
    AdsHistoricoVendasClient client = mock(AdsHistoricoVendasClient.class);
    AdsSincronizacaoService servico = new AdsSincronizacaoService(
            atribuicoes, totais, representantes, mock(MetaRepository.class), client);

    Representante marye;

    @BeforeEach
    void preparar() {
        marye = new Representante();
        marye.setId("rep-1");
        marye.setNome("Marye");
        marye.setCodigoAds("003");
        when(atribuicoes.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(totais.findByRepresentanteIdAndMes(anyString(), anyString())).thenReturn(Optional.empty());
        when(client.buscarTudo(any(), any(), anyString())).thenReturn(List.of(
                venda(1, 100), venda(19, 20), venda(20, 5), venda(30, 1)));
    }

    @Test
    void semPeriodoContaOMesInteiro() {
        assertThat(realizado(meta(null, null))).isEqualTo(126);
    }

    @Test
    void primeiraParteDoMes() {
        assertThat(realizado(meta(1, 19))).isEqualTo(120);
    }

    @Test
    void segundaParteVaiAteOFimMesmoComMesDe30Dias() {
        assertThat(realizado(meta(20, 31))).isEqualTo(6);
    }

    private Meta meta(Integer inicio, Integer fim) {
        Meta meta = new Meta();
        meta.setUnidade(UnidadeMeta.REAL);
        meta.setCnpjAdsFornecedor(OUROFINO);
        meta.setDiaInicio(inicio);
        meta.setDiaFim(fim);
        return meta;
    }

    private double realizado(Meta meta) {
        MetaRepresentante atribuicao = new MetaRepresentante();
        atribuicao.setRepresentante(marye);
        atribuicao.setMeta(meta);
        atribuicao.setMes(SETEMBRO.toString());
        when(atribuicoes.findByMes(SETEMBRO.toString())).thenReturn(List.of(atribuicao));
        return servico.sincronizarMes(SETEMBRO).get(0).getValorRealizado();
    }

    private static AdsVenda venda(int dia, double valor) {
        return new AdsVenda(LocalDateTime.of(2025, 9, dia, 0, 0), dia, "VENDA DE MERCADORIA",
                new AdsVenda.AdsFornecedor(OUROFINO), new AdsVenda.AdsCliente("1"),
                new AdsVenda.AdsRepresentante("003", null, "Marye"),
                List.of(new AdsItemVenda(new AdsItemVenda.AdsProduto("1", "X"), new AdsItemVenda.AdsDivisao("1", null), 1,
                        new AdsItemVenda.AdsValoresItem(valor), new AdsItemVenda.AdsPeso(1, 1))));
    }
}
