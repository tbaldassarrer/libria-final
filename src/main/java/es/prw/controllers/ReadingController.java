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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import es.prw.dao.MySqlConnection;
import es.prw.services.GoogleBooksService;

@Controller
public class ReadingController {
    @Autowired
    private GoogleBooksService googleBooksService;

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
        googleBooksService.findOrCreateByTitle(title);

        try (MySqlConnection db = new MySqlConnection()) {
            db.open();
            Connection connection = db.connection;
    
            // Buscar el ID del libro por el título
            String sql = "SELECT idLibro, cover_image FROM libros WHERE LOWER(titulo) = LOWER(?)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, title);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int idLibro = rs.getInt("idLibro");
                        String coverImage = rs.getString("cover_image");
    
                        // Buscar ID del usuario
                        String userQuery = "SELECT id_usuario FROM usuariolector WHERE nombre_usuario = ?";
                        int idUsuario = -1;
                        try (PreparedStatement psUser = connection.prepareStatement(userQuery)) {
                            psUser.setString(1, username);
                            try (ResultSet rsUser = psUser.executeQuery()) {
                                if (rsUser.next()) {
                                    idUsuario = rsUser.getInt("id_usuario");
                                }
                            }
                        }
    
                        if (idUsuario == -1) {
                            response.put("success", false);
                            response.put("message", "Usuario no encontrado.");
                            return response;
                        }
    
                        // Verificar si el libro ya está en la biblioteca del usuario
                        String checkQuery = "SELECT estadoLectura FROM registrolectura WHERE idUsuario = ? AND idLibro = ? AND estadoLectura = 'Completado'";
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
                        String insertQuery = "INSERT INTO registrolectura (estadoLectura, fechaInicio, fechaFin, idLibro, idUsuario, resenia, puntuacion) VALUES ('Completado', NOW(), NOW(), ?, ?, ?, ?)";
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

            String sql = "SELECT l.idLibro, l.titulo, l.cover_image, rl.fechaInicio FROM registrolectura rl " +
                         "JOIN libros l ON rl.idLibro = l.idLibro " +
                         "JOIN usuariolector u ON rl.idUsuario = u.id_usuario " +
                         "WHERE u.nombre_usuario = ? AND rl.estadoLectura = 'Completado' " +
                         "ORDER BY rl.fechaFin ASC";

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, String> book = new HashMap<>();
                        book.put("idLibro", String.valueOf(rs.getInt("idLibro")));
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
        googleBooksService.findOrCreateByTitle(title);

        try (MySqlConnection db = new MySqlConnection()) {
            db.open();
            Connection connection = db.connection;

            String sql = "SELECT idLibro, cover_image FROM libros WHERE LOWER(titulo) = LOWER(?)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, title);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int idLibro = rs.getInt("idLibro");
                        String coverImage = rs.getString("cover_image");

                        // Obtener idUsuario
                        String userQuery = "SELECT id_usuario FROM usuariolector WHERE nombre_usuario = ?";
                        int idUsuario = -1;
                        try (PreparedStatement psUser = connection.prepareStatement(userQuery)) {
                            psUser.setString(1, username);
                            try (ResultSet rsUser = psUser.executeQuery()) {
                                if (rsUser.next()) {
                                    idUsuario = rsUser.getInt("id_usuario");
                                }
                            }
                        }
                        if (idUsuario == -1) {
                            response.put("success", false);
                            response.put("message", "Usuario no encontrado.");
                            return response;
                        }

                        // Verificar si el libro ya está marcado como "Completado" (ya leído)
                        String checkQuery = "SELECT * FROM registrolectura WHERE idUsuario = ? AND idLibro = ? AND estadoLectura = 'Completado'";
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

        if (!response.containsKey("success")) {
            response.put("success", false);
            response.put("message", "Libro no encontrado en la base de datos ni en Google Books.");
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

            String sql = "SELECT l.idLibro, l.titulo, l.cover_image FROM registrolectura rl " +
                         "JOIN libros l ON rl.idLibro = l.idLibro " +
                         "JOIN usuariolector u ON rl.idUsuario = u.id_usuario " +
                         "WHERE u.nombre_usuario = ? AND rl.estadoLectura = 'Próximas Lecturas'";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, String> book = new HashMap<>();
                        book.put("idLibro", String.valueOf(rs.getInt("idLibro")));
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
    googleBooksService.findOrCreateByTitle(title);
    int idUsuario = -1;
    int idLibro = -1;
    String author = "";
    String fechaInicio = "";

    try (MySqlConnection db = new MySqlConnection()) {
        db.open();
        Connection connection = db.connection;

        // 1) Obtener idUsuario
        String userQuery = "SELECT id_usuario FROM usuariolector WHERE nombre_usuario = ?";
        try (PreparedStatement psUser = connection.prepareStatement(userQuery)) {
            psUser.setString(1, username);
            try (ResultSet rsUser = psUser.executeQuery()) {
                if (rsUser.next()) {
                    idUsuario = rsUser.getInt("id_usuario");
                }
            }
        }
        if (idUsuario == -1) {
            response.put("success", false);
            response.put("message", "Usuario no encontrado.");
            return response;
        }

        // 2) Obtener idLibro y autor
        String bookQuery = "SELECT idLibro, autor FROM libros WHERE LOWER(titulo) = LOWER(?)";
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

        // 3) Si ya existe cualquier registro del usuario para ese libro, reutilizar esa fila.
        String existingEntryQuery = "SELECT estadoLectura, DATE_FORMAT(fechaInicio, '%Y-%m-%d') AS fechaInicio " +
                "FROM registrolectura WHERE idUsuario = ? AND idLibro = ?";
        String existingState = "";
        try (PreparedStatement psExisting = connection.prepareStatement(existingEntryQuery)) {
            psExisting.setInt(1, idUsuario);
            psExisting.setInt(2, idLibro);
            try (ResultSet rsExisting = psExisting.executeQuery()) {
                if (rsExisting.next()) {
                    existingState = rsExisting.getString("estadoLectura");
                    fechaInicio = rsExisting.getString("fechaInicio");
                }
            }
        }

        if ("En progreso".equals(existingState)) {
            response.put("success", true);
            response.put("reading", title + " - " + author);
            response.put("idLibro", idLibro);
            response.put("fechaInicio", fechaInicio != null ? fechaInicio : "");
            return response;
        }

        // 4) Si existe una fila previa no completada, la actualizamos a "En progreso".
        if (!existingState.isEmpty()) {
            String updateToInProgress =
                    "UPDATE registrolectura SET estadoLectura = 'En progreso', fechaInicio = NOW(), fechaFin = NULL, resenia = NULL, puntuacion = NULL " +
                    "WHERE idUsuario = ? AND idLibro = ?";
            try (PreparedStatement psInProgress = connection.prepareStatement(updateToInProgress)) {
                psInProgress.setInt(1, idUsuario);
                psInProgress.setInt(2, idLibro);
                psInProgress.executeUpdate();
            }
        } else {
            // 5) Si no existía ninguna fila, la insertamos.
            String insertQuery =
                    "INSERT INTO registrolectura (estadoLectura, fechaInicio, idLibro, idUsuario) " +
                    "VALUES ('En progreso', NOW(), ?, ?)";
            try (PreparedStatement psInsert = connection.prepareStatement(insertQuery)) {
                psInsert.setInt(1, idLibro);
                psInsert.setInt(2, idUsuario);
                psInsert.executeUpdate();
            }
        }

        // 6) Obtener la fecha actual del registro ya consolidado.
        String fechaQuery = "SELECT DATE_FORMAT(fechaInicio, '%Y-%m-%d') FROM registrolectura WHERE idUsuario = ? AND idLibro = ?";
        try (PreparedStatement psFecha = connection.prepareStatement(fechaQuery)) {
            psFecha.setInt(1, idUsuario);
            psFecha.setInt(2, idLibro);
            try (ResultSet rsFecha = psFecha.executeQuery()) {
                if (rsFecha.next()) {
                    fechaInicio = rsFecha.getString(1);
                }
            }
        }

        // 7) Retornar datos actualizados
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
                     "JOIN usuariolector u ON rl.idUsuario = u.id_usuario " +
                     "WHERE u.nombre_usuario = ? AND l.titulo = ? AND rl.estadoLectura = 'Completado'";

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
    public Map<String, Object> getCurrentReading(Principal principal) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, String>> readings = new ArrayList<>();
    
        if (principal == null) {
            response.put("books", readings);
            response.put("reading", "");
            return response;
        }
    
        String username = principal.getName();
    
        try (MySqlConnection db = new MySqlConnection()) {
            db.open();
            Connection connection = db.connection;
    
            // Obtener ID del usuario autenticado
            String userQuery = "SELECT id_usuario FROM usuariolector WHERE nombre_usuario = ?";
            int idUsuario = -1;
            try (PreparedStatement psUser = connection.prepareStatement(userQuery)) {
                psUser.setString(1, username);
                try (ResultSet rsUser = psUser.executeQuery()) {
                    if (rsUser.next()) {
                        idUsuario = rsUser.getInt("id_usuario");
                    }
                }
            }
    
            if (idUsuario == -1) {
                response.put("books", readings);
                response.put("reading", "");
                return response;
            }
    // Configurar el idioma a español en MySQL
    String setLanguage = "SET lc_time_names = 'es_ES'";
    try (PreparedStatement psSetLang = connection.prepareStatement(setLanguage)) {
        psSetLang.execute();
    }            // Obtener el libro que el usuario tiene en estado "En progreso"
            String sql = "SELECT l.idLibro, l.titulo, l.autor, l.cover_image, " +
             "COALESCE(DATE_FORMAT(rl.fechaInicio, '%Y-%m-%d'), '') AS fechaInicio " +
             "FROM registrolectura rl " +
             "JOIN libros l ON rl.idLibro = l.idLibro " +
             "WHERE rl.estadoLectura = 'En progreso' AND rl.idUsuario = ? " +
             "ORDER BY rl.fechaInicio DESC";

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, idUsuario);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, String> book = new HashMap<>();
                        book.put("idLibro", String.valueOf(rs.getInt("idLibro")));
                        book.put("titulo", rs.getString("titulo"));
                        book.put("autor", rs.getString("autor"));
                        book.put("cover_image", rs.getString("cover_image"));
                        
                        String fechaInicio = rs.getString("fechaInicio");
                        book.put("fechaInicio", fechaInicio != null ? fechaInicio.trim() : "");
                        readings.add(book);
                    }

                    response.put("books", readings);
                    if (!readings.isEmpty()) {
                        response.putAll(readings.get(0));
                    } else {
                        response.put("reading", "");
                    }
                    
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Error al obtener el estado de lectura: " + e.getMessage());
            response.put("books", readings);
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
            String userQuery = "SELECT id_usuario FROM usuariolector WHERE nombre_usuario = ?";
            int idUsuario = -1;
            try (PreparedStatement psUser = connection.prepareStatement(userQuery)) {
                psUser.setString(1, username);
                try (ResultSet rsUser = psUser.executeQuery()) {
                    if (rsUser.next()) {
                        idUsuario = rsUser.getInt("id_usuario");
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
            String userQuery = "SELECT id_usuario FROM usuariolector WHERE nombre_usuario = ?";
            int idUsuario = -1;
            try (PreparedStatement psUser = connection.prepareStatement(userQuery)) {
                psUser.setString(1, username);
                try (ResultSet rsUser = psUser.executeQuery()) {
                    if (rsUser.next()) {
                        idUsuario = rsUser.getInt("id_usuario");
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

    @PostMapping("/deleteReadingEntry")
    @ResponseBody
    public Map<String, Object> deleteReadingEntry(
            @RequestParam("idLibro") int idLibro,
            @RequestParam("estadoLectura") String estadoLectura,
            Principal principal) {

        Map<String, Object> response = new HashMap<>();

        if (principal == null) {
            response.put("success", false);
            response.put("message", "Usuario no autenticado.");
            return response;
        }

        if (!estadoLectura.equals("Completado")
                && !estadoLectura.equals("En progreso")
                && !estadoLectura.equals("Próximas Lecturas")) {
            response.put("success", false);
            response.put("message", "Estado de lectura no válido.");
            return response;
        }

        String username = principal.getName();

        try (MySqlConnection db = new MySqlConnection()) {
            db.open();
            Connection connection = db.connection;

            String sql = "DELETE rl FROM registrolectura rl " +
                    "JOIN usuariolector u ON rl.idUsuario = u.id_usuario " +
                    "WHERE u.nombre_usuario = ? AND rl.idLibro = ? AND rl.estadoLectura = ?";

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setInt(2, idLibro);
                ps.setString(3, estadoLectura);
                response.put("success", ps.executeUpdate() > 0);
            }
        } catch (SQLException e) {
            response.put("success", false);
            response.put("message", "Error al eliminar el libro: " + e.getMessage());
        }

        return response;
    }
    
}
