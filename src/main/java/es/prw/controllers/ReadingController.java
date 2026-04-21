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
public class ReadingController {
    @PostMapping("/addToLibraryWithReview")
    @ResponseBody
    public Map<String, Object> addToLibraryWithReview(
            @RequestParam("title") String title,
            @RequestParam("resenia") String resenia,
            @RequestParam("puntuacion") int puntuacion,
            Principal principal) {
    
        Map<String, Object> response = new HashMap<>();
    
        if (principal == null) {
            response.put("success", false);
            response.put("message", "Usuario no autenticado.");
            return response;
        }
    
        // ✅ Verificar si el título llega correctamente
        System.out.println("📌 Título recibido en backend: " + title);
    
        String username = principal.getName();
    
        try (MySqlConnection db = new MySqlConnection()) {
            db.open();
            Connection connection = db.connection;
    
            // Buscar el ID del libro por el título
            String sql = "SELECT idLibro, cover_image FROM libros WHERE titulo = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, title);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int idLibro = rs.getInt("idLibro");
                        String coverImage = rs.getString("cover_image");
    
                        // Buscar ID del usuario
                        String userQuery = "SELECT idUsuario FROM usuariolector WHERE nombreUsuario = ?";
                        int idUsuario = -1;
                        try (PreparedStatement psUser = connection.prepareStatement(userQuery)) {
                            psUser.setString(1, username);
                            try (ResultSet rsUser = psUser.executeQuery()) {
                                if (rsUser.next()) {
                                    idUsuario = rsUser.getInt("idUsuario");
                                }
                            }
                        }
    
                        if (idUsuario == -1) {
                            response.put("success", false);
                            response.put("message", "Usuario no encontrado.");
                            return response;
                        }
    
                        // Verificar si el libro ya está en la biblioteca del usuario
                        String checkQuery = "SELECT * FROM registrolectura WHERE idUsuario = ? AND idLibro = ?";
                        try (PreparedStatement psCheck = connection.prepareStatement(checkQuery)) {
                            psCheck.setInt(1, idUsuario);
                            psCheck.setInt(2, idLibro);
                            try (ResultSet rsCheck = psCheck.executeQuery()) {
                                if (rsCheck.next()) {
                                    response.put("success", false);
                                    response.put("message", "El libro ya está en tu biblioteca.");
                                    return response;
                                }
                            }
                        }
    
                        // Insertar libro con reseña
                        String insertQuery = "INSERT INTO registrolectura (estadoLectura, fechaInicio, idLibro, idUsuario, resenia, puntuacion) VALUES ('Completado', NOW(), ?, ?, ?, ?)";
                        try (PreparedStatement psInsert = connection.prepareStatement(insertQuery)) {
                            psInsert.setInt(1, idLibro);
                            psInsert.setInt(2, idUsuario);
                            psInsert.setString(3, resenia);
                            psInsert.setInt(4, puntuacion);
                            psInsert.executeUpdate();
                        }
    
                        response.put("success", true);
                        response.put("cover_image", coverImage);
                        return response;
                    } else {
                        response.put("success", false);
                        response.put("message", "Libro no encontrado en la base de datos.");
                    }
                }
            }
        } catch (SQLException e) {
            response.put("success", false);
            response.put("message", "Error al agregar el libro: " + e.getMessage());
        }
        return response;
    }
      

    @GetMapping("/getLibraryBooks")
    @ResponseBody
    public List<Map<String, String>> getLibraryBooks(Principal principal) {
        List<Map<String, String>> books = new ArrayList<>();

        if (principal == null) {
            return books; // Devuelve lista vacía si el usuario no está autenticado
        }

        String username = principal.getName();

        try (MySqlConnection db = new MySqlConnection()) {
            db.open();
            Connection connection = db.connection;

            String sql = "SELECT l.titulo, l.cover_image, rl.fechaInicio FROM registrolectura rl " +
                         "JOIN libros l ON rl.idLibro = l.idLibro " +
                         "JOIN usuariolector u ON rl.idUsuario = u.idUsuario " +
                         "WHERE u.nombreUsuario = ? AND rl.estadoLectura = 'Completado' " +
                         "ORDER BY rl.fechaFin ASC";

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, String> book = new HashMap<>();
                        book.put("titulo", rs.getString("titulo"));
                        book.put("cover_image", rs.getString("cover_image"));
                        book.put("fechaInicio", rs.getString("fechaInicio"));
                        books.add(book);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Error al obtener los libros leídos del usuario: " + e.getMessage());
        }

        return books;
    }

    @PostMapping("/addToUpcomingReads")
    @ResponseBody
    public Map<String, Object> addToUpcomingReads(@RequestParam("title") String title, Principal principal) {
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

            String sql = "SELECT idLibro, cover_image FROM libros WHERE titulo = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, title);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int idLibro = rs.getInt("idLibro");
                        String coverImage = rs.getString("cover_image");

                        // Obtener idUsuario
                        String userQuery = "SELECT idUsuario FROM usuariolector WHERE nombreUsuario = ?";
                        int idUsuario = -1;
                        try (PreparedStatement psUser = connection.prepareStatement(userQuery)) {
                            psUser.setString(1, username);
                            try (ResultSet rsUser = psUser.executeQuery()) {
                                if (rsUser.next()) {
                                    idUsuario = rsUser.getInt("idUsuario");
                                }
                            }
                        }
                        if (idUsuario == -1) {
                            response.put("success", false);
                            response.put("message", "Usuario no encontrado.");
                            return response;
                        }

                        // Verificar si el libro ya está marcado como "Completado" (ya leído)
                        String checkQuery = "SELECT * FROM registrolectura WHERE idUsuario = ? AND idLibro = ? AND estadoLectura = 'Completas Lecturas'";
                        // Nota: Revisa la lógica de estado: ¿debería ser 'Próximas Lecturas' o 'Completado'?
                        try (PreparedStatement psCheck = connection.prepareStatement(checkQuery)) {
                            psCheck.setInt(1, idUsuario);
                            psCheck.setInt(2, idLibro);
                            try (ResultSet rsCheck = psCheck.executeQuery()) {
                                if (rsCheck.next()) {
                                    response.put("success", false);
                                    response.put("message", "Libro leído");
                                    return response;
                                }
                            }
                        }

                        // Insertar en registrolectura con estado "Próximas Lecturas"
                        String insertQuery = "INSERT INTO registrolectura (estadoLectura, fechaInicio, idLibro, idUsuario) VALUES ('Próximas Lecturas', NOW(), ?, ?)";
                        try (PreparedStatement psInsert = connection.prepareStatement(insertQuery)) {
                            psInsert.setInt(1, idLibro);
                            psInsert.setInt(2, idUsuario);
                            psInsert.executeUpdate();
                        }

                        response.put("success", true);
                        response.put("cover_image", coverImage);
                        return response;
                    }
                }
            }
        } catch (SQLException e) {
            response.put("success", false);
            response.put("message", "Error al agregar el libro a Próximas Lecturas: " + e.getMessage());
        }

        return response;
    }

    @GetMapping("/getUpcomingReads")
    @ResponseBody
    public List<Map<String, String>> getUpcomingReads(Principal principal) {
        List<Map<String, String>> books = new ArrayList<>();

        if (principal == null) {
            return books;
        }

        String username = principal.getName();

        try (MySqlConnection db = new MySqlConnection()) {
            db.open();
            Connection connection = db.connection;

            String sql = "SELECT l.titulo, l.cover_image FROM registrolectura rl " +
                         "JOIN libros l ON rl.idLibro = l.idLibro " +
                         "JOIN usuariolector u ON rl.idUsuario = u.idUsuario " +
                         "WHERE u.nombreUsuario = ? AND rl.estadoLectura = 'Próximas Lecturas'";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, String> book = new HashMap<>();
                        book.put("titulo", rs.getString("titulo"));
                        book.put("cover_image", rs.getString("cover_image"));
                        books.add(book);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Error al obtener los libros de Próximas Lecturas: " + e.getMessage());
        }

        return books;
    }

    @PostMapping("/startReading")
