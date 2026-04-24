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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
            
            String sql = "SELECT l.idLibro, l.titulo, rl.resenia, rl.puntuacion " +
                         "FROM registrolectura rl " +
                         "JOIN libros l ON rl.idLibro = l.idLibro " +
                         "JOIN usuariolector u ON rl.idUsuario = u.id_usuario " +
                         "WHERE u.nombre_usuario = ? AND rl.resenia IS NOT NULL " +
                         "AND rl.estadoLectura = 'Completado'";
            
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, username);
                
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, String> review = new HashMap<>();
                        review.put("idLibro", String.valueOf(rs.getInt("idLibro")));
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

    @GetMapping("/communityReviews")
    @ResponseBody
    public List<Map<String, String>> getCommunityReviewsByBook(@RequestParam("idLibro") int idLibro) {
        List<Map<String, String>> reviews = new ArrayList<>();

        try (MySqlConnection db = new MySqlConnection()) {
            db.open();
            Connection connection = db.connection;

            String sql = "SELECT u.nombre_usuario, rl.resenia, rl.puntuacion, " +
                    "DATE_FORMAT(COALESCE(rl.fechaFin, rl.fechaInicio), '%d/%m/%Y') AS fechaActividad " +
                    "FROM registrolectura rl " +
                    "JOIN usuariolector u ON rl.idUsuario = u.id_usuario " +
                    "WHERE rl.idLibro = ? AND rl.estadoLectura = 'Completado' " +
                    "AND rl.resenia IS NOT NULL AND TRIM(rl.resenia) <> '' " +
                    "ORDER BY COALESCE(rl.fechaFin, rl.fechaInicio) DESC";

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, idLibro);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, String> review = new HashMap<>();
                        review.put("usuario", rs.getString("nombre_usuario"));
                        review.put("resenia", rs.getString("resenia"));
                        review.put("puntuacion", rs.getString("puntuacion"));
                        review.put("fecha", rs.getString("fechaActividad"));
                        reviews.add(review);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener las resenas publicas del libro: " + e.getMessage());
        }

        return reviews;
    }

    @PostMapping("/deleteReview")
    @ResponseBody
    public Map<String, Object> deleteReview(@RequestParam("idLibro") int idLibro, Principal principal) {
        Map<String, Object> response = new HashMap<>();

        if (principal == null) {
            response.put("success", false);
            response.put("message", "Usuario no autenticado.");
            return response;
        }

        String username = principal.getName();

        try (MySqlConnection db = new MySqlConnection()) {
            db.open();
            Connection connection = db.connection;

            String sql = "UPDATE registrolectura rl " +
                    "JOIN usuariolector u ON rl.idUsuario = u.id_usuario " +
                    "SET rl.resenia = NULL, rl.puntuacion = NULL " +
                    "WHERE u.nombre_usuario = ? AND rl.idLibro = ? AND rl.estadoLectura = 'Completado'";

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setInt(2, idLibro);
                response.put("success", ps.executeUpdate() > 0);
            }
        } catch (SQLException e) {
            response.put("success", false);
            response.put("message", "Error al eliminar la reseña: " + e.getMessage());
        }

        return response;
    }
}
