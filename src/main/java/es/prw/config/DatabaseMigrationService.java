package es.prw.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class DatabaseMigrationService {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigrationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void migrate() {
        addColumnIfMissing("usuariolector", "enabled", "BIT DEFAULT 1");
        addColumnIfMissing("usuariolector", "activation_token", "VARCHAR(100) NULL");
        addColumnIfMissing("usuariolector", "activation_token_created_at", "DATETIME(6) NULL");
        addColumnIfMissing("usuariolector", "quote_name_public", "BIT DEFAULT 0");
        jdbcTemplate.update("UPDATE usuariolector SET enabled = 1 WHERE enabled IS NULL");
        jdbcTemplate.update("UPDATE usuariolector SET quote_name_public = 0 WHERE quote_name_public IS NULL");
    }

    private void addColumnIfMissing(String tableName, String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName);

        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }
}