@ResponseBody
public Map<String, Object> startReading(@RequestParam("title") String title, Principal principal) {
    Map<String, Object> response = new HashMap<>();
    
    if (principal == null) {
        response.put("success", false);
        response.put("message", "Usuario no autenticado.");
        return response;
    }

    String username = principal.getName();
    int idUsuario = -1;
    int idLibro = -1;
    String author = "";
    String fechaInicio = "";

    try (MySqlConnection db = new MySqlConnection()) {
        db.open();
        Connection connection = db.connection;

        // 1) Obtener idUsuario
        String userQuery = "SELECT idUsuario FROM usuariolector WHERE nombreUsuario = ?";
        try (PreparedStatement psUser = connection.prepareStatement(userQuery)) {
            psUser.setString(1, username);
            try (ResultSet rsUser = psUser.executeQuery()) {
                if (rsUser.next()) {
                    idUsuario = rsUser.getInt("idUsuario");
                }
            }
        }
        if (idUsuario == -1) {
            response.put("success", false);
            response.put("message", "Usuario no encontrado.");
            return response;
        }

        // 2) Obtener idLibro y autor
        String bookQuery = "SELECT idLibro, autor FROM libros WHERE titulo = ?";
        try (PreparedStatement psBook = connection.prepareStatement(bookQuery)) {
            psBook.setString(1, title);
            try (ResultSet rsBook = psBook.executeQuery()) {
                if (rsBook.next()) {
                    idLibro = rsBook.getInt("idLibro");
                    author = rsBook.getString("autor");
                }
            }
        }
        if (idLibro == -1) {
            response.put("success", false);
            response.put("message", "Libro no encontrado.");
            return response;
        }
        
             // NUEVO: Comprobar si el libro ya está en "Completado" (ya leído)
             String checkCompletedQuery = "SELECT * FROM registrolectura WHERE idUsuario = ? AND idLibro = ? AND estadoLectura = 'Completado'";
             try (PreparedStatement psCompleted = connection.prepareStatement(checkCompletedQuery)) {
                 psCompleted.setInt(1, idUsuario);
                 psCompleted.setInt(2, idLibro);
                 try (ResultSet rsCompleted = psCompleted.executeQuery()) {
                     if (rsCompleted.next()) {
                         response.put("success", false);
                         response.put("message", "Libro ya leído");
                         return response;
                     }
                 }
             }

        // 3) Cerrar cualquier libro "En progreso"
        String closeCurrent = "UPDATE registrolectura SET estadoLectura = 'Completado', fechaFin = NOW() "
                            + "WHERE estadoLectura = 'En progreso' AND idUsuario = ?";
        try (PreparedStatement psUpdate = connection.prepareStatement(closeCurrent)) {
            psUpdate.setInt(1, idUsuario);
            psUpdate.executeUpdate();
        }

        // 4) Revisar si el libro ya está en "Próximas Lecturas" y pasarlo a "En progreso"
        String updateToInProgress = 
            "UPDATE registrolectura SET estadoLectura = 'En progreso', fechaInicio = NOW() "
          + "WHERE idUsuario = ? AND idLibro = ?";
        try (PreparedStatement psInProgress = connection.prepareStatement(updateToInProgress)) {
            psInProgress.setInt(1, idUsuario);
            psInProgress.setInt(2, idLibro);
            int rowsUpdated = psInProgress.executeUpdate();
            if (rowsUpdated > 0) {
                // Obtener la fecha de inicio recién actualizada
                fechaInicio = "SELECT DATE_FORMAT(fechaInicio, '%Y-%m-%d') FROM registrolectura WHERE idUsuario = ? AND idLibro = ?";
                try (PreparedStatement psFecha = connection.prepareStatement(fechaInicio)) {
                    psFecha.setInt(1, idUsuario);
                    psFecha.setInt(2, idLibro);
                    try (ResultSet rsFecha = psFecha.executeQuery()) {
                        if (rsFecha.next()) {
                            fechaInicio = rsFecha.getString(1);
                        }
                    }
                }
            }
        }

        // 5) Si el libro no estaba en "Próximas Lecturas", insertarlo como "En progreso"
        if (fechaInicio.isEmpty()) {
            String insertQuery = 
                "INSERT INTO registrolectura (estadoLectura, fechaInicio, idLibro, idUsuario) "
              + "VALUES ('En progreso', NOW(), ?, ?)";
            try (PreparedStatement psInsert = connection.prepareStatement(insertQuery)) {
                psInsert.setInt(1, idLibro);
                psInsert.setInt(2, idUsuario);
                psInsert.executeUpdate();
            }

            // Obtener la fecha recién insertada
            fechaInicio = "SELECT DATE_FORMAT(fechaInicio, '%Y-%m-%dT00:00:00') FROM registrolectura WHERE idUsuario = ? AND idLibro = ?";
            try (PreparedStatement psFecha = connection.prepareStatement(fechaInicio)) {
                psFecha.setInt(1, idUsuario);
                psFecha.setInt(2, idLibro);
                try (ResultSet rsFecha = psFecha.executeQuery()) {
                    if (rsFecha.next()) {
                        fechaInicio = rsFecha.getString(1);
                    }
                }
            }
        }

        // 6) Retornar datos actualizados
        response.put("success", true);
        response.put("reading", title + " - " + author);
        response.put("idLibro", idLibro);
        response.put("fechaInicio", fechaInicio);

    } catch (SQLException e) {
        response.put("success", false);
        response.put("message", "Error al comenzar a leer: " + e.getMessage());
    }

    return response;
}

