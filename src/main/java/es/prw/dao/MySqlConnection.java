package es.prw.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MySqlConnection implements AutoCloseable { // Implementamos AutoCloseable

    private final String host = "localhost";
    private final String puerto = "3306";
    private final String nameDB = "db_libria";
    private final String usuario = "root";
    private final String password = "1234";

    private final boolean autocomit = true; // Habilitar el autocommit por defecto
    private boolean flagError;
    private String msgError;

    public Connection connection;

    // Método que abre la conexión
    public void open() {
        try {
            this.flagError = false;
            this.msgError = "";
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(
                    "jdbc:mysql://" + this.host + ":" + this.puerto + "/" + this.nameDB, this.usuario, this.password);
            this.connection.setAutoCommit(this.autocomit);
        } catch (ClassNotFoundException | SQLException ex) {
            this.flagError = true;
            this.msgError = ex.getMessage();
        }
    }

    // Método para ejecutar consultas SELECT
    public ResultSet executeSelect(String sql) throws SQLException {
        Statement statement = this.connection.createStatement();
        return statement.executeQuery(sql);
    }

    // Método para cerrar la conexión
    @Override
    public void close() {
        if (this.connection != null) {
            try {
                this.connection.close();
            } catch (SQLException ex) {
                this.flagError = true;
                this.msgError = "Error al cerrar la conexión: " + ex.getMessage();
            }
        }
    }

    // Getters para errores
    public boolean getFlagError() {
        return this.flagError;
    }

    public String getMsgError() {
        return this.msgError;
    }

    public PreparedStatement prepareStatement(String query) {
        return null;
    }

    public PreparedStatement prepare(String rolQuery) {
        return null;
    }

    public String getPuerto() {
        return puerto;
    }

    public String getNameDB() {
        return nameDB;
    }

    public String getUsuario() {
        return usuario;
    }
}
