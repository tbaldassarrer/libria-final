package es.prw.services;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import es.prw.repositories.FavoriteQuoteRepository;

@Component
public class FavoriteQuoteCleanupService implements ApplicationRunner {

    private static final String DEFAULT_TEXT_ONE =
            "Si el mundo desapareciera y el se salvara, yo seguiria viviendo. Pero si desapareciera el y lo demas continuara igual, yo no podria vivir.";
    private static final String DEFAULT_WORK_ONE = "Cumbres Borrascosas";

    private static final String DEFAULT_TEXT_TWO =
            "La rabia puede calentarte por la noche, y el orgullo herido puede alentar a un hombre a hacer cosas maravillosas.";
    private static final String DEFAULT_WORK_TWO = "El nombre del viento";

    private final FavoriteQuoteRepository favoriteQuoteRepository;

    public FavoriteQuoteCleanupService(FavoriteQuoteRepository favoriteQuoteRepository) {
        this.favoriteQuoteRepository = favoriteQuoteRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        long deletedOne = favoriteQuoteRepository.deleteByTextoAndObra(DEFAULT_TEXT_ONE, DEFAULT_WORK_ONE);
        long deletedTwo = favoriteQuoteRepository.deleteByTextoAndObra(DEFAULT_TEXT_TWO, DEFAULT_WORK_TWO);
        long totalDeleted = deletedOne + deletedTwo;

        if (totalDeleted > 0) {
            System.out.println("Limpieza de citas heredadas completada. Registros eliminados: " + totalDeleted);
        }
    }
}
