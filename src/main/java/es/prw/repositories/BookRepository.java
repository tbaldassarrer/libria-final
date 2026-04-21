package es.prw.repositories;

import es.prw.models.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer> {

    // Buscar libros por título que empiecen con la query
    @Query("SELECT b.titulo FROM Book b WHERE b.titulo LIKE :query% ORDER BY b.titulo ASC")
    List<String> findTitulosStartingWith(@Param("query") String query);

    // Buscar libro por título exacto
    Book findByTitulo(String titulo);
}