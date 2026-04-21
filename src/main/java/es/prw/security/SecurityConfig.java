package es.prw.security;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    @SuppressWarnings("unused")
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Deshabilitamos CSRF para permitir peticiones POST como /register
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/styles/**", "/js/**", "/images/**", "/bootstrap/**", "/login", "/register").permitAll() // Ahora se permite acceso a /register
                        .requestMatchers("/home").authenticated()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/home", true)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .permitAll()
                        .deleteCookies("JSESSIONID"))
                
                ; 

        return http.build();
    }

    @Bean
    @SuppressWarnings("unused")
    UserDetailsService userDetailsService() {
        return username -> {
            try (Connection connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/db_libria", "root", "1234")) {

                System.out.println("🔍 Buscando usuario en la base de datos: " + username);

                // Consulta para obtener el usuario y su contraseña
                String queryUser = "SELECT idUsuario, password FROM usuariolector WHERE nombreUsuario = ?";
                try (PreparedStatement ps = connection.prepareStatement(queryUser)) {
                    ps.setString(1, username);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int userId = rs.getInt("idUsuario");
                            String password = rs.getString("password"); // {noop} indica que la contraseña no está encriptada

                            System.out.println("✅ Usuario encontrado en la BD: " + username);

                            // Consulta para obtener los libros leídos por el usuario (esto es opcional)
                            String queryBooks = "SELECT idLibro, puntuacion FROM registrolectura WHERE idUsuario = ?";
                            try (PreparedStatement psBooks = connection.prepareStatement(queryBooks)) {
                                psBooks.setInt(1, userId);
                                try (ResultSet rsBooks = psBooks.executeQuery()) {
                                    List<String> books = new ArrayList<>();
                                    while (rsBooks.next()) {
                                        String bookInfo = "Libro ID: " + rsBooks.getInt("idLibro") +
                                                ", Puntuación: " + rsBooks.getInt("puntuacion");
                                        books.add(bookInfo);
                                    }

                                    // Asignamos el rol "USER" para evitar errores con roles dinámicos
                                    return User.builder()
                                            .username(username)
                                            .password(password)
                                            .roles("USER") 
                                            .build();
                                }
                            }

                        } else {
                            System.out.println("⚠️ ERROR: Usuario no encontrado: " + username);
                            throw new UsernameNotFoundException("Usuario no encontrado");
                        }
                    }
                }
            } catch (SQLException e) {
                System.out.println("❌ ERROR al acceder a la base de datos: " + e.getMessage());
                throw new UsernameNotFoundException("Error al acceder a la base de datos", e);
            }
        };
    }
}
