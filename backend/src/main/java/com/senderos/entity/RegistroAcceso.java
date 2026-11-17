package com.senderos.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Rastro de actividad de una vivienda: un fila por inicio de sesión
 * (modulo = SESION) y por consulta de módulo. Es el registro de
 * "accesos y módulos consultados" que el consentimiento REG-2.0
 * declara a los participantes.
 */
@Entity
@Table(name = "registro_acceso")
public class RegistroAcceso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vivienda_id", nullable = false)
    private Vivienda vivienda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_hora", nullable = false)
    private Instant fechaHora;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    /** SESION | LECCIONES | EJERCICIOS | PRACTICA | COMENTARIOS | ENCUESTA_FINAL | ENCUESTA */
    @Column(name = "modulo", nullable = false, length = 30)
    private String modulo;

    @PrePersist
    void onCreate() { if (fechaHora == null) fechaHora = Instant.now(); }

    public Long getId() { return id; }
    public Vivienda getVivienda() { return vivienda; }
    public void setVivienda(Vivienda vivienda) { this.vivienda = vivienda; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Instant getFechaHora() { return fechaHora; }
    public String getIpOrigen() { return ipOrigen; }
    public void setIpOrigen(String ipOrigen) { this.ipOrigen = ipOrigen; }
    public String getModulo() { return modulo; }
    public void setModulo(String modulo) { this.modulo = modulo; }
}
