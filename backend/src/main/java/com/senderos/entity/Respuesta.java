package com.senderos.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "respuesta")
public class Respuesta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vivienda_id", nullable = false)
    private Vivienda vivienda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pregunta_id", nullable = false)
    private Pregunta pregunta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opcion_id")
    private Opcion opcion;

    @Column(name = "valor_texto", length = 500)
    private String valorTexto;

    @Column(name = "respondido_en", nullable = false, updatable = false)
    private Instant respondidoEn;

    @PrePersist
    void onCreate() { this.respondidoEn = Instant.now(); }

    public Long getId() { return id; }
    public Vivienda getVivienda() { return vivienda; }
    public void setVivienda(Vivienda vivienda) { this.vivienda = vivienda; }
    public Pregunta getPregunta() { return pregunta; }
    public void setPregunta(Pregunta pregunta) { this.pregunta = pregunta; }
    public Opcion getOpcion() { return opcion; }
    public void setOpcion(Opcion opcion) { this.opcion = opcion; }
    public String getValorTexto() { return valorTexto; }
    public void setValorTexto(String valorTexto) { this.valorTexto = valorTexto; }
    public Instant getRespondidoEn() { return respondidoEn; }
}
