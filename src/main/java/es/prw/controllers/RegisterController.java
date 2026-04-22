package es.prw.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.transaction.annotation.Transactional;

import es.prw.models.User;
import es.prw.repositories.UserRepository;

@Controller
public class RegisterController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/register")
    @Transactional
    public String registerUser(
            @RequestParam("nombreUsuario") String nombreUsuario,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("reppassword") String reppassword,
            Model model) {

        System.out.println("⏳ Intentando registrar usuario: " + nombreUsuario + ", email: " + email);

        // Validación de la contraseña
        String passwordPattern = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*?&.])[A-Za-z\\d@$!%*?&.]{6,}$";
        if (!password.matches(passwordPattern)) {
            model.addAttribute("registerError", "⚠️ La contraseña debe tener al menos 6 caracteres, una letra, un número y un carácter especial.");
            model.addAttribute("nombreUsuario", nombreUsuario);
            model.addAttribute("email", email);
            return "login";
        }

        // Verificar que las contraseñas coincidan
        if (!password.equals(reppassword)) {
            model.addAttribute("registerError", "❌ Las contraseñas no coinciden.");
            model.addAttribute("nombreUsuario", nombreUsuario);
            model.addAttribute("email", email);
            return "login";
        }

        try {
            // Verificar si el email ya está registrado
            if (userRepository.findByEmail(email) != null) {
                model.addAttribute("registerError", "⚠️ El correo ya está registrado.");
                model.addAttribute("nombreUsuario", nombreUsuario);
                model.addAttribute("email", email);
                return "login";
            }

            // Verificar si el nombre de usuario ya está registrado
            if (userRepository.findByNombreUsuario(nombreUsuario) != null) {
                model.addAttribute("registerError", "⚠️ El nombre de usuario ya está registrado.");
                model.addAttribute("nombreUsuario", nombreUsuario);
                model.addAttribute("email", email);
                return "login";
            }

            // Encriptar la contraseña
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            String hashedPassword = encoder.encode(password);

            System.out.println("🔐 Contraseña encriptada: " + hashedPassword);

            // Crear y guardar el usuario
            User user = new User(nombreUsuario, email, hashedPassword);
            User savedUser = userRepository.save(user);

            System.out.println("✅ Usuario guardado con ID: " + savedUser.getIdUsuario());

            model.addAttribute("registerSuccess", "✅ Registro completado correctamente, ¡Bienvenido a LIBRIA!");
            System.out.println("✅ Usuario registrado correctamente.");

            return "login";

        } catch (Exception e) {
            model.addAttribute("registerError", "❌ Error en la base de datos: " + e.getMessage());
            model.addAttribute("nombreUsuario", nombreUsuario);
            model.addAttribute("email", email);
            System.out.println("❌ ERROR: " + e.getMessage());
            return "login";
        }
    }
}
