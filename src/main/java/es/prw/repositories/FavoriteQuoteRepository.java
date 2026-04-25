package es.prw.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import es.prw.models.FavoriteQuote;

@Repository
public interface FavoriteQuoteRepository extends JpaRepository<FavoriteQuote, Integer> {

    List<FavoriteQuote> findByIdUsuarioOrderByOrdenVisualAscIdCitaAsc(Integer idUsuario);

    List<FavoriteQuote> findByIdUsuarioOrderByOrdenVisualDescIdCitaDesc(Integer idUsuario);

    @Modifying
    @Transactional
    long deleteByTextoAndObra(String texto, String obra);
}
