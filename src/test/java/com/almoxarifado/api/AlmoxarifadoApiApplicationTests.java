package com.almoxarifado.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Sobe o contexto do Spring com um H2 em memória, só para garantir que a
 * aplicação inicializa sem precisar de um MySQL rodando (útil em CI).
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:almoxarifado;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AlmoxarifadoApiApplicationTests {

    @Test
    void contextLoads() {
    }
}
