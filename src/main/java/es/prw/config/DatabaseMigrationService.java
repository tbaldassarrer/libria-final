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
        addColumnIfMissing("usuariolector", "id_usuario", "INT NULL");
        addColumnIfMissing("usuariolector", "nombre_usuario", "VARCHAR(255) NULL");
        if (columnExists("usuariolector", "idUsuario")) {
            jdbcTemplate.update("UPDATE usuariolector SET id_usuario = idUsuario WHERE id_usuario IS NULL");
        }
        if (columnExists("usuariolector", "nombreUsuario")) {
            jdbcTemplate.update("UPDATE usuariolector SET nombre_usuario = nombreUsuario "
                    + "WHERE (nombre_usuario IS NULL OR nombre_usuario = '') AND nombreUsuario IS NOT NULL");
        }
        jdbcTemplate.update("UPDATE usuariolector SET enabled = 1 WHERE enabled IS NULL");
        jdbcTemplate.update("UPDATE usuariolector SET quote_name_public = 0 WHERE quote_name_public IS NULL");
        addColumnIfMissing("readingjournal", "idLibro", "INT NULL");
        createReadingClubTables();
    }

    private void createReadingClubTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS reading_club (
                    id_club INT AUTO_INCREMENT PRIMARY KEY,
                    id_libro INT NOT NULL,
                    owner_user_id INT NOT NULL,
                    name VARCHAR(160) NOT NULL,
                    description VARCHAR(700) NULL,
                    current_chapter INT DEFAULT 1,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_reading_club_book (id_libro),
                    INDEX idx_reading_club_owner (owner_user_id)
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS reading_club_member (
                    id_club INT NOT NULL,
                    id_usuario INT NOT NULL,
                    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id_club, id_usuario),
                    INDEX idx_reading_club_member_user (id_usuario)
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS reading_club_comment (
                    id_comment INT AUTO_INCREMENT PRIMARY KEY,
                    id_club INT NOT NULL,
                    id_usuario INT NOT NULL,
                    chapter_number INT NOT NULL,
                    comment_text TEXT NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_reading_club_comment_club_chapter (id_club, chapter_number),
                    INDEX idx_reading_club_comment_user (id_usuario)
                )
                """);
    }

    private void addColumnIfMissing(String tableName, String columnName, String definition) {
        if (!columnExists(tableName, columnName)) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName);

        return count != null && count > 0;
    }
}
