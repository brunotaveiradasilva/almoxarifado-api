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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

/**
 * Vendas de um período qualquer direto do histórico da ADS, sem depender do que já foi sincronizado
 * nas metas — é o que deixa a aba Dados comparar com o ano passado, antes mesmo de existir meta.
 *
 * Só olha os representantes do cadastro que têm {@link Representante#getCodigoAds()}: busca o
 * histórico de cada um filtrado por {@code repr_id} (o mesmo jeito da sincronização das metas),
 * alguns ao mesmo tempo. Assim cada linha já é o representante do cadastro, e não precisa varrer a
 * ADS inteira. Com fornecedor escolhido, só contam os itens que as metas desse fornecedor
 * reconhecem: pedidos do CNPJ ou itens das divisões cadastradas nas metas dele (produtos
 * incluídos/excluídos de cada meta não entram aqui — é o total do fornecedor, não de uma meta).
 */
@Service
public class AdsVendasPeriodoService {

    /** Folga pra arredondamento, igual à positivação das metas. */
    private static final double SALDO_MINIMO_POSITIVADO = 0.01;

    /** Quantos representantes buscar na ADS ao mesmo tempo — sem exagerar pra ela não recusar. */
    private static final int BUSCAS_SIMULTANEAS = 4;

    /** Período que terminou antes disso ainda recebe devolução lançada com atraso na ADS: não guarda. */
    private static final int DIAS_ATE_FECHAR = 5;
    private static final Duration VALIDADE_CACHE = Duration.ofHours(6);
    /** Um item por representante e período: dá umas oito comparações da equipe toda. */
    private static final int MAX_CACHE = 160;

    private final AdsHistoricoVendasClient client;
    private final RepresentanteRepository representantes;
    private final MetaRepository metas;
    private final ExecutorService buscas = Executors.newFixedThreadPool(BUSCAS_SIMULTANEAS);

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

    @PreDestroy
    void encerrar() {
        buscas.shutdownNow();
    }

    /** {@code representanteId} vazio busca todos os cadastrados com código ADS; {@code fornecedorId} vazio soma todos os fornecedores. */
    public VendasPeriodo buscar(LocalDate inicio, LocalDate fim, String representanteId, String fornecedorId) {
        List<Representante> alvos = representantes.findAll().stream()
                .filter(r -> temCodigo(r.getCodigoAds()))
                .filter(r -> !temCodigo(representanteId) || representanteId.equals(r.getId()))
                .toList();
        if (alvos.isEmpty()) {
            throw new ConsultaInvalidaException(temCodigo(representanteId)
                    ? "Esse representante não tem código da ADS no cadastro"
                    : "Nenhum representante tem código da ADS no cadastro");
        }

        Filtro filtro = filtroDoFornecedor(fornecedorId);
        Map<Representante, List<AdsVenda>> vendas = buscarTodos(inicio, fim, alvos);
        return agrupar(inicio, fim, vendas, filtro);
    }

    /** Busca os representantes em paralelo; se a ADS falhar em qualquer um, a consulta toda falha (número pela metade engana). */
    private Map<Representante, List<AdsVenda>> buscarTodos(LocalDate inicio, LocalDate fim, List<Representante> alvos) {
        Map<Representante, CompletableFuture<List<AdsVenda>>> pendentes = new LinkedHashMap<>();
        for (Representante r : alvos) {
            pendentes.put(r, CompletableFuture.supplyAsync(() -> vendas(inicio, fim, r.getCodigoAds().trim()), buscas));
        }
        Map<Representante, List<AdsVenda>> vendas = new LinkedHashMap<>();
        try {
            pendentes.forEach((r, futuro) -> vendas.put(r, futuro.join()));
        } catch (CompletionException e) {
            pendentes.values().forEach(f -> f.cancel(true));
            if (e.getCause() instanceof RuntimeException causa) throw causa;
            throw e;
        }
        return vendas;
    }

    /** Sem fornecedor, tudo conta. Com fornecedor, os CNPJs e divisões das metas dele. */
    private Filtro filtroDoFornecedor(String fornecedorId) {
        if (!temCodigo(fornecedorId)) return Filtro.TUDO;

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
        return new Filtro(cnpjs, divisoes);
    }

    /** Uma linha por representante (mesmo sem venda); o total conta cada cliente uma vez só. */
    private VendasPeriodo agrupar(LocalDate inicio, LocalDate fim, Map<Representante, List<AdsVenda>> vendasPorRepresentante, Filtro filtro) {
        List<VendasRepresentante> linhas = new ArrayList<>();
        Acumulado total = new Acumulado();
        vendasPorRepresentante.forEach((representante, vendas) -> {
            Acumulado doRepresentante = new Acumulado();
            for (AdsVenda venda : vendas) {
                int sinal = AdsSincronizacaoService.sinal(venda);
                if (sinal == 0) continue;
                boolean pedidoInteiro = filtro.cnpjs() == null
                        || (venda.fornecedor() != null && filtro.cnpjs().contains(venda.fornecedor().cnpj()));

                double valor = 0;
                double kg = 0;
                for (AdsItemVenda item : venda.itens()) {
                    if (!pedidoInteiro && (item.divisao() == null || !filtro.divisoes().contains(item.divisao().id()))) continue;
                    valor += item.valores().valorProduto();
                    kg += item.peso() == null ? 0 : item.peso().bruto();
                }
                if (valor == 0 && kg == 0) continue;

                String cliente = venda.cliente() == null ? null : venda.cliente().id();
                doRepresentante.somar(sinal * valor, sinal * kg, cliente);
                total.somar(sinal * valor, sinal * kg, cliente);
            }
            linhas.add(new VendasRepresentante(representante.getCodigoAds().trim(), representante.getId(),
                    representante.getNome(), doRepresentante.valores()));
        });
        linhas.sort(Comparator.comparingDouble((VendasRepresentante l) -> l.valores().valor()).reversed());
        return new VendasPeriodo(inicio, fim, linhas, total.valores());
    }

    private List<AdsVenda> vendas(LocalDate inicio, LocalDate fim, String codigoAds) {
        boolean fechado = fim.isBefore(LocalDate.now(Mes.FUSO).minusDays(DIAS_ATE_FECHAR));
        String chave = inicio + "/" + fim + "/" + codigoAds;
        if (fechado) {
            synchronized (cache) {
                CacheItem guardado = cache.get(chave);
                if (guardado != null && guardado.em().plus(VALIDADE_CACHE).isAfter(Instant.now())) return guardado.vendas();
            }
        }
        List<AdsVenda> vendas = client.buscarTudo(inicio, fim, codigoAds);
        if (fechado) {
            synchronized (cache) {
                cache.put(chave, new CacheItem(vendas, Instant.now()));
            }
        }
        return vendas;
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

    /** CNPJs cujo pedido conta inteiro e divisões cujos itens contam nos outros pedidos; {@code cnpjs} nulo = sem filtro. */
    private record Filtro(Set<String> cnpjs, Set<String> divisoes) {
        static final Filtro TUDO = new Filtro(null, null);
    }

    /** Somas de um representante (ou do total) enquanto percorre os pedidos. */
    private static final class Acumulado {
        double valor;
        double kg;
        final Map<String, Double> saldoPorCliente = new HashMap<>();

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
