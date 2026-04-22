package es.prw.services;

import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import es.prw.dao.MySqlConnection;
import es.prw.models.Book;
import es.prw.repositories.BookRepository;

@Service
public class GoogleBooksService {

    private static final String DEFAULT_COVER = "/images/portadaLibro.jpg";

    @Autowired
    private BookRepository bookRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    public Book findOrCreateByTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return null;
        }

        String cleanTitle = title.trim();
        Book existingBook = bookRepository.findFirstByTituloIgnoreCase(cleanTitle);
        if (existingBook != null) {
            return existingBook;
        }

        JsonNode items = getGoogleBooks("intitle:" + cleanTitle, 1);
        if (items == null || !items.isArray() || items.isEmpty()) {
            return null;
        }

        JsonNode info = items.get(0).path("volumeInfo");
        String googleTitle = text(info.path("title"), cleanTitle);

        existingBook = bookRepository.findFirstByTituloIgnoreCase(googleTitle);
        if (existingBook != null) {
            return existingBook;
        }

        String author = truncate(joinAuthors(info.path("authors")), 255);
        String genre = truncate(firstValue(info.path("categories"), "Sin genero"), 255);
        String year = truncate(yearFromDate(text(info.path("publishedDate"), "")), 255);
        String synopsis = truncate(text(info.path("description"), "Sin descripcion disponible."), 2000);
        String rating = truncate(text(info.path("averageRating"), "N/A"), 255);
        String cover = truncate(coverFrom(info.path("imageLinks")), 255);
        String isbn = truncate(isbnFrom(info.path("industryIdentifiers")), 255);

        Book book = new Book(
                truncate(googleTitle, 255),
                author,
                genre,
                year,
                synopsis,
                rating,
                cover);

        try {
            return bookRepository.save(book);
        } catch (Exception e) {
            System.out.println("No se pudo guardar con JPA; probando insercion JDBC: " + e.getMessage());
            return insertBookWithJdbc(isbn, googleTitle, author, genre, year, synopsis, rating, cover);
        }
    }

    public List<String> searchTitles(String query, int limit) {
        List<String> titles = new ArrayList<>();
        JsonNode items = getGoogleBooks(query, limit);

        if (items == null || !items.isArray()) {
            return titles;
        }

        for (JsonNode item : items) {
            String title = text(item.path("volumeInfo").path("title"), "");
            if (!title.isBlank()) {
                titles.add(title);
            }
        }

        return titles;
    }

    private JsonNode getGoogleBooks(String query, int limit) {
        URI uri = UriComponentsBuilder
                .fromUriString("https://www.googleapis.com/books/v1/volumes")
                .queryParam("q", query)
                .queryParam("langRestrict", "es")
                .queryParam("orderBy", "relevance")
                .queryParam("maxResults", Math.max(1, limit))
                .build()
                .encode()
                .toUri();

        try {
            JsonNode response = restTemplate.getForObject(uri, JsonNode.class);
            return response == null ? null : response.path("items");
        } catch (Exception e) {
            System.out.println("Error consultando Google Books: " + e.getMessage());
            return null;
        }
    }

    private String text(JsonNode node, String fallback) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return fallback;
        }

        String value = node.asText();
        return value == null || value.isBlank() ? fallback : value;
    }

    private String joinAuthors(JsonNode authors) {
        if (authors == null || !authors.isArray() || authors.isEmpty()) {
            return "Autor desconocido";
        }

        List<String> names = new ArrayList<>();
        for (JsonNode author : authors) {
            names.add(author.asText());
        }
        return String.join(", ", names);
    }

    private String firstValue(JsonNode values, String fallback) {
        if (values == null || !values.isArray() || values.isEmpty()) {
            return fallback;
        }
        return text(values.get(0), fallback);
    }

    private String isbnFrom(JsonNode identifiers) {
        if (identifiers == null || !identifiers.isArray() || identifiers.isEmpty()) {
            return "";
        }

        String firstIdentifier = "";
        for (JsonNode identifier : identifiers) {
            String type = text(identifier.path("type"), "");
            String value = text(identifier.path("identifier"), "");

            if (firstIdentifier.isBlank()) {
                firstIdentifier = value;
            }
            if ("ISBN_13".equalsIgnoreCase(type) && !value.isBlank()) {
                return value;
            }
        }

        return firstIdentifier;
    }

    private Book insertBookWithJdbc(
            String isbn,
            String title,
            String author,
            String genre,
            String year,
            String synopsis,
            String rating,
            String cover) {

        try (MySqlConnection db = new MySqlConnection()) {
            db.open();
            Connection connection = db.connection;

            String sql = "INSERT INTO libros (isbn, titulo, autor, genero, anioEdicion, sinopsis, puntuacion, cover_image) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, normalizeIsbnForDatabase(isbn, title));
                ps.setString(2, truncate(title, 255));
                ps.setString(3, author);
                ps.setString(4, genre);
                if (year == null || year.isBlank() || !year.matches("\\d{4}")) {
                    ps.setNull(5, Types.INTEGER);
                } else {
                    ps.setInt(5, Integer.parseInt(year));
                }
                ps.setString(6, synopsis);
                ps.setString(7, rating);
                ps.setString(8, cover);
                ps.executeUpdate();
            }

            return bookRepository.findFirstByTituloIgnoreCase(title);
        } catch (SQLException e) {
            System.out.println("No se pudo insertar el libro de Google Books: " + e.getMessage());
            return bookRepository.findFirstByTituloIgnoreCase(title);
        }
    }

    private String normalizeIsbnForDatabase(String isbn, String title) {
        String digits = isbn == null ? "" : isbn.replaceAll("\\D", "");
        if (!digits.isBlank()) {
            return truncate(digits, 255);
        }

        long hash = Math.abs((long) title.toLowerCase().hashCode());
        return String.format("9%012d", hash % 1_000_000_000_000L);
    }

    private String yearFromDate(String publishedDate) {
        if (publishedDate == null || publishedDate.isBlank() || publishedDate.length() < 4) {
            return "";
        }
        return publishedDate.substring(0, 4);
    }

    private String coverFrom(JsonNode imageLinks) {
        String cover = text(imageLinks.path("thumbnail"), DEFAULT_COVER);
        return cover.replace("http://", "https://");
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
