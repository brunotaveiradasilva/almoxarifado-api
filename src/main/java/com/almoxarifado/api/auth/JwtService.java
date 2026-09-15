package com.almoxarifado.api.auth;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Emite e valida tokens JWT (HS256) sem depender de biblioteca externa: só
 * HMAC-SHA256 assinando header.payload em base64url, do jeito que qualquer
 * decodificador de JWT padrão entende.
 */
@Component
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private static final String CABECALHO = encodeJson(Map.of("alg", "HS256", "typ", "JWT"));

    private final byte[] chave;
    private final long validadeSegundos;

    public JwtService(
            @Value("${app.jwt.secret}") String segredo,
            @Value("${app.jwt.validade-horas:168}") long validadeHoras) {
        if (segredo.getBytes(StandardCharsets.UTF_8).length < 32) {
            log.warn("JWT_SECRET fraco ou usando o valor padrão de desenvolvimento. "
                    + "Defina um JWT_SECRET forte e único antes de expor esta API na internet.");
        }
        this.chave = segredo.getBytes(StandardCharsets.UTF_8);
        this.validadeSegundos = validadeHoras * 3600;
    }

    public String gerar(String usuario, Role role) {
        long agora = Instant.now().getEpochSecond();
        String payload = encodeJson(Map.of(
                "sub", usuario, "role", role.name(), "iat", agora, "exp", agora + validadeSegundos));
        String semAssinatura = CABECALHO + "." + payload;
        return semAssinatura + "." + assinar(semAssinatura);
    }

    /** Usuário e papel do token, se a assinatura bater e o token ainda não tiver expirado. */
    public Optional<Sessao> validarEExtrairSessao(String token) {
        try {
            String[] partes = token.split("\\.");
            if (partes.length != 3) return Optional.empty();

            String semAssinatura = partes[0] + "." + partes[1];
            if (!constantTimeEquals(assinar(semAssinatura), partes[2])) return Optional.empty();

            Map<?, ?> payload = JSON.readValue(DECODER.decode(partes[1]), Map.class);
            long exp = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() > exp) return Optional.empty();

            String usuario = (String) payload.get("sub");
            if (usuario == null) return Optional.empty();

            // Tokens emitidos antes do campo "role" existir: trata como USUARIO comum.
            Object roleBruta = payload.get("role");
            Role role = roleBruta == null ? Role.USUARIO : Role.valueOf((String) roleBruta);

            return Optional.of(new Sessao(usuario, role));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** Identidade extraída de um token já validado. */
    public record Sessao(String usuario, Role role) {
    }

    private String assinar(String dado) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(chave, "HmacSHA256"));
            return ENCODER.encodeToString(mac.doFinal(dado.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static String encodeJson(Map<String, ?> valores) {
        try {
            return ENCODER.encodeToString(JSON.writeValueAsBytes(valores));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
