package com.almoxarifado.api.ads;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.almoxarifado.api.dados.ConsultaInvalidaException;
import com.almoxarifado.api.dados.VendasPeriodo;
import com.almoxarifado.api.dados.VendasPeriodo.Valores;
import com.almoxarifado.api.dados.VendasPeriodo.VendasRepresentante;
import com.almoxarifado.api.meta.Meta;
import com.almoxarifado.api.meta.MetaRepository;
import com.almoxarifado.api.metarepresentante.Mes;
import com.almoxarifado.api.representante.Representante;
import com.almoxarifado.api.representante.RepresentanteRepository;

import org.springframework.stereotype.Service;

/**
 * Vendas de um período qualquer direto do histórico da ADS, sem depender do que já foi sincronizado
 * nas metas — é o que deixa a aba Dados comparar com o ano passado, antes mesmo de existir meta.
 *
 * Busca o histórico de todos os representantes numa varredura só e agrupa pelo representante do
 * pedido. Com fornecedor escolhido, só contam os itens que as metas desse fornecedor reconhecem: pedidos
 * do CNPJ ou itens das divisões cadastradas nas metas dele (produtos incluídos/excluídos de cada meta
 * não entram aqui — é o total do fornecedor, não de uma meta).
 */
@Service
public class AdsVendasPeriodoService {

    /** Folga pra arredondamento, igual à positivação das metas. */
    private static final double SALDO_MINIMO_POSITIVADO = 0.01;

    /** Período que terminou antes disso ainda recebe devolução lançada com atraso na ADS: não guarda. */
    private static final int DIAS_ATE_FECHAR = 5;
    private static final Duration VALIDADE_CACHE = Duration.ofHours(6);
    /** Cada item pode ser um ano inteiro de pedidos: poucos, pra não pesar na memória. */
    private static final int MAX_CACHE = 8;

    private final AdsHistoricoVendasClient client;
    private final RepresentanteRepository representantes;
    private final MetaRepository metas;

