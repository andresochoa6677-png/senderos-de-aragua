package com.senderos.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "intento_ejercicio")
public class IntentoEjercicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vivienda_id")
    private Vivienda vivienda;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ejercicio_id")
    private Ejercicio ejercicio;

    /** Respuesta enviada por la vivienda, tal como llegó. */
    @Column(nullable = false, length = 200)
    private String respuesta;

    @Column(nullable = false)
    private Boolean correcto;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @PrePersist
    void onCreate() { this.creadoEn = Instant.now(); }

    public IntentoEjercicio() {}

    public IntentoEjercicio(Vivienda vivienda, Ejercicio ejercicio, String respuesta, boolean correcto) {
        this.vivienda = vivienda;
        this.ejercicio = ejercicio;
        this.respuesta = respuesta;
        this.correcto = correcto;
    }

    public Long getId() { return id; }
    public Vivienda getVivienda() { return vivienda; }
    public Ejercicio getEjercicio() { return ejercicio; }
    public String getRespuesta() { return respuesta; }
    public Boolean getCorrecto() { return correcto; }
    public Instant getCreadoEn() { return creadoEn; }
}
