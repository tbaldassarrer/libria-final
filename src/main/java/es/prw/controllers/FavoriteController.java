package es.prw.controllers;

import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import es.prw.dao.MySqlConnection;

@Controller
public class FavoriteController {

    @GetMapping("/getFavorites")
    @ResponseBody
    public List<Map<String, String>> getFavorites(Principal principal) {
        List<Map<String, String>> favoriteBooks = new ArrayList<>();

        if (principal == null) {
            return favoriteBooks; // Si el usuario no está autenticado, devolver lista vacía
        }

        String username = principal.getName();

        try (MySqlConnection db = new MySqlConnection()) {
            db.open(); // Abre la conexión a la base de datos
            Connection connection = db.connection;
            
            // Consulta para obtener los libros con puntuación 5 para el usuario autenticado
            String sql = "SELECT l.titulo, l.cover_image " +
                         "FROM registrolectura rl " +
                         "JOIN libros l ON rl.idLibro = l.idLibro " +
                         "JOIN usuariolector u ON rl.idUsuario = u.idUsuario " +
                         "WHERE u.nombreUsuario = ? AND rl.puntuacion = 5";
            
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, username);
                
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, String> book = new HashMap<>();
                        book.put("titulo", rs.getString("titulo"));
                        book.put("cover_image", rs.getString("cover_image"));
                        favoriteBooks.add(book);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Error al obtener los favoritos del usuario: " + e.getMessage());
        }

        return favoriteBooks;
    }
}
