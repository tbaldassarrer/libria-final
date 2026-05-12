package es.prw.controllers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import es.prw.services.EmailService;

@Controller
public class Contacto {

    private final EmailService emailService;

    @Value("${app.mail.contact-to:${spring.mail.username:}}")
    private String contactTo;

    public Contacto(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/contacto")
    public String mostrarFormularioContacto() {
        return "contacto";
    }

    @PostMapping("/contacto")
    public ResponseEntity<Map<String, String>> enviarContacto(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("message") String message) {

        name = name.trim();
        email = email.trim();
        message = message.trim();

        if (name.isBlank() || email.isBlank() || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Completa todos los campos."));
        }

        try {
            emailService.sendContactEmail(contactTo, name, email, message);
            return ResponseEntity.ok(Map.of("message", "Tu mensaje ha sido enviado correctamente."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (MailException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "No se pudo enviar el mensaje. Revisa la configuracion SMTP."));
        }
    }
}
