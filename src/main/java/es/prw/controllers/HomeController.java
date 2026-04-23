package es.prw.controllers;

import java.security.Principal;
import java.util.HashMap;
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

import es.prw.models.User;
import es.prw.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class HomeController {

    @Autowired
    private UserRepository userRepository;

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
        } else {
            model.addAttribute("username", "Invitado");
            model.addAttribute("readingGoal", 10);
        }
        return "home"; // Se renderiza home.html en src/main/resources/templates
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

    @PostMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }
        return "redirect:/logout"; // Redirige al login con parámetro de logout
    }
}
