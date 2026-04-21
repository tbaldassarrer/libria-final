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
public class ReviewController {

    @GetMapping("/getReviews")
    @ResponseBody
    public List<Map<String, String>> getUserReviews(Principal principal) {
        List<Map<String, String>> reviews = new ArrayList<>();

        // Si el usuario no está autenticado, devuelve una lista vacía.
        if (principal == null) {
            return reviews;
        }

        String username = principal.getName();

        try (MySqlConnection db = new MySqlConnection()) {
            db.open(); // Abre la conexión
            Connection connection = db.connection;
            
            String sql = "SELECT l.titulo, rl.resenia, rl.puntuacion " +
                         "FROM registrolectura rl " +
                         "JOIN libros l ON rl.idLibro = l.idLibro " +
                         "JOIN usuariolector u ON rl.idUsuario = u.idUsuario " +
                         "WHERE u.nombreUsuario = ? AND rl.resenia IS NOT NULL " +
                         "AND rl.estadoLectura = 'Completado'";
            
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, username);
                
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, String> review = new HashMap<>();
                        review.put("titulo", rs.getString("titulo"));
                        review.put("resenia", rs.getString("resenia"));
                        review.put("puntuacion", rs.getString("puntuacion"));
                        reviews.add(review);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Error al obtener las reseñas del usuario: " + e.getMessage());
        }

        return reviews;
    }
}
