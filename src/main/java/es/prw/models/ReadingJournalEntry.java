package es.prw.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "readingjournal")
public class ReadingJournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idJournal")
    private Integer idJournal;

    @Column(name = "idUsuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "titulo", length = 255)
    private String titulo;

    @Column(name = "autor", length = 255)
    private String autor;

    @Column(name = "paginas")
    private Integer paginas;

    @Column(name = "formato", length = 50)
    private String formato;

    @Column(name = "fechaInicio")
    private LocalDate fechaInicio;

    @Column(name = "fechaFin")
    private LocalDate fechaFin;

    @Column(name = "feelingOption", length = 255)
    private String feelingOption;

    @Column(name = "generalRating")
    private Integer generalRating = 0;

    @Column(name = "romanceRating")
    private Integer romanceRating = 0;

    @Column(name = "spiceRating")
    private Integer spiceRating = 0;

    @Column(name = "sadnessRating")
    private Integer sadnessRating = 0;

    @Column(name = "plotRating")
    private Integer plotRating = 0;

    @Column(name = "charactersRating")
    private Integer charactersRating = 0;

    @Column(name = "styleRating")
    private Integer styleRating = 0;

    @Column(name = "endingRating")
    private Integer endingRating = 0;

    @Column(name = "bestCharacter", length = 255)
    private String bestCharacter;

    @Column(name = "worstCharacter", length = 255)
    private String worstCharacter;

    @Lob
    @Column(name = "reflexionesFinales", columnDefinition = "LONGTEXT")
    private String reflexionesFinales;

    @Lob
    @Column(name = "coverImage", columnDefinition = "LONGTEXT")
    private String coverImage;

    @Column(name = "createdAt", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updatedAt", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Integer getIdJournal() {
        return idJournal;
    }

    public void setIdJournal(Integer idJournal) {
        this.idJournal = idJournal;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
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

    public Integer getPaginas() {
        return paginas;
    }

    public void setPaginas(Integer paginas) {
        this.paginas = paginas;
    }

    public String getFormato() {
        return formato;
    }

    public void setFormato(String formato) {
        this.formato = formato;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getFeelingOption() {
        return feelingOption;
    }

    public void setFeelingOption(String feelingOption) {
        this.feelingOption = feelingOption;
    }

    public Integer getGeneralRating() {
        return generalRating;
    }

    public void setGeneralRating(Integer generalRating) {
        this.generalRating = generalRating;
    }

    public Integer getRomanceRating() {
        return romanceRating;
    }

    public void setRomanceRating(Integer romanceRating) {
        this.romanceRating = romanceRating;
    }

    public Integer getSpiceRating() {
        return spiceRating;
    }

    public void setSpiceRating(Integer spiceRating) {
        this.spiceRating = spiceRating;
    }

    public Integer getSadnessRating() {
        return sadnessRating;
    }

    public void setSadnessRating(Integer sadnessRating) {
        this.sadnessRating = sadnessRating;
    }

    public Integer getPlotRating() {
        return plotRating;
    }

    public void setPlotRating(Integer plotRating) {
        this.plotRating = plotRating;
    }

    public Integer getCharactersRating() {
        return charactersRating;
    }

    public void setCharactersRating(Integer charactersRating) {
        this.charactersRating = charactersRating;
    }

    public Integer getStyleRating() {
        return styleRating;
    }

    public void setStyleRating(Integer styleRating) {
        this.styleRating = styleRating;
    }

    public Integer getEndingRating() {
        return endingRating;
    }

    public void setEndingRating(Integer endingRating) {
        this.endingRating = endingRating;
    }

    public String getBestCharacter() {
        return bestCharacter;
    }

    public void setBestCharacter(String bestCharacter) {
        this.bestCharacter = bestCharacter;
    }

    public String getWorstCharacter() {
        return worstCharacter;
    }

    public void setWorstCharacter(String worstCharacter) {
        this.worstCharacter = worstCharacter;
    }

    public String getReflexionesFinales() {
        return reflexionesFinales;
    }

    public void setReflexionesFinales(String reflexionesFinales) {
        this.reflexionesFinales = reflexionesFinales;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