    /** Varrer um ano inteiro na ADS demora; períodos já fechados ficam guardados um tempo. */
    private final Map<String, CacheItem> cache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CacheItem> maisAntigo) {
            return size() > MAX_CACHE;
        }
    };

    private record CacheItem(List<AdsVenda> vendas, Instant em) {
    }

    public AdsVendasPeriodoService(AdsHistoricoVendasClient client, RepresentanteRepository representantes, MetaRepository metas) {
        this.client = client;
        this.representantes = representantes;
        this.metas = metas;
    }

    /** {@code fornecedorId} nulo/vazio soma tudo, de todos os fornecedores. */
    public VendasPeriodo buscar(LocalDate inicio, LocalDate fim, String fornecedorId) {
        if (!temCodigo(fornecedorId)) return agrupar(inicio, fim, vendas(inicio, fim), null, null);

        List<Meta> doFornecedor = metas.findAll().stream()
                .filter(m -> m.getFornecedor() != null && fornecedorId.equals(m.getFornecedor().getId()))
                .toList();
        Set<String> cnpjs = codigos(doFornecedor.stream().map(Meta::getCnpjAdsFornecedor).toList());
        // Divisão de meta que já tem CNPJ é ignorada na sincronização também (CNPJ tem prioridade).
        Set<String> divisoes = codigos(doFornecedor.stream()
                .filter(m -> !temCodigo(m.getCnpjAdsFornecedor()))
                .map(Meta::getCodigoAdsDivisao)
                .toList());
        if (cnpjs.isEmpty() && divisoes.isEmpty()) {
            throw new ConsultaInvalidaException(
                    "Esse fornecedor não tem CNPJ nem divisão da ADS em nenhuma meta — cadastre numa meta dele pra poder filtrar");
        }
        return agrupar(inicio, fim, vendas(inicio, fim), cnpjs, divisoes);
    }

    /**
     * Agrupa por representante do pedido. Com {@code cnpjs}/{@code divisoes} nulos conta tudo; senão
     * só pedidos de um dos CNPJs e, dos outros pedidos, itens de uma das divisões.
     */
    VendasPeriodo agrupar(LocalDate inicio, LocalDate fim, List<AdsVenda> vendas, Set<String> cnpjs, Set<String> divisoes) {
        boolean filtra = cnpjs != null;
        Map<String, Representante> cadastradosPorCodigo = new HashMap<>();
        for (Representante r : representantes.findAll()) {
            if (temCodigo(r.getCodigoAds())) cadastradosPorCodigo.put(normalizar(r.getCodigoAds()), r);
        }

        Map<String, Acumulado> porRepresentante = new LinkedHashMap<>();
        Acumulado total = new Acumulado(null);
        for (AdsVenda venda : vendas) {
            int sinal = AdsSincronizacaoService.sinal(venda);
            if (sinal == 0) continue;
            boolean pedidoInteiro = !filtra || (venda.fornecedor() != null && cnpjs.contains(venda.fornecedor().cnpj()));

            double valor = 0;
            double kg = 0;
            for (AdsItemVenda item : venda.itens()) {
                if (!pedidoInteiro && (item.divisao() == null || !divisoes.contains(item.divisao().id()))) continue;
                valor += item.valores().valorProduto();
                kg += item.peso() == null ? 0 : item.peso().bruto();
            }
            if (valor == 0 && kg == 0) continue;

            String codigo = venda.representante() == null || !temCodigo(venda.representante().id())
                    ? ""
                    : normalizar(venda.representante().id());
            String nomeAds = venda.representante() == null ? null : venda.representante().nome();
            Acumulado doRepresentante = porRepresentante.computeIfAbsent(codigo, c -> new Acumulado(nomeAds));
            String cliente = venda.cliente() == null ? null : venda.cliente().id();
            doRepresentante.somar(sinal * valor, sinal * kg, cliente);
            total.somar(sinal * valor, sinal * kg, cliente);
        }

        List<VendasRepresentante> linhas = new ArrayList<>();
        porRepresentante.forEach((codigo, acumulado) -> {
            Representante cadastrado = cadastradosPorCodigo.get(codigo);
            String nome = cadastrado != null ? cadastrado.getNome()
                    : codigo.isEmpty() ? "Sem representante"
                    : acumulado.nomeAds != null && !acumulado.nomeAds.isBlank() ? acumulado.nomeAds.trim()
                    : "Representante " + codigo;
            linhas.add(new VendasRepresentante(codigo, cadastrado == null ? null : cadastrado.getId(), nome, acumulado.valores()));
        });
        linhas.sort(Comparator.comparingDouble((VendasRepresentante l) -> l.valores().valor()).reversed());
        return new VendasPeriodo(inicio, fim, linhas, total.valores());
    }

    private List<AdsVenda> vendas(LocalDate inicio, LocalDate fim) {
        boolean fechado = fim.isBefore(LocalDate.now(Mes.FUSO).minusDays(DIAS_ATE_FECHAR));
        String chave = inicio + "/" + fim;
        if (fechado) {
            synchronized (cache) {
                CacheItem guardado = cache.get(chave);
                if (guardado != null && guardado.em().plus(VALIDADE_CACHE).isAfter(Instant.now())) return guardado.vendas();
            }
        }
        List<AdsVenda> vendas = client.buscarTudo(inicio, fim, null);
        if (fechado) {
            synchronized (cache) {
                cache.put(chave, new CacheItem(vendas, Instant.now()));
            }
        }
        return vendas;
    }

    /** A ADS pode mandar "3" onde o cadastro tem "003": compara sem os zeros à esquerda. */
    static String normalizar(String codigo) {
        return codigo.trim().replaceFirst("^0+(?=.)", "");
    }

    private static Set<String> codigos(List<String> listas) {
        return listas.stream()
                .filter(AdsVendasPeriodoService::temCodigo)
                .flatMap(lista -> List.of(lista.split(",")).stream())
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private static boolean temCodigo(String codigo) {
        return codigo != null && !codigo.isBlank();
    }

    /** Somas de um representante (ou do total) enquanto percorre os pedidos. */
    private static final class Acumulado {
        final String nomeAds;
        double valor;
        double kg;
        final Map<String, Double> saldoPorCliente = new HashMap<>();

        Acumulado(String nomeAds) {
            this.nomeAds = nomeAds;
        }

        void somar(double valor, double kg, String cliente) {
            this.valor += valor;
            this.kg += kg;
            if (cliente != null) saldoPorCliente.merge(cliente, valor, Double::sum);
        }

        Valores valores() {
            long clientes = saldoPorCliente.values().stream().filter(s -> s > SALDO_MINIMO_POSITIVADO).count();
            return new Valores(valor, kg, clientes);
        }
    }
}
