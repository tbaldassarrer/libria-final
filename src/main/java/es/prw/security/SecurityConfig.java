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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
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
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    @SuppressWarnings("unused")
    SecurityFilterChain securityFilterChain(HttpSecurity http, DaoAuthenticationProvider authenticationProvider) throws Exception {
        http
                .authenticationProvider(authenticationProvider)
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/styles/**", "/js/**", "/images/**", "/bootstrap/**", "/login", "/register", "/").permitAll()
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
                String queryUser = "SELECT id_usuario, password FROM usuariolector WHERE nombre_usuario = ?";
                try (PreparedStatement ps = connection.prepareStatement(queryUser)) {
                    ps.setString(1, username);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String password = rs.getString("password");

                            System.out.println("✅ Usuario encontrado en la BD: " + username);

                            // Asignamos el rol "USER"
                            // Si la contraseña comienza con $2a o $2b, es BCrypt
                            // Si no, agregamos el prefijo {bcrypt}
                            if (!password.startsWith("$2a") && !password.startsWith("$2b")) {
                                password = "{bcrypt}" + password;
                            }

                            return User.builder()
                                    .username(username)
                                    .password(password)
                                    .roles("USER")
                                    .build();
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
