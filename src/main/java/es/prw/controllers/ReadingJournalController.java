package es.prw.controllers;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import es.prw.models.ReadingJournalEntry;
import es.prw.models.User;
import es.prw.repositories.ReadingJournalEntryRepository;
import es.prw.repositories.UserRepository;

@Controller
public class ReadingJournalController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReadingJournalEntryRepository readingJournalEntryRepository;

    @GetMapping("/readingJournal/entries")
    @ResponseBody
    public Map<String, Object> getEntries(Principal principal) {
        Map<String, Object> response = new HashMap<>();
        User user = getAuthenticatedUser(principal);

        if (user == null) {
            response.put("success", false);
            response.put("entries", new ArrayList<>());
            return response;
        }

        List<ReadingJournalEntry> entries = readingJournalEntryRepository
                .findByIdUsuarioOrderByUpdatedAtDescIdJournalDesc(user.getIdUsuario());
        response.put("success", true);
        response.put("entries", mapEntries(entries));
        return response;
    }

    @PostMapping("/readingJournal/save")
    @ResponseBody
    public Map<String, Object> saveEntry(
            @RequestParam(value = "idJournal", required = false) Integer idJournal,
            @RequestParam(value = "titulo", required = false) String titulo,
            @RequestParam(value = "autor", required = false) String autor,
            @RequestParam(value = "paginas", required = false) Integer paginas,
            @RequestParam(value = "formato", required = false) String formato,
            @RequestParam(value = "fechaInicio", required = false) String fechaInicio,
            @RequestParam(value = "fechaFin", required = false) String fechaFin,
            @RequestParam(value = "feelingOption", required = false) String feelingOption,
            @RequestParam(value = "generalRating", required = false) Integer generalRating,
            @RequestParam(value = "romanceRating", required = false) Integer romanceRating,
            @RequestParam(value = "spiceRating", required = false) Integer spiceRating,
            @RequestParam(value = "sadnessRating", required = false) Integer sadnessRating,
            @RequestParam(value = "plotRating", required = false) Integer plotRating,
            @RequestParam(value = "charactersRating", required = false) Integer charactersRating,
            @RequestParam(value = "styleRating", required = false) Integer styleRating,
            @RequestParam(value = "endingRating", required = false) Integer endingRating,
            @RequestParam(value = "bestCharacter", required = false) String bestCharacter,
            @RequestParam(value = "worstCharacter", required = false) String worstCharacter,
            @RequestParam(value = "reflexionesFinales", required = false) String reflexionesFinales,
            @RequestParam(value = "coverImage", required = false) String coverImage,
            Principal principal) {

        Map<String, Object> response = new HashMap<>();
        User user = getAuthenticatedUser(principal);

        if (user == null) {
            response.put("success", false);
            response.put("message", "Usuario no autenticado.");
            return response;
        }

        if (isBlankEntry(titulo, autor, paginas, fechaInicio, fechaFin, feelingOption, generalRating,
                romanceRating, spiceRating, sadnessRating, plotRating, charactersRating, styleRating, endingRating,
                bestCharacter, worstCharacter, reflexionesFinales, coverImage)) {
            response.put("success", false);
            response.put("message", "No hay datos para guardar.");
            return response;
        }

        ReadingJournalEntry entry = resolveEntry(user.getIdUsuario(), idJournal);
        if (entry == null) {
            response.put("success", false);
            response.put("message", "Reseña no encontrada.");
            return response;
        }

        entry.setIdUsuario(user.getIdUsuario());
        entry.setTitulo(normalize(titulo));
        entry.setAutor(normalize(autor));
        entry.setPaginas(paginas);
        entry.setFormato(normalize(formato));
        entry.setFechaInicio(parseDate(fechaInicio));
        entry.setFechaFin(parseDate(fechaFin));
        entry.setFeelingOption(normalize(feelingOption));
        entry.setGeneralRating(normalizeRating(generalRating));
        entry.setRomanceRating(normalizeRating(romanceRating));
        entry.setSpiceRating(normalizeRating(spiceRating));
        entry.setSadnessRating(normalizeRating(sadnessRating));
        entry.setPlotRating(normalizeRating(plotRating));
        entry.setCharactersRating(normalizeRating(charactersRating));
        entry.setStyleRating(normalizeRating(styleRating));
        entry.setEndingRating(normalizeRating(endingRating));
        entry.setBestCharacter(normalize(bestCharacter));
        entry.setWorstCharacter(normalize(worstCharacter));
        entry.setReflexionesFinales(normalizeLong(reflexionesFinales));
        entry.setCoverImage(normalizeLong(coverImage));

        ReadingJournalEntry saved = readingJournalEntryRepository.save(entry);

        response.put("success", true);
        response.put("message", "Reading Journal guardado.");
        response.put("entry", mapEntry(saved));
        response.put("entries", mapEntries(readingJournalEntryRepository
                .findByIdUsuarioOrderByUpdatedAtDescIdJournalDesc(user.getIdUsuario())));
        return response;
    }

    @PostMapping("/readingJournal/delete")
    @ResponseBody
    public Map<String, Object> deleteEntry(
            @RequestParam("idJournal") Integer idJournal,
            Principal principal) {
        Map<String, Object> response = new HashMap<>();
        User user = getAuthenticatedUser(principal);

        if (user == null) {
            response.put("success", false);
            response.put("message", "Usuario no autenticado.");
            return response;
        }

        ReadingJournalEntry entry = resolveEntry(user.getIdUsuario(), idJournal);
        if (entry == null || entry.getIdJournal() == null) {
            response.put("success", false);
            response.put("message", "Reseña no encontrada.");
            return response;
        }

        readingJournalEntryRepository.delete(entry);
        response.put("success", true);
        response.put("entries", mapEntries(readingJournalEntryRepository
                .findByIdUsuarioOrderByUpdatedAtDescIdJournalDesc(user.getIdUsuario())));
        return response;
    }

    private User getAuthenticatedUser(Principal principal) {
        if (principal == null) {
            return null;
        }
        return userRepository.findByNombreUsuario(principal.getName());
    }

    private ReadingJournalEntry resolveEntry(Integer idUsuario, Integer idJournal) {
        if (idJournal == null) {
            return new ReadingJournalEntry();
        }

        ReadingJournalEntry entry = readingJournalEntryRepository.findById(idJournal).orElse(null);
        if (entry == null || !idUsuario.equals(entry.getIdUsuario())) {
            return null;
        }
        return entry;
    }

    private List<Map<String, Object>> mapEntries(List<ReadingJournalEntry> entries) {
        List<Map<String, Object>> mapped = new ArrayList<>();
        for (ReadingJournalEntry entry : entries) {
            mapped.add(mapEntry(entry));
        }
        return mapped;
    }

    private Map<String, Object> mapEntry(ReadingJournalEntry entry) {
        Map<String, Object> item = new HashMap<>();
        item.put("idJournal", entry.getIdJournal());
        item.put("titulo", safe(entry.getTitulo()));
        item.put("autor", safe(entry.getAutor()));
        item.put("paginas", entry.getPaginas());
        item.put("formato", safe(entry.getFormato()));
        item.put("fechaInicio", entry.getFechaInicio() != null ? entry.getFechaInicio().toString() : "");
        item.put("fechaFin", entry.getFechaFin() != null ? entry.getFechaFin().toString() : "");
        item.put("feelingOption", safe(entry.getFeelingOption()));
        item.put("generalRating", safeRating(entry.getGeneralRating()));
        item.put("romanceRating", safeRating(entry.getRomanceRating()));
        item.put("spiceRating", safeRating(entry.getSpiceRating()));
        item.put("sadnessRating", safeRating(entry.getSadnessRating()));
        item.put("plotRating", safeRating(entry.getPlotRating()));
        item.put("charactersRating", safeRating(entry.getCharactersRating()));
        item.put("styleRating", safeRating(entry.getStyleRating()));
        item.put("endingRating", safeRating(entry.getEndingRating()));
        item.put("bestCharacter", safe(entry.getBestCharacter()));
        item.put("worstCharacter", safe(entry.getWorstCharacter()));
        item.put("reflexionesFinales", safe(entry.getReflexionesFinales()));
        item.put("coverImage", safe(entry.getCoverImage()));
        item.put("updatedAt", entry.getUpdatedAt() != null ? entry.getUpdatedAt().toString() : "");
        return item;
    }

    private boolean isBlankEntry(String titulo, String autor, Integer paginas, String fechaInicio, String fechaFin,
            String feelingOption, Integer generalRating, Integer romanceRating, Integer spiceRating,
            Integer sadnessRating, Integer plotRating, Integer charactersRating, Integer styleRating,
            Integer endingRating, String bestCharacter, String worstCharacter, String reflexionesFinales,
            String coverImage) {
        return normalize(titulo).isEmpty()
                && normalize(autor).isEmpty()
                && paginas == null
                && normalize(fechaInicio).isEmpty()
                && normalize(fechaFin).isEmpty()
                && normalize(feelingOption).isEmpty()
                && normalizeRating(generalRating) == 0
                && normalizeRating(romanceRating) == 0
                && normalizeRating(spiceRating) == 0
                && normalizeRating(sadnessRating) == 0
                && normalizeRating(plotRating) == 0
                && normalizeRating(charactersRating) == 0
                && normalizeRating(styleRating) == 0
                && normalizeRating(endingRating) == 0
                && normalize(bestCharacter).isEmpty()
                && normalize(worstCharacter).isEmpty()
                && normalizeLong(reflexionesFinales).isEmpty()
                && normalizeLong(coverImage).isEmpty();
    }

    private java.time.LocalDate parseDate(String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return null;
        }
        return java.time.LocalDate.parse(normalized);
    }

    private int normalizeRating(Integer rating) {
        if (rating == null) {
            return 0;
        }
        return Math.max(0, Math.min(rating, 5));
    }

    private int safeRating(Integer rating) {
        return rating != null ? rating : 0;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeLong(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private String safe(String value) {
        return value != null ? value : "";
    }
}
