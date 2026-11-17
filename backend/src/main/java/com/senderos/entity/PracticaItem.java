package com.senderos.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Actividad del banco de práctica libre del módulo Clasificación.
 * La solución (respuesta) nunca sale del backend.
 */
@Entity
@Table(name = "practica_item")
public class PracticaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** CLASIFICA | PREPARA | SITUACION */
    @Column(nullable = false, length = 20)
    private String modo;

    /** DESTINO | ELECCION | ORDENAR | GRUPO */
    @Column(nullable = false, length = 20)
    private String tipo;

    /** Contexto opcional del escenario (modo SITUACION). */
    @Column(length = 300)
    private String escenario;

    @Column(nullable = false, length = 400)
    private String enunciado;

    @Column(length = 200)
    private String imagen;

    /** JSON de configuración visible (destinos, opciones, items, pasos). */
    @Column(nullable = false, columnDefinition = "json")
    private String contenido;

    /** Solución; jamás se expone al cliente. */
    @Column(nullable = false, length = 1000)
    private String respuesta;

    @Column(nullable = false, length = 500)
    private String explicacion;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @PrePersist
    void onCreate() { this.creadoEn = Instant.now(); }

    public Long getId() { return id; }
    public String getModo() { return modo; }
    public void setModo(String modo) { this.modo = modo; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getEscenario() { return escenario; }
    public void setEscenario(String escenario) { this.escenario = escenario; }
    public String getEnunciado() { return enunciado; }
    public void setEnunciado(String enunciado) { this.enunciado = enunciado; }
    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public String getRespuesta() { return respuesta; }
    public void setRespuesta(String respuesta) { this.respuesta = respuesta; }
    public String getExplicacion() { return explicacion; }
    public void setExplicacion(String explicacion) { this.explicacion = explicacion; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public Instant getCreadoEn() { return creadoEn; }
}
