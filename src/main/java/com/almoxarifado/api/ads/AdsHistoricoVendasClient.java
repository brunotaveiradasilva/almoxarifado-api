package com.almoxarifado.api.ads;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Consulta o histórico de vendas da ADS (`GET /api/v1/{cnpjDistribuidora}/historico-de-vendas`).
 *
 * Autentica só com os headers `x-api-key` e `User-Agent` — confirmado testando direto contra a
 * API de produção (`adsapi.com.br`; o ambiente de homologação usado pela doc não aceita essa
 * autenticação, e a doc/spec não documenta esses headers). `dtinicio`/`dtfinal` são obrigatórios
 * na prática, mesmo a doc marcando como opcionais (sem eles a API responde 400). `especificoid` é
 * um valor fixo da conta (não um filtro — outros valores dão 400), por isso vem só da config,
 * nunca de quem chama.
 */
@Service
public class AdsHistoricoVendasClient {

    private static final int MAX_PAGINAS = 1000;

    private final RestClient restClient;
    private final String cnpjDistribuidora;
    private final String especificoId;

    public AdsHistoricoVendasClient(
            @Value("${app.ads.base-url}") String baseUrl,
            @Value("${app.ads.api-key}") String apiKey,
            @Value("${app.ads.user-agent}") String userAgent,
            @Value("${app.ads.cnpj-distribuidora}") String cnpjDistribuidora,
            @Value("${app.ads.especifico-id}") String especificoId) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("User-Agent", userAgent)
                .build();
        this.cnpjDistribuidora = cnpjDistribuidora;
        this.especificoId = especificoId;
    }

    /**
     * Busca todas as páginas do período informado e devolve a lista completa de vendas.
     * `reprId` filtra por representante (opcional — vazio busca de todos).
     */
    public List<AdsVenda> buscarTudo(LocalDate dtInicio, LocalDate dtFinal, String reprId) {
        List<AdsVenda> todas = new ArrayList<>();
        int pagina = 1;
        AdsHistoricoVendasPagina resultado;
        do {
            resultado = buscarPagina(dtInicio, dtFinal, reprId, pagina, 100);
            todas.addAll(resultado.items());
            pagina++;
        } while (resultado.hasNext() && pagina <= MAX_PAGINAS);
        return todas;
    }

    public AdsHistoricoVendasPagina buscarPagina(LocalDate dtInicio, LocalDate dtFinal, String reprId, int page, int pageSize) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/api/v1/{cnpjDistribuidora}/historico-de-vendas")
                                .queryParam("especificoid", especificoId)
                                .queryParam("dtinicio", dtInicio)
                                .queryParam("dtfinal", dtFinal)
                                .queryParam("page", page)
                                .queryParam("pageSize", pageSize);
                        if (reprId != null && !reprId.isBlank()) uriBuilder.queryParam("repr_id", reprId);
                        return uriBuilder.build(cnpjDistribuidora);
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        throw new AdsApiException("API da ADS respondeu " + resp.getStatusCode() + " para " + req.getURI());
                    })
                    .body(AdsHistoricoVendasPagina.class);
        } catch (AdsApiException e) {
            throw e;
        } catch (RestClientException e) {
            throw new AdsApiException("Não foi possível falar com a API da ADS: " + e.getMessage(), e);
        }
    }
}
