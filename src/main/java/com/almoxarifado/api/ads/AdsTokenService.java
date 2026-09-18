package com.almoxarifado.api.ads;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Faz login na API da ADS e guarda o token em memória, renovando sozinho.
 *
 * A doc da ADS não explica o formato de `expiry` (segundos? milissegundos? epoch de quê?), então,
 * em vez de confiar nele, renova de forma conservadora a cada {@link #RENOVAR_APOS} e também
 * de novo sempre que a ADS devolver 401 com o token atual (ver {@link #invalidar()}).
 */
@Service
public class AdsTokenService {

    private static final Duration RENOVAR_APOS = Duration.ofMinutes(15);

    private final RestClient restClient;
    private final String email;
    private final String senha;

    private volatile String token;
    private volatile Instant obtidoEm;

    public AdsTokenService(
            @Value("${app.ads.base-url}") String baseUrl,
            @Value("${app.ads.email}") String email,
            @Value("${app.ads.senha}") String senha) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.email = email;
        this.senha = senha;
    }

    /** Token válido pra usar no header Authorization — loga de novo se ainda não tem um ou se já passou da renovação. */
    public synchronized String obterToken() {
        if (token == null || Duration.between(obtidoEm, Instant.now()).compareTo(RENOVAR_APOS) >= 0) {
            login();
        }
        return token;
    }

    /** Descarta o token em cache, forçando login de novo na próxima chamada (ex: a ADS respondeu 401 com o token atual). */
    public synchronized void invalidar() {
        token = null;
    }

    private void login() {
        if (email == null || email.isBlank() || senha == null || senha.isBlank()) {
            throw new AdsApiException("ADS_EMAIL / ADS_SENHA não configurados");
        }

        AdsLoginResponse resposta;
        try {
            resposta = restClient.post()
                    .uri("/api/v1/login")
                    .body(new AdsLoginRequest(email, senha))
                    .retrieve()
                    .body(AdsLoginResponse.class);
        } catch (RestClientException e) {
            throw new AdsApiException("Não foi possível fazer login na API da ADS: " + e.getMessage(), e);
        }

        if (resposta == null || resposta.accessToken() == null || resposta.accessToken().isBlank()) {
            throw new AdsApiException("Login na API da ADS não devolveu um token");
        }

        token = resposta.accessToken();
        obtidoEm = Instant.now();
    }
}
