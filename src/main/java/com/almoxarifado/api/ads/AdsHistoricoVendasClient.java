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
 * `especificoId` é obrigatório pra ADS mas não documentado na spec da API — quem chamar precisa
 * confirmar com o suporte da ADS o que ele espera aí antes de usar isto de verdade (ver README).
 */
@Service
public class AdsHistoricoVendasClient {

    private static final int MAX_PAGINAS = 1000;

    private final RestClient restClient;
    private final AdsTokenService tokenService;
    private final String cnpjDistribuidora;

    public AdsHistoricoVendasClient(
            AdsTokenService tokenService,
            @Value("${app.ads.base-url}") String baseUrl,
            @Value("${app.ads.cnpj-distribuidora}") String cnpjDistribuidora) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.tokenService = tokenService;
        this.cnpjDistribuidora = cnpjDistribuidora;
    }

    /**
     * Busca todas as páginas do período informado e devolve a lista completa de vendas.
     * `dtInicio`/`dtFinal` são opcionais (a ADS aceita omitir); `reprId` filtra por representante.
     */
    public List<AdsVenda> buscarTudo(String especificoId, LocalDate dtInicio, LocalDate dtFinal, String reprId) {
        List<AdsVenda> todas = new ArrayList<>();
        int pagina = 1;
        AdsHistoricoVendasPagina resultado;
        do {
            resultado = buscarPagina(especificoId, dtInicio, dtFinal, reprId, pagina, 100);
            todas.addAll(resultado.items());
            pagina++;
        } while (resultado.hasNext() && pagina <= MAX_PAGINAS);
        return todas;
    }

    public AdsHistoricoVendasPagina buscarPagina(
            String especificoId, LocalDate dtInicio, LocalDate dtFinal, String reprId, int page, int pageSize) {
        return chamar(especificoId, dtInicio, dtFinal, reprId, page, pageSize, true);
    }

    private AdsHistoricoVendasPagina chamar(
            String especificoId, LocalDate dtInicio, LocalDate dtFinal, String reprId,
            int page, int pageSize, boolean tentarDeNovoSe401) {
        String token = tokenService.obterToken();
        try {
            return restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/api/v1/{cnpjDistribuidora}/historico-de-vendas")
                                .queryParam("especificoid", especificoId)
                                .queryParam("page", page)
                                .queryParam("pageSize", pageSize);
                        if (dtInicio != null) uriBuilder.queryParam("dtinicio", dtInicio.toString());
                        if (dtFinal != null) uriBuilder.queryParam("dtfinal", dtFinal.toString());
                        if (reprId != null && !reprId.isBlank()) uriBuilder.queryParam("repr_id", reprId);
                        return uriBuilder.build(cnpjDistribuidora);
                    })
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                        if (resp.getStatusCode().value() == 401) {
                            tokenService.invalidar();
                        }
                        throw new AdsApiException(
                                "API da ADS respondeu " + resp.getStatusCode() + " para " + req.getURI());
                    })
                    .body(AdsHistoricoVendasPagina.class);
        } catch (AdsApiException e) {
            if (tentarDeNovoSe401 && e.getMessage().contains("401")) {
                return chamar(especificoId, dtInicio, dtFinal, reprId, page, pageSize, false);
            }
            throw e;
        } catch (RestClientException e) {
            throw new AdsApiException("Não foi possível falar com a API da ADS: " + e.getMessage(), e);
        }
    }
}