@GetMapping("/checkIfBookExists")
@ResponseBody
public Map<String, Object> checkIfBookExists(@RequestParam("title") String title, Principal principal) {
    Map<String, Object> response = new HashMap<>();

    if (principal == null) {
        response.put("exists", false);
        return response;
    }

    String username = principal.getName();

    try (MySqlConnection db = new MySqlConnection()) {
        db.open();
        Connection connection = db.connection;

        String sql = "SELECT rl.idLibro FROM registrolectura rl " +
                     "JOIN libros l ON rl.idLibro = l.idLibro " +
                     "JOIN usuariolector u ON rl.idUsuario = u.idUsuario " +
                     "WHERE u.nombreUsuario = ? AND l.titulo = ? AND rl.estadoLectura = 'Completado'";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, title);
            try (ResultSet rs = ps.executeQuery()) {
                response.put("exists", rs.next()); // Retorna true si el libro ya está en la biblioteca
            }
        }
    } catch (SQLException e) {
        response.put("exists", false);
        System.out.println("❌ Error al verificar si el libro existe: " + e.getMessage());
    }

    return response;
}


    
    @GetMapping("/getCurrentReading")
    @ResponseBody
    public Map<String, String> getCurrentReading(Principal principal) {
        Map<String, String> response = new HashMap<>();
    
        if (principal == null) {
            response.put("reading", "");
            return response;
        }
    
        String username = principal.getName();
    
        try (MySqlConnection db = new MySqlConnection()) {
            db.open();
            Connection connection = db.connection;
    
            // Obtener ID del usuario autenticado
            String userQuery = "SELECT idUsuario FROM usuariolector WHERE nombreUsuario = ?";
            int idUsuario = -1;
            try (PreparedStatement psUser = connection.prepareStatement(userQuery)) {
                psUser.setString(1, username);
                try (ResultSet rsUser = psUser.executeQuery()) {
                    if (rsUser.next()) {
                        idUsuario = rsUser.getInt("idUsuario");
                    }
                }
            }
    
            if (idUsuario == -1) {
                response.put("reading", "");
                return response;
            }
    // Configurar el idioma a español en MySQL
    String setLanguage = "SET lc_time_names = 'es_ES'";
    try (PreparedStatement psSetLang = connection.prepareStatement(setLanguage)) {
        psSetLang.execute();
    }            // Obtener el libro que el usuario tiene en estado "En progreso"
            String sql = "SELECT l.idLibro, l.titulo, l.autor, l.cover_image, " +
             "COALESCE(DATE_FORMAT(rl.fechaInicio, '%W, %d de %M de %Y'), '') AS fechaInicio " +
             "FROM registrolectura rl " +
             "JOIN libros l ON rl.idLibro = l.idLibro " +
             "WHERE rl.estadoLectura = 'En progreso' AND rl.idUsuario = ? " +
             "ORDER BY rl.fechaInicio DESC LIMIT 1";

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, idUsuario);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        response.put("idLibro", String.valueOf(rs.getInt("idLibro")));
                        response.put("titulo", rs.getString("titulo"));
                        response.put("autor", rs.getString("autor"));
                        response.put("cover_image", rs.getString("cover_image"));
                        
                        String fechaInicio = rs.getString("fechaInicio");
                        response.put("fechaInicio", fechaInicio != null ? fechaInicio.trim() : ""); 
                    } else {
                        response.put("reading", "");
                    }
                    
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Error al obtener el estado de lectura: " + e.getMessage());
            response.put("reading", "");
        }
    
        return response;
    }
    
    

    @PostMapping("/finishReading")
    @ResponseBody
    public Map<String, Object> finishReading(
            @RequestParam("idLibro") int idLibro,
            @RequestParam("rating") int rating,
            @RequestParam("review") String review,
            Principal principal) {

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
            // Obtener ID del usuario
            String userQuery = "SELECT idUsuario FROM usuariolector WHERE nombreUsuario = ?";
            int idUsuario = -1;
            try (PreparedStatement psUser = connection.prepareStatement(userQuery)) {
                psUser.setString(1, username);
                try (ResultSet rsUser = psUser.executeQuery()) {
                    if (rsUser.next()) {
                        idUsuario = rsUser.getInt("idUsuario");
                    }
                }
            }
            if (idUsuario == -1) {
                response.put("success", false);
                response.put("message", "Usuario no encontrado.");
                return response;
            }
            // Validar que la reseña no está vacía
            if (review == null || review.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "La reseña no puede estar vacía.");
                return response;
            }
            // Actualizar estado de lectura
            String updateQuery = "UPDATE registrolectura SET estadoLectura = 'Completado', fechaFin = NOW(), resenia = ?, puntuacion = ? WHERE idUsuario = ? AND idLibro = ?";
            try (PreparedStatement psUpdate = connection.prepareStatement(updateQuery)) {
                psUpdate.setString(1, review);
                psUpdate.setInt(2, rating);
                psUpdate.setInt(3, idUsuario);
                psUpdate.setInt(4, idLibro);
                psUpdate.executeUpdate();
            }
            response.put("success", true);
            response.put("message", "Reseña guardada correctamente.");
            return response;
        } catch (SQLException e) {
            response.put("success", false);
            response.put("message", "Error al registrar la reseña: " + e.getMessage());
        }
        return response;
    }

    @PostMapping("/removeCurrentReadingBook")
    @ResponseBody
    public Map<String, Object> removeCurrentReadingBook(@RequestParam("idLibro") int idLibro, Principal principal) {
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
    
            // Obtener ID del usuario
            String userQuery = "SELECT idUsuario FROM usuariolector WHERE nombreUsuario = ?";
            int idUsuario = -1;
            try (PreparedStatement psUser = connection.prepareStatement(userQuery)) {
                psUser.setString(1, username);
                try (ResultSet rsUser = psUser.executeQuery()) {
                    if (rsUser.next()) {
                        idUsuario = rsUser.getInt("idUsuario");
                    }
                }
            }
    
            if (idUsuario == -1) {
                response.put("success", false);
                response.put("message", "Usuario no encontrado.");
                return response;
            }
    
            // 🔥 Eliminar el libro en progreso de TODAS las categorías
            String deleteQuery = "DELETE FROM registrolectura WHERE idUsuario = ? AND idLibro = ?";
            try (PreparedStatement psDelete = connection.prepareStatement(deleteQuery)) {
                psDelete.setInt(1, idUsuario);
                psDelete.setInt(2, idLibro);
                psDelete.executeUpdate();
            }
    
            response.put("success", true);
            return response;
    
        } catch (SQLException e) {
            response.put("success", false);
            response.put("message", "Error al eliminar el libro en progreso: " + e.getMessage());
        }
        return response;
    }
    
}
