package com.almoxarifado.api.meta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Migração de uma vez só: no MySQL o Hibernate criou metas.unidade como enum('KG','REAL','UNIDADE'),
 * e o ddl-auto=update nunca altera coluna que já existe — então gravar uma unidade nova (CLIENTES)
 * dava "Data truncated". Ao subir, se a coluna ainda for enum, troca por varchar(20), que é o que
 * {@link Meta} declara agora.
 *
 * Banco novo ou já migrado: não faz nada. Qualquer erro só vai pro log: a API sobe do mesmo jeito.
 */
@Component
public class MigracaoUnidadeMetaVarchar implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracaoUnidadeMetaVarchar.class);

    private final JdbcTemplate jdbc;

    public MigracaoUnidadeMetaVarchar(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer colunasEnum = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE()"
                            + " AND LOWER(table_name) = 'metas' AND LOWER(column_name) = 'unidade'"
                            + " AND LOWER(data_type) = 'enum'",
                    Integer.class);
            if (colunasEnum == null || colunasEnum == 0) return;

            jdbc.execute("ALTER TABLE metas MODIFY unidade VARCHAR(20) NOT NULL");
            log.info("Unidade de meta: coluna metas.unidade convertida de enum para varchar(20)");
        } catch (DataAccessException e) {
            log.error("Unidade de meta: não foi possível converter metas.unidade para varchar(20)", e);
        }
    }
}
