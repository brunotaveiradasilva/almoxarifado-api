package com.almoxarifado.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

// UserDetailsServiceAutoConfiguration fica excluída porque a autenticação é toda manual (JWT,
// ver auth/JwtAuthFilter): sem isso, o Spring cria um usuário padrão com senha aleatória que
// nunca é usado, só polui o log de inicialização.
// EnableScheduling liga o job diário que sincroniza o realizado das metas com a ADS (ver
// ads/AdsSincronizacaoService).
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
public class AlmoxarifadoApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlmoxarifadoApiApplication.class, args);
    }
}
