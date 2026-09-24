package com.almoxarifado.api.ads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.almoxarifado.api.fornecedor.Fornecedor;
import com.almoxarifado.api.meta.Meta;
import com.almoxarifado.api.meta.MetaRepository;
import com.almoxarifado.api.meta.UnidadeMeta;
import com.almoxarifado.api.metarepresentante.Mes;
import com.almoxarifado.api.metarepresentante.MetaRepresentante;
import com.almoxarifado.api.metarepresentante.MetaRepresentanteRepository;
import com.almoxarifado.api.metarepresentante.TotalVendidoMensal;
import com.almoxarifado.api.metarepresentante.TotalVendidoMensalRepository;
import com.almoxarifado.api.representante.Representante;
import com.almoxarifado.api.representante.RepresentanteRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Representante sem meta no mês também tem o realizado e o total vendido calculados. */
class RealizadoSemMetaTests {

    static final String OUROFINO = "57511234000148";

    MetaRepresentanteRepository atribuicoes = mock(MetaRepresentanteRepository.class);
    TotalVendidoMensalRepository totais = mock(TotalVendidoMensalRepository.class);
    RepresentanteRepository representantes = mock(RepresentanteRepository.class);
    MetaRepository metas = mock(MetaRepository.class);
    AdsHistoricoVendasClient client = mock(AdsHistoricoVendasClient.class);
    AdsSincronizacaoService servico = new AdsSincronizacaoService(atribuicoes, totais, representantes, metas, client);

    Fornecedor ourofino = fornecedor("f-1");
    Fornecedor outro = fornecedor("f-2");
    Representante marye;
    Meta geral = meta("m-1", ourofino);

    @BeforeEach
    void preparar() {
        marye = new Representante();
        marye.setId("rep-1");
        marye.setNome("Marye");
        marye.setCodigoAds("003");
        marye.setFornecedores(Set.of(ourofino));
        when(representantes.findAll()).thenReturn(List.of(marye));
        when(metas.findAll()).thenReturn(List.of(geral, meta("m-2", outro)));
        when(atribuicoes.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(totais.findByRepresentanteIdAndMes(anyString(), anyString())).thenReturn(Optional.empty());
        when(client.buscarTudo(any(), any(), anyString())).thenReturn(List.of(new AdsVenda(null, 1, "VENDA DE MERCADORIA",
                new AdsVenda.AdsFornecedor(OUROFINO), new AdsVenda.AdsCliente("1"),
                new AdsVenda.AdsRepresentante("003", null, "Marye"),
                List.of(new AdsItemVenda(new AdsItemVenda.AdsProduto("1", "X"), new AdsItemVenda.AdsDivisao("1", null), 1,
                        new AdsItemVenda.AdsValoresItem(150), new AdsItemVenda.AdsPeso(1, 1))))));
    }

    @Test
    void criaLinhaSemMetaSoPraMetasDosFornecedoresDele() {
        List<MetaRepresentante> atualizadas = servico.sincronizarMes(Mes.atual());

        assertThat(atualizadas).hasSize(1);
        MetaRepresentante linha = atualizadas.get(0);
        assertThat(linha.getMeta()).isSameAs(geral);
        assertThat(linha.getValorMeta()).isZero();
        assertThat(linha.getValorRealizado()).isEqualTo(150);
        assertThat(linha.getMes()).isEqualTo(Mes.atual().toString());
    }

    @Test
    void gravaTotalVendidoMesmoSemMeta() {
        servico.sincronizarMes(Mes.atual());

        ArgumentCaptor<TotalVendidoMensal> total = ArgumentCaptor.forClass(TotalVendidoMensal.class);
        verify(totais).save(total.capture());
        assertThat(total.getValue().getTotal()).isEqualTo(150);
    }

    @Test
    void naoDuplicaQuandoJaTemMeta() {
        MetaRepresentante existente = new MetaRepresentante();
        existente.setRepresentante(marye);
        existente.setMeta(geral);
        existente.setMes(Mes.atual().toString());
        existente.setValorMeta(1000);
        when(atribuicoes.findByMes(Mes.atual().toString())).thenReturn(List.of(existente));

        List<MetaRepresentante> atualizadas = servico.sincronizarMes(Mes.atual());

        assertThat(atualizadas).containsExactly(existente);
        assertThat(existente.getValorMeta()).isEqualTo(1000);
        assertThat(existente.getValorRealizado()).isEqualTo(150);
    }

    private static Fornecedor fornecedor(String id) {
        Fornecedor f = new Fornecedor();
        f.setId(id);
        return f;
    }

    private static Meta meta(String id, Fornecedor fornecedor) {
        Meta meta = new Meta();
        meta.setId(id);
        meta.setFornecedor(fornecedor);
        meta.setUnidade(UnidadeMeta.REAL);
        meta.setCnpjAdsFornecedor(OUROFINO);
        return meta;
    }
}
