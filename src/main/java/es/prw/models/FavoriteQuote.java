package es.prw.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "citasfavoritas")
public class FavoriteQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idCita")
    private Integer idCita;

    @Column(name = "idUsuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "texto", nullable = false, length = 1200)
    private String texto;

    @Column(name = "obra", nullable = false, length = 255)
    private String obra;

    @Column(name = "ordenVisual", nullable = false)
    private Integer ordenVisual = 0;

    public FavoriteQuote() {
    }

    public FavoriteQuote(Integer idUsuario, String texto, String obra, Integer ordenVisual) {
        this.idUsuario = idUsuario;
        this.texto = texto;
        this.obra = obra;
        this.ordenVisual = ordenVisual;
    }

    public Integer getIdCita() {
        return idCita;
    }

    public void setIdCita(Integer idCita) {
        this.idCita = idCita;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public String getObra() {
        return obra;
    }

    public void setObra(String obra) {
        this.obra = obra;
    }

    public Integer getOrdenVisual() {
        return ordenVisual;
    }

    public void setOrdenVisual(Integer ordenVisual) {
        this.ordenVisual = ordenVisual;
    }
}
