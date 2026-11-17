package com.senderos.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Configuración única del estudio (fila id = 1). La posprueba está
 * DESHABILITADA por defecto; el administrador la habilita/deshabilita
 * manualmente desde el panel y cada acción queda con trazabilidad
 * (fecha/hora y responsable). fecha_inicio/iniciado_por son histórico
 * de la ventana automática anterior (ya sin uso en runtime).
 */
@Entity
@Table(name = "estudio_config")
public class EstudioConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_inicio")
    private Instant fechaInicio;

    @Column(name = "iniciado_por", length = 100)
    private String iniciadoPor;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;

    @Column(name = "posprueba_habilitada", nullable = false)
    private boolean pospruebaHabilitada = false;

    @Column(name = "posprueba_habilitada_en")
    private Instant pospruebaHabilitadaEn;

    @Column(name = "posprueba_habilitada_por", length = 100)
    private String pospruebaHabilitadaPor;

    @PrePersist
    void onCreate() { if (fechaCreacion == null) fechaCreacion = Instant.now(); }

    public Long getId() { return id; }
    public Instant getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(Instant fechaInicio) { this.fechaInicio = fechaInicio; }
    public String getIniciadoPor() { return iniciadoPor; }
    public void setIniciadoPor(String iniciadoPor) { this.iniciadoPor = iniciadoPor; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public boolean isPospruebaHabilitada() { return pospruebaHabilitada; }
    public void setPospruebaHabilitada(boolean pospruebaHabilitada) { this.pospruebaHabilitada = pospruebaHabilitada; }
    public Instant getPospruebaHabilitadaEn() { return pospruebaHabilitadaEn; }
    public void setPospruebaHabilitadaEn(Instant pospruebaHabilitadaEn) { this.pospruebaHabilitadaEn = pospruebaHabilitadaEn; }
    public String getPospruebaHabilitadaPor() { return pospruebaHabilitadaPor; }
    public void setPospruebaHabilitadaPor(String pospruebaHabilitadaPor) { this.pospruebaHabilitadaPor = pospruebaHabilitadaPor; }
}
