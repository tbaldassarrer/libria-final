package es.prw.controllers;

import java.security.Principal;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class HomeController {

    @GetMapping("/home")
    public String home(Model model, Principal principal) {
        if (principal != null) {
            String username = principal.getName();
            model.addAttribute("username", username);
        } else {
            model.addAttribute("username", "Invitado");
        }
        return "home"; // Se renderiza home.html en src/main/resources/templates
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
