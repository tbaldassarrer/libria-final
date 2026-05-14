package es.prw.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuariolector")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private int idUsuario;

    @Column(name = "nombre_usuario", unique = true, nullable = false)
    private String nombreUsuario;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "desafio_lectura")
    private Integer desafioLectura = 10;

    @Column(name = "enabled")
    private Boolean enabled = false;

    @Column(name = "activation_token", length = 100)
    private String activationToken;

    @Column(name = "activation_token_created_at")
    private LocalDateTime activationTokenCreatedAt;

    // Constructor vacío obligatorio para JPA
    public User() {}

    public User(String nombreUsuario, String email, String password) {
        this.nombreUsuario = nombreUsuario;
        this.email = email;
        this.password = password;
        this.enabled = false;
    }

    // Getters y Setters
    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getDesafioLectura() {
        return desafioLectura != null ? desafioLectura : 10;
    }

    public void setDesafioLectura(Integer desafioLectura) {
        this.desafioLectura = desafioLectura;
    }

    public boolean isEnabled() {
        return enabled == null || Boolean.TRUE.equals(enabled);
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getActivationToken() {
        return activationToken;
    }

    public void setActivationToken(String activationToken) {
        this.activationToken = activationToken;
    }

    public LocalDateTime getActivationTokenCreatedAt() {
        return activationTokenCreatedAt;
    }

    public void setActivationTokenCreatedAt(LocalDateTime activationTokenCreatedAt) {
        this.activationTokenCreatedAt = activationTokenCreatedAt;
    }
}
