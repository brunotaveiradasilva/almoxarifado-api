package com.almoxarifado.api.ads;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.almoxarifado.api.meta.Meta;
import com.almoxarifado.api.meta.UnidadeMeta;
import com.almoxarifado.api.metarepresentante.MetaRepresentante;
import com.almoxarifado.api.metarepresentante.MetaRepresentanteRepository;
import com.almoxarifado.api.metarepresentante.Mes;
import com.almoxarifado.api.metarepresentante.TotalVendidoMensal;
import com.almoxarifado.api.metarepresentante.TotalVendidoMensalRepository;
import com.almoxarifado.api.representante.Representante;
import com.almoxarifado.api.representante.RepresentanteRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Recalcula o valorRealizado das metas de um mês a partir do histórico de vendas da ADS desse mês
 * (dia 1 até o fim do mês, ou até hoje no mês atual). Para cada representante com {@link Representante#getCodigoAds()} preenchido,
 * busca o histórico uma vez só e soma, por meta, de um dos dois jeitos:
 *
 * <ul>
 *   <li>{@link Meta#getCnpjAdsFornecedor()} preenchido — soma tudo vendido desse fornecedor
 *   (todas as divisões), pra metas "catch-all" tipo "Geral". Tem prioridade sobre codigoAdsDivisao.</li>
 *   <li>{@link Meta#getCodigoAdsDivisao()} preenchido — soma só os itens cuja divisão bate com um
 *   dos códigos (aceita vários separados por vírgula, ex: "112,113").</li>
 * </ul>
 *
 * Cada um soma quantidade (UNIDADE), peso bruto (KG — confirmado batendo com o valor esperado
 * pelo usuário; peso líquido dava um número menor) ou valor do produto (REAL), considerando o
 * tipo de operação de cada pedido (ver {@link #sinal(AdsVenda)}): venda soma, devolução desconta,
 * bonificação não conta. Metas CLIENTES (positivação) em vez de somar contam quantos clientes
 * diferentes ficaram com saldo positivo em R$ nesses mesmos itens. Representante ou meta sem nenhum
 * dos dois códigos cadastrado fica de fora, sem erro. Nos dois jeitos, itens que batem com
 * {@link Meta#getProdutosExcluidos()} (código do produto ou trecho do nome) não contam.
 *
 * Também grava um {@link TotalVendidoMensal}: a soma de TODO o histórico de vendas do
 * representante no mês (todos os fornecedores/divisões, não só o que está mapeado em alguma meta)
 * — é o número que a tela usa pro card "Total vendido".
 */
@Service
public class AdsSincronizacaoService {

    private static final Logger log = LoggerFactory.getLogger(AdsSincronizacaoService.class);

    /** Folga pra arredondamento: cliente com venda e devolução que se anulam fica com ~0, não positivado. */
    private static final double SALDO_MINIMO_POSITIVADO = 0.01;

    private final MetaRepresentanteRepository metasRepresentante;
    private final TotalVendidoMensalRepository totais;
    private final RepresentanteRepository representantes;
    private final AdsHistoricoVendasClient client;

    public AdsSincronizacaoService(
            MetaRepresentanteRepository metasRepresentante,
            TotalVendidoMensalRepository totais,
            RepresentanteRepository representantes,
            AdsHistoricoVendasClient client) {
        this.metasRepresentante = metasRepresentante;
        this.totais = totais;
        this.representantes = representantes;
        this.client = client;
    }

    /**
     * Todo dia sincroniza o mês atual. Nos primeiros dias do mês também refaz o mês anterior, pra
     * fechar ele com as vendas e devoluções que a ADS ainda lança com atraso.
     */
    @Scheduled(cron = "0 0 6 * * *", zone = "America/Sao_Paulo")
    public void sincronizarAgendado() {
        YearMonth atual = Mes.atual();
        sincronizarMes(atual);
        if (LocalDate.now(Mes.FUSO).getDayOfMonth() <= 5) {
            sincronizarMes(atual.minusMonths(1));
        }
    }

    /**
     * Recalcula e salva o valorRealizado das atribuições do mês cujo representante e meta têm código
     * ADS cadastrado, com as vendas do dia 1 ao último dia do mês (ou até hoje, se for o mês atual).
     * Mês futuro não tem venda: não faz nada.
     */
    public List<MetaRepresentante> sincronizarMes(YearMonth mes) {
        LocalDate hoje = LocalDate.now(Mes.FUSO);
        LocalDate inicio = mes.atDay(1);
        if (inicio.isAfter(hoje)) return List.of();
        LocalDate fim = mes.atEndOfMonth().isAfter(hoje) ? hoje : mes.atEndOfMonth();
        boolean mesAtual = mes.equals(Mes.atual());

        Map<String, List<MetaRepresentante>> porRepresentanteId = metasRepresentante.findByMes(mes.toString()).stream()
                .filter(mv -> temCodigo(mv.getRepresentante().getCodigoAds()) && metaTemCodigo(mv.getMeta()))
                .collect(Collectors.groupingBy(mv -> mv.getRepresentante().getId()));

        List<MetaRepresentante> atualizadas = new ArrayList<>();
        porRepresentanteId.forEach((representanteId, atribuicoes) -> {
            Representante representante = atribuicoes.get(0).getRepresentante();
            try {
                List<AdsVenda> vendas = client.buscarTudo(inicio, fim, representante.getCodigoAds());
                for (MetaRepresentante atribuicao : atribuicoes) {
                    atribuicao.setValorRealizado(somar(vendas, atribuicao.getMeta()));
                    atualizadas.add(metasRepresentante.save(atribuicao));
                }

                double totalVendido = vendas.stream()
                        .mapToDouble(v -> sinal(v) * v.itens().stream()
                                .mapToDouble(item -> item.valores().valorProduto())
                                .sum())
                        .sum();
                TotalVendidoMensal total = totais.findByRepresentanteIdAndMes(representanteId, mes.toString())
                        .orElseGet(() -> new TotalVendidoMensal(representanteId, mes.toString()));
                total.setTotal(totalVendido);
                totais.save(total);

                // Campo antigo, sem mês: continua sendo o total do mês atual, pra quem ainda lê ele.
                if (mesAtual) {
                    representante.setTotalVendidoAds(totalVendido);
                    representantes.save(representante);
                }
            } catch (AdsApiException e) {
                log.warn("Não foi possível sincronizar o representante {} ({}) com a ADS: {}",
                        representante.getNome(), representante.getCodigoAds(), e.getMessage());
            }
        });
        return atualizadas;
    }

    private double somar(List<AdsVenda> vendas, Meta meta) {
        Predicate<AdsItemVenda> daMeta = itemDaMeta(meta);
        List<AdsVenda> doFornecedor = temCodigo(meta.getCnpjAdsFornecedor())
                ? vendas.stream().filter(v -> meta.getCnpjAdsFornecedor().equals(v.fornecedor().cnpj())).toList()
                : vendas;

        if (meta.getUnidade() == UnidadeMeta.CLIENTES) {
            return contarClientesPositivados(doFornecedor, daMeta);
        }
        return doFornecedor.stream()
                .mapToDouble(v -> sinal(v) * v.itens().stream()
                        .filter(daMeta)
                        .mapToDouble(item -> valor(item, meta.getUnidade()))
                        .sum())
                .sum();
    }

    /**
     * Com cnpjAdsFornecedor todo item do pedido conta (o filtro é no pedido); senão, só os das divisões da meta.
     * Nos dois casos, os produtos excluídos da meta ficam de fora.
     */
    private Predicate<AdsItemVenda> itemDaMeta(Meta meta) {
        return itemIncluido(meta).and(itemExcluido(meta).negate());
    }

    private Predicate<AdsItemVenda> itemIncluido(Meta meta) {
        if (temCodigo(meta.getCnpjAdsFornecedor())) return item -> true;
        Set<String> divisoes = separarPorVirgula(meta.getCodigoAdsDivisao());
        return item -> divisoes.contains(item.divisao().id());
    }

    /** Só dígitos é o código do produto na ADS (tem que ser igual); texto é um trecho do nome, sem diferenciar maiúscula. */
    private Predicate<AdsItemVenda> itemExcluido(Meta meta) {
        if (!temCodigo(meta.getProdutosExcluidos())) return item -> false;
        Set<String> excluidos = separarPorVirgula(meta.getProdutosExcluidos().toUpperCase());
        return item -> {
            if (item.produto() == null) return false;
            String descricao = item.produto().descricao() == null ? "" : item.produto().descricao().toUpperCase();
            return excluidos.stream().anyMatch(ex -> ex.chars().allMatch(Character::isDigit)
                    ? ex.equals(item.produto().id())
                    : descricao.contains(ex));
        };
    }

    private Set<String> separarPorVirgula(String texto) {
        return Arrays.stream(texto.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Positivação: quantos clientes diferentes ficaram com saldo positivo em R$ nos itens da meta
     * (vendas menos devoluções, sem bonificação). Quem comprou e devolveu tudo não conta.
     */
    private double contarClientesPositivados(List<AdsVenda> vendas, Predicate<AdsItemVenda> daMeta) {
        Map<String, Double> saldoPorCliente = new HashMap<>();
        for (AdsVenda venda : vendas) {
            if (venda.cliente() == null || venda.cliente().id() == null) continue;
            double valor = sinal(venda) * venda.itens().stream()
                    .filter(daMeta)
                    .mapToDouble(item -> item.valores().valorProduto())
                    .sum();
            saldoPorCliente.merge(venda.cliente().id(), valor, Double::sum);
        }
        return saldoPorCliente.values().stream().filter(saldo -> saldo > SALDO_MINIMO_POSITIVADO).count();
    }

    /**
     * Nem todo pedido da ADS é uma venda de verdade: bonificação (brinde/troca promocional) não
     * conta pro realizado, e devolução desconta o que tinha sido contado antes. Só
     * "VENDA DE MERCADORIA" soma; o que tem "DEV" no nome da operação subtrai; bonificação e
     * qualquer operação não reconhecida ficam de fora (soma zero), pra nunca contar algo errado
     * por engano.
     */
    private int sinal(AdsVenda venda) {
        String operacao = venda.operacao() == null ? "" : venda.operacao().toUpperCase();
        if (operacao.contains("BONIFICA")) return 0;
        if (operacao.contains("DEV")) return -1;
        if (operacao.contains("VENDA")) return 1;
        return 0;
    }

    private double valor(AdsItemVenda item, UnidadeMeta unidade) {
        return switch (unidade) {
            case REAL -> item.valores().valorProduto();
            case KG -> item.peso().bruto();
            case UNIDADE -> item.quantidade();
            case CLIENTES -> throw new IllegalArgumentException("CLIENTES é contado por cliente, não somado por item");
        };
    }

    private boolean metaTemCodigo(Meta meta) {
        return temCodigo(meta.getCnpjAdsFornecedor()) || temCodigo(meta.getCodigoAdsDivisao());
    }

    private boolean temCodigo(String codigo) {
        return codigo != null && !codigo.isBlank();
    }
}
