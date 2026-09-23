package com.almoxarifado.api.metarepresentante;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Migração de uma vez só: as metas de representante eram guardadas sem mês, na tabela
 * metas_representante. Agora cada mês tem seu valor, na metas_representante_mensal (que o Hibernate
 * cria sozinho com o ddl-auto). Ao subir, se a tabela antiga ainda existir:
 *
 * <ol>
 *   <li>copia tudo pra tabela nova, marcado como o mês atual (só se a nova ainda estiver vazia);</li>
 *   <li>apaga a antiga, quando a nova já tiver todas as linhas — senão as chaves estrangeiras dela
 *   impediriam excluir representantes e metas.</li>
 * </ol>
 *
 * Depois da primeira vez a tabela antiga não existe mais e isso não faz nada. Qualquer erro só vai
 * pro log: a API sobe do mesmo jeito.
 */
@Component
public class MigracaoMetasPorMes implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracaoMetasPorMes.class);

    static final String TABELA_ANTIGA = "metas_representante";
    static final String TABELA_NOVA = "metas_representante_mensal";

    private final JdbcTemplate jdbc;

    public MigracaoMetasPorMes(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer antigas;
        try {
            antigas = jdbc.queryForObject("SELECT COUNT(*) FROM " + TABELA_ANTIGA, Integer.class);
        } catch (DataAccessException e) {
            return; // tabela antiga não existe: já migrado (ou banco novo)
        }

        try {
            String mes = Mes.atual().toString();
            if (contarNovas() == 0 && antigas > 0) {
                int copiadas = jdbc.update(
                        "INSERT INTO " + TABELA_NOVA + " (id, representante_id, meta_id, mes, valor_meta, valor_realizado) "
                                + "SELECT id, representante_id, meta_id, ?, valor_meta, valor_realizado FROM " + TABELA_ANTIGA,
                        mes);
                log.info("Metas por mês: {} meta(s) de representante copiada(s) pra {} como {}", copiadas, TABELA_NOVA, mes);
            }

            int novas = contarNovas();
            if (novas >= antigas) {
                jdbc.execute("DROP TABLE " + TABELA_ANTIGA);
                log.info("Metas por mês: tabela {} apagada ({} linha(s) já estão em {})", TABELA_ANTIGA, antigas, TABELA_NOVA);
            } else {
                log.warn("Metas por mês: {} tem {} linha(s) e {} só {} — a tabela antiga NÃO foi apagada",
                        TABELA_ANTIGA, antigas, TABELA_NOVA, novas);
            }
        } catch (DataAccessException e) {
            log.error("Metas por mês: não foi possível migrar {} pra {}", TABELA_ANTIGA, TABELA_NOVA, e);
        }
    }

    private int contarNovas() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + TABELA_NOVA, Integer.class);
        return n == null ? 0 : n;
    }
}
