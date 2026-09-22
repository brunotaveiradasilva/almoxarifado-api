package com.almoxarifado.api.ads;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.almoxarifado.api.meta.Meta;
import com.almoxarifado.api.meta.UnidadeMeta;
import com.almoxarifado.api.metarepresentante.MetaRepresentante;
import com.almoxarifado.api.metarepresentante.MetaRepresentanteRepository;
import com.almoxarifado.api.representante.Representante;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Recalcula o valorRealizado das metas a partir do histórico de vendas da ADS do mês corrente
 * (dia 1 até hoje). Para cada representante com {@link Representante#getCodigoAds()} preenchido,
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
 * pelo usuário; peso líquido dava um número menor) ou valor do produto (REAL). Representante
 * ou meta sem nenhum dos dois códigos cadastrado fica de fora, sem erro.
 */
@Service
public class AdsSincronizacaoService {

    private static final Logger log = LoggerFactory.getLogger(AdsSincronizacaoService.class);

    private final MetaRepresentanteRepository metasRepresentante;
    private final AdsHistoricoVendasClient client;

    public AdsSincronizacaoService(MetaRepresentanteRepository metasRepresentante, AdsHistoricoVendasClient client) {
        this.metasRepresentante = metasRepresentante;
        this.client = client;
    }

    @Scheduled(cron = "0 0 6 * * *")
    public void sincronizarAgendado() {
        sincronizarTudo();
    }

    /** Recalcula e salva o valorRealizado de toda atribuição cujo representante e meta têm código ADS cadastrado. */
    public List<MetaRepresentante> sincronizarTudo() {
        LocalDate inicio = LocalDate.now().withDayOfMonth(1);
        LocalDate fim = LocalDate.now();

        Map<String, List<MetaRepresentante>> porRepresentanteId = metasRepresentante.findAll().stream()
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
            } catch (AdsApiException e) {
                log.warn("Não foi possível sincronizar o representante {} ({}) com a ADS: {}",
                        representante.getNome(), representante.getCodigoAds(), e.getMessage());
            }
        });
        return atualizadas;
    }

    private double somar(List<AdsVenda> vendas, Meta meta) {
        if (temCodigo(meta.getCnpjAdsFornecedor())) {
            return vendas.stream()
                    .filter(v -> meta.getCnpjAdsFornecedor().equals(v.fornecedor().cnpj()))
                    .flatMap(v -> v.itens().stream())
                    .mapToDouble(item -> valor(item, meta.getUnidade()))
                    .sum();
        }

        Set<String> divisoes = Set.of(meta.getCodigoAdsDivisao().split(",")).stream()
                .map(String::trim)
                .collect(Collectors.toSet());
        return vendas.stream()
                .flatMap(v -> v.itens().stream())
                .filter(item -> divisoes.contains(item.divisao().id()))
                .mapToDouble(item -> valor(item, meta.getUnidade()))
                .sum();
    }

    private double valor(AdsItemVenda item, UnidadeMeta unidade) {
        return switch (unidade) {
            case REAL -> item.valores().valorProduto();
            case KG -> item.peso().bruto();
            case UNIDADE -> item.quantidade();
        };
    }

    private boolean metaTemCodigo(Meta meta) {
        return temCodigo(meta.getCnpjAdsFornecedor()) || temCodigo(meta.getCodigoAdsDivisao());
    }

    private boolean temCodigo(String codigo) {
        return codigo != null && !codigo.isBlank();
    }
}
