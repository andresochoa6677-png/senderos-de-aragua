package com.senderos.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Aporte privado del residente al proyecto (inquietud, sugerencia,
 * percepción o situación). Asociado a la vivienda y al usuario de la
 * sesión para trazabilidad y análisis cualitativo posterior.
 * categoriaTematica queda RESERVADA para la categorización temática
 * manual del equipo investigador: la aplicación nunca la determina.
 */
@Entity
@Table(name = "comentario")
public class Comentario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vivienda_id", nullable = false)
    private Vivienda vivienda;

    /** INQUIETUD | SUGERENCIA | PERCEPCION | SITUACION */
    @Column(nullable = false, length = 20)
    private String tipo;

    /** Tema que el residente declara (opcional); NO es la categoría del análisis. */
    @Column(name = "tema_declarado", length = 40)
    private String temaDeclarado;

    @Column(nullable = false, length = 500)
    private String contenido;

    /** Reservada para la categorización temática manual posterior. */
    @Column(name = "categoria_tematica", length = 60)
    private String categoriaTematica;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @PrePersist
    void onCreate() { this.creadoEn = Instant.now(); }

    public Comentario() {}

    public Comentario(Usuario usuario, Vivienda vivienda, String tipo,
                      String temaDeclarado, String contenido) {
        this.usuario = usuario;
        this.vivienda = vivienda;
        this.tipo = tipo;
        this.temaDeclarado = temaDeclarado;
        this.contenido = contenido;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Vivienda getVivienda() { return vivienda; }
    public String getTipo() { return tipo; }
    public String getTemaDeclarado() { return temaDeclarado; }
    public String getContenido() { return contenido; }
    public String getCategoriaTematica() { return categoriaTematica; }
    public void setCategoriaTematica(String categoriaTematica) { this.categoriaTematica = categoriaTematica; }
    public Instant getCreadoEn() { return creadoEn; }
}
