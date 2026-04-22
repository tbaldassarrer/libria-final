package es.prw.controllers;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import es.prw.models.Book;
import es.prw.repositories.BookRepository;
import es.prw.services.GoogleBooksService;

@Controller
public class SearchBookController {

    private static final int MAX_SUGGESTIONS = 10;
    private static final String DEFAULT_COVER = "/images/portadaLibro.jpg";

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private GoogleBooksService googleBooksService;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/searchBooks")
    @ResponseBody
    public List<String> searchBooks(@RequestParam("query") String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return bookRepository.findTitulosStartingWith(query.trim())
                    .stream()
                    .limit(MAX_SUGGESTIONS)
                    .toList();
        } catch (Exception e) {
            System.out.println("Error al buscar libros en BD: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @GetMapping("/getBookDetails")
    @ResponseBody
    public Map<String, String> getBookDetails(@RequestParam("title") String title) {
        Map<String, String> bookDetails = new HashMap<>();

        if (title == null || title.trim().isEmpty()) {
            return bookDetails;
        }

        try {
            Book book = bookRepository.findFirstByTituloIgnoreCase(title.trim());
            if (book == null) {
                book = googleBooksService.findOrCreateByTitle(title.trim());
            }

            if (book != null) {
                bookDetails.put("titulo", book.getTitulo());
                bookDetails.put("autor", book.getAutor());
                bookDetails.put("genero", book.getGenero());
                bookDetails.put("anioEdicion", book.getAnioEdicion());
                bookDetails.put("sinopsis", book.getSinopsis());
                bookDetails.put("puntuacion", book.getPuntuacion());
                bookDetails.put("cover_image", book.getCoverImage());
            }
        } catch (Exception e) {
            System.out.println("Error al obtener detalles del libro: " + e.getMessage());
        }

        return bookDetails;
    }

    @GetMapping("/exploreBooks")
    @ResponseBody
    public List<Map<String, String>> exploreBooks(@RequestParam("genre") String genre) {
        return searchGoogleBooksByGenre(genre, 5);
    }

    private List<String> searchGoogleBookTitles(String query, int limit) {
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

    private Book findAndSaveGoogleBook(String title) {
        JsonNode items = getGoogleBooks("intitle:" + title, 1);

        if (items == null || !items.isArray() || items.isEmpty()) {
            return null;
        }

        JsonNode info = items.get(0).path("volumeInfo");
        String googleTitle = text(info.path("title"), title);
        Book existingBook = bookRepository.findFirstByTituloIgnoreCase(googleTitle);
        if (existingBook != null) {
            return existingBook;
        }

        Book book = new Book(
                truncate(googleTitle, 255),
                truncate(joinAuthors(info.path("authors")), 255),
                truncate(firstValue(info.path("categories"), "Sin genero"), 255),
                truncate(yearFromDate(text(info.path("publishedDate"), "Desconocido")), 255),
                truncate(text(info.path("description"), "Sin descripcion disponible."), 2000),
                truncate(text(info.path("averageRating"), "N/A"), 255),
                truncate(coverFrom(info.path("imageLinks")), 255));

        return bookRepository.save(book);
    }

    private List<Map<String, String>> searchGoogleBooksByGenre(String genre, int limit) {
        List<Map<String, String>> books = new ArrayList<>();
        JsonNode items = getGoogleBooks(genreQuery(genre), limit);

        if (items == null || !items.isArray()) {
            return books;
        }

        for (JsonNode item : items) {
            JsonNode info = item.path("volumeInfo");
            Map<String, String> book = new HashMap<>();
            book.put("titulo", text(info.path("title"), "Titulo desconocido"));
            book.put("autor", joinAuthors(info.path("authors")));
            book.put("genero", firstValue(info.path("categories"), displayGenre(genre)));
            book.put("anioEdicion", yearFromDate(text(info.path("publishedDate"), "Desconocido")));
            book.put("sinopsis", text(info.path("description"), "Sin descripcion disponible."));
            book.put("puntuacion", text(info.path("averageRating"), "N/A"));
            book.put("cover_image", coverFrom(info.path("imageLinks")));
            books.add(book);
        }

        return books;
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

    private String genreQuery(String genre) {
        return switch (genre) {
            case "fantasy" -> "fantasia fantasy";
            case "romance" -> "romance";
            case "science_fiction" -> "ciencia ficcion science fiction";
            case "mystery" -> "misterio thriller";
            case "history" -> "historia history";
            default -> genre;
        };
    }

    private String displayGenre(String genre) {
        return switch (genre) {
            case "fantasy" -> "Fantasia";
            case "science_fiction" -> "Ciencia Ficcion";
            case "mystery" -> "Misterio";
            case "history" -> "Historia";
            default -> genre;
        };
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

    private String yearFromDate(String publishedDate) {
        if (publishedDate == null || publishedDate.isBlank() || publishedDate.length() < 4) {
            return "Desconocido";
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
