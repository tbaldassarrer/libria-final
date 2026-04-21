package es.prw.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import es.prw.models.Book;
import es.prw.repositories.BookRepository;

@Controller
public class SearchBookController {

    @Autowired
    private BookRepository bookRepository;

    @GetMapping("/searchBooks")
    @ResponseBody
    public List<String> searchBooks(@RequestParam("query") String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return bookRepository.findTitulosStartingWith(query);
        } catch (Exception e) {
            System.out.println("❌ Error al buscar libros: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @GetMapping("/getBookDetails")
    @ResponseBody
    public Map<String, String> getBookDetails(@RequestParam("title") String title) {
        Map<String, String> bookDetails = new HashMap<>();

        try {
            Book book = bookRepository.findByTitulo(title);
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
            System.out.println("❌ Error al obtener detalles del libro: " + e.getMessage());
        }

        return bookDetails;
    }
}
