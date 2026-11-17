package com.senderos.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Intento de una actividad de práctica libre, registrado por vivienda
 * (resuelta desde la sesión autenticada) para análisis posterior de
 * errores. Nunca se exponen intentos de otras viviendas.
 */
@Entity
@Table(name = "intento_practica")
public class IntentoPractica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vivienda_id", nullable = false)
    private Vivienda vivienda;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "practica_item_id", nullable = false)
    private PracticaItem item;

    @Column(nullable = false, length = 20)
    private String modo;

    @Column(nullable = false, length = 1000)
    private String respuesta;

    @Column(nullable = false)
    private boolean correcto;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @PrePersist
    void onCreate() { this.creadoEn = Instant.now(); }

    public IntentoPractica() {}

    public IntentoPractica(Vivienda vivienda, PracticaItem item, String modo, String respuesta, boolean correcto) {
        this.vivienda = vivienda;
        this.item = item;
        this.modo = modo;
        this.respuesta = respuesta;
        this.correcto = correcto;
    }

    public Long getId() { return id; }
    public Vivienda getVivienda() { return vivienda; }
    public PracticaItem getItem() { return item; }
    public String getModo() { return modo; }
    public String getRespuesta() { return respuesta; }
    public boolean isCorrecto() { return correcto; }
    public Instant getCreadoEn() { return creadoEn; }
}
