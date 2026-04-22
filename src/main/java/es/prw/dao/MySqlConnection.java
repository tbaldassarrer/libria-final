package es.prw.dao;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MySqlConnection implements AutoCloseable {

    private static final String LOCAL_URL =
            "jdbc:mysql://localhost:3306/db_libria?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String LOCAL_USER = "root";
    private static final String LOCAL_PASSWORD = "1234";

    private final boolean autocomit = true;
    private boolean flagError;
    private String msgError;

    public Connection connection;

    public void open() {
        try {
            this.flagError = false;
            this.msgError = "";

            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(resolveUrl(), resolveUsername(), resolvePassword());
            this.connection.setAutoCommit(this.autocomit);
        } catch (ClassNotFoundException | SQLException ex) {
            this.flagError = true;
            this.msgError = ex.getMessage();
            System.out.println("Error al abrir conexion MySQL: " + ex.getMessage());
        }
    }

    public ResultSet executeSelect(String sql) throws SQLException {
        Statement statement = this.connection.createStatement();
        return statement.executeQuery(sql);
    }

    @Override
    public void close() {
        if (this.connection != null) {
            try {
                this.connection.close();
            } catch (SQLException ex) {
                this.flagError = true;
                this.msgError = "Error al cerrar la conexion: " + ex.getMessage();
            }
        }
    }

    public boolean getFlagError() {
        return this.flagError;
    }

    public String getMsgError() {
        return this.msgError;
    }

    public PreparedStatement prepareStatement(String query) throws SQLException {
        return this.connection.prepareStatement(query);
    }

    public PreparedStatement prepare(String query) throws SQLException {
        return this.connection.prepareStatement(query);
    }

    public String getPuerto() {
        return envOrDefault("MYSQLPORT", "3306");
    }

    public String getNameDB() {
        return envOrDefault("MYSQLDATABASE", "db_libria");
    }

    public String getUsuario() {
        return resolveUsername();
    }

    private String resolveUrl() {
        String dbUrl = firstPresent(
                System.getenv("DB_URL"),
                System.getenv("MYSQL_URL"),
                System.getenv("MYSQL_PUBLIC_URL"),
                System.getenv("DATABASE_URL"));

        if (dbUrl != null && !dbUrl.isBlank()) {
            return normalizeJdbcUrl(dbUrl);
        }

        String host = System.getenv("MYSQLHOST");
        String port = System.getenv("MYSQLPORT");
        String database = System.getenv("MYSQLDATABASE");

        if (hasText(host) && hasText(port) && hasText(database)) {
            return "jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        }

        return LOCAL_URL;
    }

    private String resolveUsername() {
        String username = firstPresent(System.getenv("DB_USERNAME"), System.getenv("MYSQLUSER"));
        if (hasText(username)) {
            return username;
        }

        String urlUsername = getCredentialFromUrl(0);
        return hasText(urlUsername) ? urlUsername : LOCAL_USER;
    }

    private String resolvePassword() {
        String password = firstPresent(System.getenv("DB_PASSWORD"), System.getenv("MYSQLPASSWORD"));
        if (hasText(password)) {
            return password;
        }

        String urlPassword = getCredentialFromUrl(1);
        return hasText(urlPassword) ? urlPassword : LOCAL_PASSWORD;
    }

    private String normalizeJdbcUrl(String rawUrl) {
        if (rawUrl.startsWith("jdbc:mysql://")) {
            return rawUrl;
        }

        if (rawUrl.startsWith("mysql://")) {
            return convertMysqlUrlToJdbc(rawUrl);
        }

        return rawUrl;
    }

    private String convertMysqlUrlToJdbc(String mysqlUrl) {
        try {
            URI uri = URI.create(mysqlUrl);
            String query = uri.getQuery();
            String database = uri.getPath() == null ? "" : uri.getPath();
            String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
            String jdbcUrl = "jdbc:mysql://" + uri.getHost() + port + database;

            if (query == null || query.isBlank()) {
                return jdbcUrl + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
            }

            return jdbcUrl + "?" + query;
        } catch (Exception e) {
            return mysqlUrl.replaceFirst("^mysql://", "jdbc:mysql://");
        }
    }

    private String envOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        return hasText(value) ? value : fallback;
    }

    private String getCredentialFromUrl(int index) {
        String dbUrl = firstPresent(
                System.getenv("DB_URL"),
                System.getenv("MYSQL_URL"),
                System.getenv("MYSQL_PUBLIC_URL"),
                System.getenv("DATABASE_URL"));

        if (!hasText(dbUrl) || !dbUrl.startsWith("mysql://")) {
            return "";
        }

        try {
            String userInfo = URI.create(dbUrl).getUserInfo();
            if (!hasText(userInfo)) {
                return "";
            }

            String[] credentials = userInfo.split(":", 2);
            if (credentials.length <= index) {
                return "";
            }

            return URLDecoder.decode(credentials[index], StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
