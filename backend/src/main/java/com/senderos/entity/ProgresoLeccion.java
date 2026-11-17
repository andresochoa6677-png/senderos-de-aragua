package com.senderos.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "progreso_leccion")
public class ProgresoLeccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vivienda_id", nullable = false)
    private Vivienda vivienda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leccion_id", nullable = false)
    private Leccion leccion;

    @Column(name = "iniciado_en", nullable = false, updatable = false)
    private Instant iniciadoEn;

    @Column(name = "completado_en")
    private Instant completadoEn;

    @PrePersist
    void onCreate() { this.iniciadoEn = Instant.now(); }

    public ProgresoLeccion() {}

    public ProgresoLeccion(Vivienda vivienda, Leccion leccion) {
        this.vivienda = vivienda;
        this.leccion = leccion;
    }

    /** Marca la leccion como completada. Idempotente: conserva la primera fecha. */
    public void completar() {
        if (this.completadoEn == null) this.completadoEn = Instant.now();
    }

    public Long getId() { return id; }
    public Vivienda getVivienda() { return vivienda; }
    public Leccion getLeccion() { return leccion; }
    public Instant getIniciadoEn() { return iniciadoEn; }
    public Instant getCompletadoEn() { return completadoEn; }
}
