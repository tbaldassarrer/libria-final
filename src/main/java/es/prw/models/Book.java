package es.prw.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "libros")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idLibro")
    private int idLibro;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "autor", nullable = false)
    private String autor;

    @Column(name = "genero")
    private String genero;

    @Column(name = "anioEdicion")
    private Integer anioEdicion;

    @Column(name = "sinopsis", length = 2000)
    private String sinopsis;

    @Column(name = "puntuacion")
    private String puntuacion;

    @Column(name = "cover_image")
    private String coverImage;

    // Constructor vacío
    public Book() {}

    // Constructor con parámetros
    public Book(String titulo, String autor, String genero, String anioEdicion, String sinopsis, String puntuacion, String coverImage) {
        this.titulo = titulo;
        this.autor = autor;
        this.genero = genero;
        this.anioEdicion = parseAnioEdicion(anioEdicion);
        this.sinopsis = sinopsis;
        this.puntuacion = puntuacion;
        this.coverImage = coverImage;
    }

    // Getters y Setters
    public int getIdLibro() {
        return idLibro;
    }

    public void setIdLibro(int idLibro) {
        this.idLibro = idLibro;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public String getAnioEdicion() {
        return anioEdicion != null ? anioEdicion.toString() : "";
    }

    public void setAnioEdicion(String anioEdicion) {
        this.anioEdicion = parseAnioEdicion(anioEdicion);
    }

    private Integer parseAnioEdicion(String anioEdicion) {
        if (anioEdicion == null || anioEdicion.isBlank()) {
            return null;
        }

        try {
            return Integer.valueOf(anioEdicion.trim().substring(0, Math.min(4, anioEdicion.trim().length())));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String getSinopsis() {
        return sinopsis;
    }

    public void setSinopsis(String sinopsis) {
        this.sinopsis = sinopsis;
    }

    public String getPuntuacion() {
        return puntuacion;
    }

    public void setPuntuacion(String puntuacion) {
        this.puntuacion = puntuacion;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }
}
