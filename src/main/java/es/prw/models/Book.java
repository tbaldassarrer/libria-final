package es.prw.models;

import jakarta.persistence.*;

@Entity
@Table(name = "libros")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int idLibro;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "autor", nullable = false)
    private String autor;

    @Column(name = "genero")
    private String genero;

    @Column(name = "anioEdicion")
    private String anioEdicion;

    @Column(name = "sinopsis")
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
        this.anioEdicion = anioEdicion;
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
        return anioEdicion;
    }

    public void setAnioEdicion(String anioEdicion) {
        this.anioEdicion = anioEdicion;
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