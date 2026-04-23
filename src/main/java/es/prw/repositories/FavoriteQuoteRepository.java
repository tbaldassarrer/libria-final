package es.prw.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import es.prw.models.FavoriteQuote;

@Repository
public interface FavoriteQuoteRepository extends JpaRepository<FavoriteQuote, Integer> {

    List<FavoriteQuote> findByIdUsuarioOrderByOrdenVisualAscIdCitaAsc(Integer idUsuario);

    List<FavoriteQuote> findByIdUsuarioOrderByOrdenVisualDescIdCitaDesc(Integer idUsuario);
}
