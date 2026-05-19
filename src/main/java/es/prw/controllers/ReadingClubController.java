package es.prw.controllers;

import java.security.Principal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ReadingClubController {

    private static final String DEFAULT_COVER = "/images/portadaLibro.jpg";

    private final JdbcTemplate jdbcTemplate;

    public ReadingClubController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/clubes")
    public String getReadingClubs(Model model, Principal principal) {
        model.addAttribute("username", principal != null ? principal.getName() : "");
        return "clubes";
    }

    @GetMapping("/readingClubs/data")
    @ResponseBody
    public Map<String, Object> getReadingClubData(Principal principal) {
        Map<String, Object> response = new HashMap<>();
        UserRef user = getUser(principal);
        response.put("success", user != null);
        response.put("books", getBooks());
        response.put("clubs", getClubs(user != null ? user.idUsuario() : null));
        response.put("username", user != null ? user.username() : "");
        return response;
    }

    @PostMapping("/readingClubs/create")
    @ResponseBody
    public Map<String, Object> createClub(
            @RequestParam("idLibro") int idLibro,
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "currentChapter", required = false, defaultValue = "1") int currentChapter,
            Principal principal) {
        Map<String, Object> response = new HashMap<>();
        UserRef user = getUser(principal);
        if (user == null) {
            response.put("success", false);
            response.put("message", "Usuario no autenticado.");
            return response;
        }

        String cleanName = normalize(name);
        if (cleanName.isEmpty()) {
            response.put("success", false);
            response.put("message", "El nombre del club es obligatorio.");
            return response;
        }

        if (!bookExists(idLibro)) {
            response.put("success", false);
            response.put("message", "Selecciona un libro de Libria.");
            return response;
        }

        jdbcTemplate.update("""
                INSERT INTO reading_club (id_libro, owner_user_id, name, description, current_chapter)
                VALUES (?, ?, ?, ?, ?)
                """,
                idLibro,
                user.idUsuario(),
                cleanName,
                normalize(description),
                Math.max(1, currentChapter));

        Integer clubId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
        if (clubId != null) {
            jdbcTemplate.update("INSERT IGNORE INTO reading_club_member (id_club, id_usuario) VALUES (?, ?)",
                    clubId,
                    user.idUsuario());
        }

        response.put("success", true);
        response.put("clubs", getClubs(user.idUsuario()));
        return response;
    }

    @PostMapping("/readingClubs/join")
    @ResponseBody
    public Map<String, Object> joinClub(@RequestParam("idClub") int idClub, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        UserRef user = getUser(principal);
        if (user == null) {
            response.put("success", false);
            response.put("message", "Usuario no autenticado.");
            return response;
        }

        jdbcTemplate.update("INSERT IGNORE INTO reading_club_member (id_club, id_usuario) VALUES (?, ?)",
                idClub,
                user.idUsuario());
        response.put("success", true);
        response.put("clubs", getClubs(user.idUsuario()));
        return response;
    }

    @PostMapping("/readingClubs/leave")
    @ResponseBody
    public Map<String, Object> leaveClub(@RequestParam("idClub") int idClub, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        UserRef user = getUser(principal);
        if (user == null) {
            response.put("success", false);
            response.put("message", "Usuario no autenticado.");
            return response;
        }

        jdbcTemplate.update("""
                DELETE FROM reading_club_member
                WHERE id_club = ? AND id_usuario = ? AND EXISTS (
                    SELECT 1 FROM reading_club WHERE id_club = ? AND owner_user_id <> ?
                )
                """,
                idClub,
                user.idUsuario(),
                idClub,
                user.idUsuario());
        response.put("success", true);
        response.put("clubs", getClubs(user.idUsuario()));
        return response;
    }

    @GetMapping("/readingClubs/comments")
    @ResponseBody
    public Map<String, Object> getComments(@RequestParam("idClub") int idClub) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("comments", getClubComments(idClub));
        return response;
    }

    @PostMapping("/readingClubs/comments")
    @ResponseBody
    public Map<String, Object> createComment(
            @RequestParam("idClub") int idClub,
            @RequestParam("chapterNumber") int chapterNumber,
            @RequestParam("commentText") String commentText,
            Principal principal) {
        Map<String, Object> response = new HashMap<>();
        UserRef user = getUser(principal);
        if (user == null) {
            response.put("success", false);
            response.put("message", "Usuario no autenticado.");
            return response;
        }

        String text = normalize(commentText);
        if (text.isEmpty()) {
            response.put("success", false);
            response.put("message", "El comentario no puede estar vacio.");
            return response;
        }

        jdbcTemplate.update("INSERT IGNORE INTO reading_club_member (id_club, id_usuario) VALUES (?, ?)",
                idClub,
                user.idUsuario());
        jdbcTemplate.update("""
                INSERT INTO reading_club_comment (id_club, id_usuario, chapter_number, comment_text)
                VALUES (?, ?, ?, ?)
                """,
                idClub,
                user.idUsuario(),
                Math.max(1, chapterNumber),
                text);

        response.put("success", true);
        response.put("comments", getClubComments(idClub));
        response.put("clubs", getClubs(user.idUsuario()));
        return response;
    }

    private List<Map<String, Object>> getBooks() {
        return jdbcTemplate.query("""
                SELECT idLibro, titulo, autor, cover_image
                FROM libros
                ORDER BY titulo ASC
                LIMIT 120
                """,
                (rs, rowNum) -> {
                    Map<String, Object> book = new HashMap<>();
                    book.put("idLibro", rs.getInt("idLibro"));
                    book.put("titulo", safe(rs.getString("titulo")));
                    book.put("autor", safe(rs.getString("autor")));
                    book.put("coverImage", cover(rs.getString("cover_image")));
                    return book;
                });
    }

    private List<Map<String, Object>> getClubs(Integer currentUserId) {
        return jdbcTemplate.query("""
                SELECT rc.id_club, rc.name, rc.description, rc.current_chapter, rc.created_at,
                       l.idLibro, l.titulo, l.autor, l.cover_image,
                       owner_names.username AS owner_username,
                       COUNT(DISTINCT rcm.id_usuario) AS member_count,
                       COUNT(DISTINCT rcc.id_comment) AS comment_count,
                       MAX(CASE WHEN rcm_current.id_usuario IS NULL THEN 0 ELSE 1 END) AS joined
                FROM reading_club rc
                JOIN libros l ON l.idLibro = rc.id_libro
                LEFT JOIN (
                    SELECT COALESCE(id_usuario, idUsuario) AS user_id,
                           COALESCE(NULLIF(nombre_usuario, ''), nombreUsuario) AS username
                    FROM usuariolector
                ) owner_names ON owner_names.user_id = rc.owner_user_id
                LEFT JOIN reading_club_member rcm ON rcm.id_club = rc.id_club
                LEFT JOIN reading_club_member rcm_current ON rcm_current.id_club = rc.id_club
                    AND rcm_current.id_usuario = ?
                LEFT JOIN reading_club_comment rcc ON rcc.id_club = rc.id_club
                GROUP BY rc.id_club, rc.name, rc.description, rc.current_chapter, rc.created_at,
                         l.idLibro, l.titulo, l.autor, l.cover_image, owner_names.username
                ORDER BY rc.created_at DESC, rc.id_club DESC
                """,
                (rs, rowNum) -> mapClub(rs),
                currentUserId == null ? -1 : currentUserId);
    }

    private List<Map<String, Object>> getClubComments(int idClub) {
        return jdbcTemplate.query("""
                SELECT rcc.id_comment, rcc.chapter_number, rcc.comment_text, rcc.created_at,
                       COALESCE(NULLIF(u.nombre_usuario, ''), u.nombreUsuario, 'Lector') AS username
                FROM reading_club_comment rcc
                LEFT JOIN usuariolector u ON COALESCE(u.id_usuario, u.idUsuario) = rcc.id_usuario
                WHERE rcc.id_club = ?
                ORDER BY rcc.chapter_number ASC, rcc.created_at ASC, rcc.id_comment ASC
                """,
                (rs, rowNum) -> {
                    Map<String, Object> comment = new HashMap<>();
                    comment.put("idComment", rs.getInt("id_comment"));
                    comment.put("chapterNumber", rs.getInt("chapter_number"));
                    comment.put("commentText", safe(rs.getString("comment_text")));
                    comment.put("createdAt", safe(rs.getString("created_at")));
                    comment.put("username", safe(rs.getString("username")));
                    return comment;
                },
                idClub);
    }

    private Map<String, Object> mapClub(ResultSet rs) throws SQLException {
        Map<String, Object> club = new HashMap<>();
        club.put("idClub", rs.getInt("id_club"));
        club.put("name", safe(rs.getString("name")));
        club.put("description", safe(rs.getString("description")));
        club.put("currentChapter", rs.getInt("current_chapter"));
        club.put("createdAt", safe(rs.getString("created_at")));
        club.put("idLibro", rs.getInt("idLibro"));
        club.put("titulo", safe(rs.getString("titulo")));
        club.put("autor", safe(rs.getString("autor")));
        club.put("coverImage", cover(rs.getString("cover_image")));
        club.put("ownerUsername", safe(rs.getString("owner_username")));
        club.put("memberCount", rs.getInt("member_count"));
        club.put("commentCount", rs.getInt("comment_count"));
        club.put("joined", rs.getInt("joined") == 1);
        return club;
    }

    private UserRef getUser(Principal principal) {
        if (principal == null) {
            return null;
        }

        List<UserRef> users = jdbcTemplate.query("""
                SELECT COALESCE(id_usuario, idUsuario) AS id_usuario,
                       COALESCE(NULLIF(nombre_usuario, ''), nombreUsuario) AS username
                FROM usuariolector
                WHERE nombre_usuario = ? OR nombreUsuario = ?
                LIMIT 1
                """,
                (rs, rowNum) -> new UserRef(rs.getInt("id_usuario"), rs.getString("username")),
                principal.getName(),
                principal.getName());

        return users.isEmpty() ? null : users.get(0);
    }

    private boolean bookExists(int idLibro) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM libros WHERE idLibro = ?",
                Integer.class,
                idLibro);
        return count != null && count > 0;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String cover(String value) {
        return value == null || value.isBlank() ? DEFAULT_COVER : value;
    }

    private record UserRef(int idUsuario, String username) {
    }
}
