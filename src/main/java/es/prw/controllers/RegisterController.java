package es.prw.controllers;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import es.prw.models.User;
import es.prw.repositories.UserRepository;
import es.prw.services.EmailService;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class RegisterController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Value("${app.base-url:}")
    private String appBaseUrl;

    @PostMapping("/register")
    @Transactional
    public String registerUser(
            @RequestParam("nombreUsuario") String nombreUsuario,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("reppassword") String reppassword,
            Model model,
            HttpServletRequest request) {

        nombreUsuario = nombreUsuario.trim();
        email = email.trim().toLowerCase();

        System.out.println("Intentando registrar usuario: " + nombreUsuario + ", email: " + email);

        String passwordPattern = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*?&.])[A-Za-z\\d@$!%*?&.]{6,}$";
        if (!password.matches(passwordPattern)) {
            model.addAttribute("registerError", "La contrasena debe tener al menos 6 caracteres, una letra, un numero y un caracter especial.");
            model.addAttribute("nombreUsuario", nombreUsuario);
            model.addAttribute("email", email);
            return "login";
        }

        if (!password.equals(reppassword)) {
            model.addAttribute("registerError", "Las contrasenas no coinciden.");
            model.addAttribute("nombreUsuario", nombreUsuario);
            model.addAttribute("email", email);
            return "login";
        }

        try {
            if (userRepository.findByEmail(email) != null) {
                model.addAttribute("registerError", "El correo ya esta registrado.");
                model.addAttribute("nombreUsuario", nombreUsuario);
                model.addAttribute("email", email);
                return "login";
            }

            if (userRepository.findByNombreUsuario(nombreUsuario) != null) {
                model.addAttribute("registerError", "El nombre de usuario ya esta registrado.");
                model.addAttribute("nombreUsuario", nombreUsuario);
                model.addAttribute("email", email);
                return "login";
            }

            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            String hashedPassword = encoder.encode(password);
            String activationToken = UUID.randomUUID().toString();

            User user = new User(nombreUsuario, email, hashedPassword);
            user.setEnabled(false);
            user.setActivationToken(activationToken);
            user.setActivationTokenCreatedAt(LocalDateTime.now());

            User savedUser = userRepository.save(user);

            String activationUrl = buildActivationUrl(request, activationToken);

            emailService.sendActivationEmail(savedUser.getEmail(), savedUser.getNombreUsuario(), activationUrl);

            System.out.println("Usuario pendiente de activacion guardado con ID: " + savedUser.getIdUsuario());
            model.addAttribute("registerSuccess", "Te hemos enviado un email de activacion. Revisa tu correo para completar el registro.");
            return "login";

        } catch (MailException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            model.addAttribute("registerError", "No se pudo enviar el email de activacion. Revisa el correo SMTP y la contrasena de aplicacion.");
            model.addAttribute("nombreUsuario", nombreUsuario);
            model.addAttribute("email", email);
            System.err.println("ERROR SMTP al registrar usuario " + nombreUsuario + ": " + e.getMessage());
            e.printStackTrace();
            return "login";
        } catch (IllegalStateException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            model.addAttribute("registerError", e.getMessage());
            model.addAttribute("nombreUsuario", nombreUsuario);
            model.addAttribute("email", email);
            System.err.println("ERROR CONFIG EMAIL al registrar usuario " + nombreUsuario + ": " + e.getMessage());
            e.printStackTrace();
            return "login";
        } catch (DataAccessException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            model.addAttribute("registerError", "No se pudo crear la cuenta por un problema con la base de datos. Revisa los logs de Render.");
            model.addAttribute("nombreUsuario", nombreUsuario);
            model.addAttribute("email", email);
            System.err.println("ERROR BASE DE DATOS al registrar usuario " + nombreUsuario + ": " + e.getMessage());
            e.printStackTrace();
            return "login";
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            model.addAttribute("registerError", "No se pudo completar el registro. Revisa los logs de Render para ver el motivo exacto.");
            model.addAttribute("nombreUsuario", nombreUsuario);
            model.addAttribute("email", email);
            System.err.println("ERROR INESPERADO al registrar usuario " + nombreUsuario + ": " + e.getMessage());
            e.printStackTrace();
            return "login";
        }
    }

    private String buildActivationUrl(HttpServletRequest request, String activationToken) {
        if (appBaseUrl != null && !appBaseUrl.isBlank()) {
            return ServletUriComponentsBuilder
                    .fromUriString(appBaseUrl.trim())
                    .path("/activate")
                    .queryParam("token", activationToken)
                    .build()
                    .toUriString();
        }

        return ServletUriComponentsBuilder
                .fromRequestUri(request)
                .replacePath(request.getContextPath() + "/activate")
                .replaceQuery(null)
                .queryParam("token", activationToken)
                .build()
                .toUriString();
    }

    @GetMapping("/activate")
    @Transactional
    public String activateAccount(@RequestParam("token") String token, Model model) {
        User user = userRepository.findByActivationToken(token);

        if (user == null) {
            model.addAttribute("registerError", "El enlace de activacion no es valido.");
            return "login";
        }

        user.setEnabled(true);
        user.setActivationToken(null);
        user.setActivationTokenCreatedAt(null);
        userRepository.save(user);

        model.addAttribute("registerSuccess", "Cuenta activada correctamente. Ya puedes iniciar sesion.");
        return "login";
    }
}
