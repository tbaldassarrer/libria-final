package es.prw.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import es.prw.models.ReadingJournalEntry;

@Repository
public interface ReadingJournalEntryRepository extends JpaRepository<ReadingJournalEntry, Integer> {

    List<ReadingJournalEntry> findByIdUsuarioOrderByUpdatedAtDescIdJournalDesc(Integer idUsuario);
}
