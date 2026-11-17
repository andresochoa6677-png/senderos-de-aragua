package com.senderos.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "pregunta")
public class Pregunta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encuesta_id", nullable = false)
    private Encuesta encuesta;

    @Column(nullable = false, length = 300)
    private String texto;

    @Column(nullable = false, length = 15)
    private String tipo;

    /** Seccion del instrumento: CONOCIMIENTO / PRACTICA / PERCEPCION. */
    @Column(name = "tipo_pregunta", nullable = false, length = 15)
    private String tipoPregunta = "CONOCIMIENTO";

    @Column(nullable = false)
    private int orden = 0;

    @Column(nullable = false)
    private boolean requerida = true;

    @OneToMany(mappedBy = "pregunta", fetch = FetchType.LAZY)
    private List<Opcion> opciones;

    public Long getId() { return id; }
    public Encuesta getEncuesta() { return encuesta; }
    public void setEncuesta(Encuesta encuesta) { this.encuesta = encuesta; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getTipoPregunta() { return tipoPregunta; }
    public void setTipoPregunta(String tipoPregunta) { this.tipoPregunta = tipoPregunta; }
    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }
    public boolean isRequerida() { return requerida; }
    public void setRequerida(boolean requerida) { this.requerida = requerida; }
    public List<Opcion> getOpciones() { return opciones; }
}
