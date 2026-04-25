package es.prw.services;

import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.text.Normalizer;

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

        JsonNode items = firstAvailableGoogleResults(
                "intitle:" + cleanTitle,
                cleanTitle);
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

    public Book findOrCreateByQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return null;
        }

        String cleanQuery = query.trim();
        Book existingBook = bookRepository.findFirstByTituloIgnoreCase(cleanQuery);
        if (existingBook != null) {
            return existingBook;
        }

        JsonNode items = firstAvailableGoogleResults(cleanQuery, "inauthor:" + cleanQuery, "intitle:" + cleanQuery);
        if (items == null || !items.isArray() || items.isEmpty()) {
            return null;
        }

        String bestTitle = text(items.get(0).path("volumeInfo").path("title"), cleanQuery);
        return findOrCreateByTitle(bestTitle);
    }

    public Book findOrCreateByGoogleId(String googleId) {
        if (googleId == null || googleId.trim().isEmpty()) {
            return null;
        }

        URI uri = UriComponentsBuilder
                .fromUriString("https://www.googleapis.com/books/v1/volumes/{id}")
                .buildAndExpand(googleId.trim())
                .encode()
                .toUri();

        try {
            JsonNode item = restTemplate.getForObject(uri, JsonNode.class);
            if (item == null || item.isMissingNode() || item.isNull()) {
                return null;
            }

            if (!isSpanishVolume(item.path("volumeInfo"))) {
                return null;
            }

            return saveGoogleBook(item);
        } catch (Exception e) {
            System.out.println("Error consultando Google Books por id: " + e.getMessage());
            return null;
        }
    }

    public List<String> searchTitles(String query, int limit) {
        Set<String> titles = new LinkedHashSet<>();
        collectTitles(titles, getGoogleBooks(query, limit, true, "relevance"));
        collectTitles(titles, getGoogleBooks(query, limit, true, "newest"));

        return titles.stream()
                .limit(Math.max(1, limit))
                .toList();
    }

    public List<Map<String, String>> searchSuggestions(String query, int limit) {
        List<Map<String, String>> suggestions = new ArrayList<>();
        Set<String> seenTitles = new LinkedHashSet<>();
        String cleanQuery = query == null ? "" : query.trim();
        int fetchLimit = Math.max(limit * 2, 10);

        collectSuggestions(suggestions, seenTitles, cleanQuery,
                getGoogleBooks("inauthor:" + cleanQuery, fetchLimit, true, "relevance"), limit);
        collectSuggestions(suggestions, seenTitles, cleanQuery,
                getGoogleBooks("intitle:" + cleanQuery, fetchLimit, true, "relevance"), limit);
        collectSuggestions(suggestions, seenTitles, cleanQuery,
                getGoogleBooks(cleanQuery, fetchLimit, true, "relevance"), limit);
        collectSuggestions(suggestions, seenTitles, cleanQuery,
                getGoogleBooks(cleanQuery, fetchLimit, true, "newest"), limit);

        return suggestions.stream()
                .limit(Math.max(1, limit))
                .toList();
    }

    private JsonNode getGoogleBooks(String query, int limit) {
        return getGoogleBooks(query, limit, true, "relevance");
    }

    private JsonNode getGoogleBooks(String query, int limit, boolean restrictToSpanish, String orderBy) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString("https://www.googleapis.com/books/v1/volumes")
                .queryParam("q", query)
                .queryParam("printType", "books")
                .queryParam("orderBy", orderBy)
                .queryParam("maxResults", Math.max(1, limit));

        if (restrictToSpanish) {
            builder.queryParam("langRestrict", "es");
        }

        URI uri = builder.build().encode().toUri();

        try {
            JsonNode response = restTemplate.getForObject(uri, JsonNode.class);
            return response == null ? null : response.path("items");
        } catch (Exception e) {
            System.out.println("Error consultando Google Books: " + e.getMessage());
            return null;
        }
    }

    private JsonNode firstAvailableGoogleResults(String... queries) {
        for (String query : queries) {
            JsonNode items = getGoogleBooks(query, 5, true, "relevance");
            if (hasSpanishItems(items)) {
                return items;
            }

            items = getGoogleBooks(query, 5, true, "newest");
            if (hasSpanishItems(items)) {
                return items;
            }
        }

        return null;
    }

    private void collectTitles(Set<String> titles, JsonNode items) {
        if (items == null || !items.isArray()) {
            return;
        }

        for (JsonNode item : items) {
            JsonNode info = item.path("volumeInfo");
            if (!isSpanishVolume(info)) {
                continue;
            }

            String title = text(info.path("title"), "");
            if (!title.isBlank()) {
                titles.add(title);
            }
        }
    }

    private void collectSuggestions(
            List<Map<String, String>> suggestions,
            Set<String> seenTitles,
            String query,
            JsonNode items,
            int limit) {

        if (items == null || !items.isArray() || suggestions.size() >= limit) {
            return;
        }

        for (JsonNode item : items) {
            if (suggestions.size() >= limit) {
                return;
            }

            JsonNode info = item.path("volumeInfo");
            if (!isSpanishVolume(info)) {
                continue;
            }

            String title = text(info.path("title"), "");
            String author = joinAuthors(info.path("authors"));

            if (!matchesSuggestionQuery(title, author, query)) {
                continue;
            }

            if (title.isBlank() || !seenTitles.add(title.toLowerCase())) {
                continue;
            }

            Map<String, String> suggestion = new HashMap<>();
            suggestion.put("googleId", text(item.path("id"), ""));
            suggestion.put("titulo", title);
            suggestion.put("autor", author);
            suggestion.put("anioEdicion", yearFromDate(text(info.path("publishedDate"), "")));
            suggestion.put("cover_image", coverFrom(info.path("imageLinks")));
            suggestion.put("source", "Google Books");
            suggestions.add(suggestion);
        }
    }

    private boolean matchesSuggestionQuery(String title, String author, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        List<String> words = extractWords(title + " " + author);
        List<String> tokens = Arrays.stream(normalizeForMatch(query).split("\\s+"))
                .map(String::trim)
                .filter(token -> token.length() >= 2)
                .toList();

        if (tokens.isEmpty()) {
            String normalizedQuery = normalizeForMatch(query);
            return words.stream().anyMatch(word -> word.startsWith(normalizedQuery));
        }

        for (String token : tokens) {
            boolean matchesToken = words.stream().anyMatch(word -> word.startsWith(token));
            if (!matchesToken) {
                return false;
            }
        }

        return true;
    }

    private List<String> extractWords(String value) {
        String normalized = normalizeForMatch(value);
        if (normalized.isBlank()) {
            return List.of();
        }

        return Arrays.stream(normalized.split("[^\\p{IsAlphabetic}\\p{IsDigit}]+"))
                .map(String::trim)
                .filter(word -> !word.isBlank())
                .toList();
    }

    private String normalizeForMatch(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return normalized.toLowerCase(Locale.ROOT).trim();
    }

    private Book saveGoogleBook(JsonNode item) {
        JsonNode info = item.path("volumeInfo");
        if (!isSpanishVolume(info)) {
            return null;
        }

        String googleTitle = text(info.path("title"), "");
        if (googleTitle.isBlank()) {
            return null;
        }

        Book existingBook = bookRepository.findFirstByTituloIgnoreCase(googleTitle);
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

    private String text(JsonNode node, String fallback) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return fallback;
        }

        String value = node.asText();
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean hasSpanishItems(JsonNode items) {
        if (items == null || !items.isArray() || items.isEmpty()) {
            return false;
        }

        for (JsonNode item : items) {
            if (isSpanishVolume(item.path("volumeInfo"))) {
                return true;
            }
        }

        return false;
    }

    private boolean isSpanishVolume(JsonNode info) {
        String language = text(info.path("language"), "");
        return "es".equalsIgnoreCase(language);
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
