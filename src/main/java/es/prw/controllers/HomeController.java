package es.prw.controllers;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import es.prw.models.FavoriteQuote;
import es.prw.models.User;
import es.prw.repositories.FavoriteQuoteRepository;
import es.prw.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class HomeController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FavoriteQuoteRepository favoriteQuoteRepository;

    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String home(Model model, Principal principal) {
        if (principal != null) {
            String username = principal.getName();
            model.addAttribute("username", username);
            User user = userRepository.findByNombreUsuario(username);
            model.addAttribute("readingGoal", user != null ? user.getDesafioLectura() : 10);
            model.addAttribute("favoriteQuotesPreview", user != null ? getQuotePreview(user) : defaultQuotePreview());
        } else {
            model.addAttribute("username", "Invitado");
            model.addAttribute("readingGoal", 10);
            model.addAttribute("favoriteQuotesPreview", defaultQuotePreview());
        }
        return "home"; // Se renderiza home.html en src/main/resources/templates
    }

    @GetMapping("/favoriteQuotes")
    @ResponseBody
    public Map<String, Object> getFavoriteQuotes(Principal principal) {
        Map<String, Object> response = new HashMap<>();
        User user = getAuthenticatedUser(principal);

        if (user == null) {
            response.put("success", false);
            response.put("quotes", new ArrayList<>());
            return response;
        }

        response.put("success", true);
        response.put("quotes", mapQuotes(getQuotesForDisplay(user)));
        return response;
    }

    @PostMapping("/favoriteQuotes")
    @ResponseBody
    public Map<String, Object> createFavoriteQuote(
            @RequestParam("texto") String texto,
            @RequestParam("obra") String obra,
            Principal principal) {
        Map<String, Object> response = new HashMap<>();
        User user = getAuthenticatedUser(principal);

        if (user == null) {
            response.put("success", false);
            response.put("message", "Usuario no autenticado.");
            return response;
        }

        String normalizedText = normalizeQuoteText(texto);
        String normalizedWork = normalizeWork(obra);
        if (normalizedText.isEmpty() || normalizedWork.isEmpty()) {
            response.put("success", false);
            response.put("message", "La cita y la obra son obligatorias.");
            return response;
        }

        int nextOrder = ensureDefaultQuotes(user).size();
        favoriteQuoteRepository.save(new FavoriteQuote(user.getIdUsuario(), normalizedText, normalizedWork, nextOrder));

        response.put("success", true);
        response.put("quotes", mapQuotes(getQuotesForDisplay(user)));
        return response;
    }

    @PostMapping("/favoriteQuotes/update")
    @ResponseBody
    public Map<String, Object> updateFavoriteQuote(
            @RequestParam("idCita") int idCita,
            @RequestParam("texto") String texto,
            @RequestParam("obra") String obra,
            Principal principal) {
        Map<String, Object> response = new HashMap<>();
        User user = getAuthenticatedUser(principal);

        if (user == null) {
            response.put("success", false);
            response.put("message", "Usuario no autenticado.");
            return response;
        }

        FavoriteQuote quote = favoriteQuoteRepository.findById(idCita).orElse(null);
        if (quote == null || user.getIdUsuario() != quote.getIdUsuario()) {
            response.put("success", false);
            response.put("message", "Cita no encontrada.");
            return response;
        }

        String normalizedText = normalizeQuoteText(texto);
        String normalizedWork = normalizeWork(obra);
        if (normalizedText.isEmpty() || normalizedWork.isEmpty()) {
            response.put("success", false);
            response.put("message", "La cita y la obra son obligatorias.");
            return response;
        }

        quote.setTexto(normalizedText);
        quote.setObra(normalizedWork);
        favoriteQuoteRepository.save(quote);

        response.put("success", true);
        response.put("quotes", mapQuotes(getQuotesForDisplay(user)));
        return response;
    }

    @PostMapping("/favoriteQuotes/delete")
    @ResponseBody
    public Map<String, Object> deleteFavoriteQuote(
            @RequestParam("idCita") int idCita,
            Principal principal) {
        Map<String, Object> response = new HashMap<>();
        User user = getAuthenticatedUser(principal);

        if (user == null) {
            response.put("success", false);
            response.put("message", "Usuario no autenticado.");
            return response;
        }

        FavoriteQuote quote = favoriteQuoteRepository.findById(idCita).orElse(null);
        if (quote == null || user.getIdUsuario() != quote.getIdUsuario()) {
            response.put("success", false);
            response.put("message", "Cita no encontrada.");
            return response;
        }

        favoriteQuoteRepository.delete(quote);
        reorderQuotes(user.getIdUsuario());

        response.put("success", true);
        response.put("quotes", mapQuotes(getQuotesForDisplay(user)));
        return response;
    }

    @GetMapping("/readingGoal")
    @ResponseBody
    public Map<String, Object> getReadingGoal(Principal principal) {
        Map<String, Object> response = new HashMap<>();

        if (principal == null) {
            response.put("success", false);
            response.put("goal", 10);
            return response;
        }

        User user = userRepository.findByNombreUsuario(principal.getName());
        response.put("success", user != null);
        response.put("goal", user != null ? user.getDesafioLectura() : 10);
        return response;
    }

    @PostMapping("/readingGoal")
    @ResponseBody
    public Map<String, Object> updateReadingGoal(@RequestParam("goal") int goal, Principal principal) {
        Map<String, Object> response = new HashMap<>();

        if (principal == null) {
            response.put("success", false);
            response.put("message", "Usuario no autenticado.");
            return response;
        }

        User user = userRepository.findByNombreUsuario(principal.getName());
        if (user == null) {
            response.put("success", false);
            response.put("message", "Usuario no encontrado.");
            return response;
        }

        int normalizedGoal = Math.max(1, Math.min(goal, 999));
        user.setDesafioLectura(normalizedGoal);
        userRepository.save(user);

        response.put("success", true);
        response.put("goal", normalizedGoal);
        return response;
    }

    @GetMapping("/explora")
    public String getExplora() {
        return "explora"; // Renderiza explora.html
    }

    @GetMapping("/comunidades")
    public String getComunidades() {
        return "comunidades"; // Renderiza comunidades.html
    }

    private User getAuthenticatedUser(Principal principal) {
        if (principal == null) {
            return null;
        }
        return userRepository.findByNombreUsuario(principal.getName());
    }

    private List<FavoriteQuote> getQuotePreview(User user) {
        return getQuotesForDisplay(user).stream().limit(2).toList();
    }

    private List<FavoriteQuote> defaultQuotePreview() {
        List<FavoriteQuote> defaults = new ArrayList<>();
        defaults.add(new FavoriteQuote(0,
                "Si el mundo desapareciera y el se salvara, yo seguiria viviendo. Pero si desapareciera el y lo demas continuara igual, yo no podria vivir.",
                "Cumbres Borrascosas",
                0));
        defaults.add(new FavoriteQuote(0,
                "La rabia puede calentarte por la noche, y el orgullo herido puede alentar a un hombre a hacer cosas maravillosas.",
                "El nombre del viento",
                1));
        return defaults;
    }

    private List<FavoriteQuote> ensureDefaultQuotes(User user) {
        List<FavoriteQuote> quotes = favoriteQuoteRepository.findByIdUsuarioOrderByOrdenVisualAscIdCitaAsc(user.getIdUsuario());
        if (!quotes.isEmpty()) {
            return quotes;
        }

        List<FavoriteQuote> defaults = new ArrayList<>();
        defaults.add(new FavoriteQuote(user.getIdUsuario(),
                "Si el mundo desapareciera y el se salvara, yo seguiria viviendo. Pero si desapareciera el y lo demas continuara igual, yo no podria vivir.",
                "Cumbres Borrascosas",
                0));
        defaults.add(new FavoriteQuote(user.getIdUsuario(),
                "La rabia puede calentarte por la noche, y el orgullo herido puede alentar a un hombre a hacer cosas maravillosas.",
                "El nombre del viento",
                1));
        favoriteQuoteRepository.saveAll(defaults);
        return favoriteQuoteRepository.findByIdUsuarioOrderByOrdenVisualAscIdCitaAsc(user.getIdUsuario());
    }

    private List<FavoriteQuote> getQuotesForDisplay(User user) {
        ensureDefaultQuotes(user);
        return favoriteQuoteRepository.findByIdUsuarioOrderByOrdenVisualDescIdCitaDesc(user.getIdUsuario());
    }

    private List<Map<String, Object>> mapQuotes(List<FavoriteQuote> quotes) {
        List<Map<String, Object>> mapped = new ArrayList<>();
        for (FavoriteQuote quote : quotes) {
            Map<String, Object> item = new HashMap<>();
            item.put("idCita", quote.getIdCita());
            item.put("texto", quote.getTexto());
            item.put("obra", quote.getObra());
            mapped.add(item);
        }
        return mapped;
    }

    private void reorderQuotes(int idUsuario) {
        List<FavoriteQuote> quotes = favoriteQuoteRepository.findByIdUsuarioOrderByOrdenVisualAscIdCitaAsc(idUsuario);
        for (int i = 0; i < quotes.size(); i++) {
            quotes.get(i).setOrdenVisual(i);
        }
        favoriteQuoteRepository.saveAll(quotes);
    }

    private String normalizeQuoteText(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.trim().replaceAll("\\s+", " ");
    }

    private String normalizeWork(String obra) {
        if (obra == null) {
            return "";
        }
        return obra.trim().replaceAll("\\s+", " ");
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }
        return "redirect:/logout"; // Redirige al login con parámetro de logout
    }
}
