package com.senderos.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "consentimiento")
public class Consentimiento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vivienda_id", nullable = false)
    private Vivienda vivienda;

    /** Quien aceptó o rechazó; NULL en filas legacy REG-1.0 sin trazabilidad. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false)
    private boolean aceptado = false;

    @Column(name = "version_documento", nullable = false, length = 20)
    private String versionDocumento;

    /** IP desde la cual se registró la decisión (45 cubre IPv6). */
    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    /** SHA-256 del texto exacto del documento aceptado/rechazado. */
    @Column(name = "hash_texto", length = 64)
    private String hashTexto;

    @Column(name = "fecha_aceptacion")
    private Instant fechaAceptacion;

    public Long getId() { return id; }
    public Vivienda getVivienda() { return vivienda; }
    public void setVivienda(Vivienda vivienda) { this.vivienda = vivienda; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public boolean isAceptado() { return aceptado; }
    public void setAceptado(boolean aceptado) { this.aceptado = aceptado; }
    public String getVersionDocumento() { return versionDocumento; }
    public void setVersionDocumento(String versionDocumento) { this.versionDocumento = versionDocumento; }
    public String getIpOrigen() { return ipOrigen; }
    public void setIpOrigen(String ipOrigen) { this.ipOrigen = ipOrigen; }
    public String getHashTexto() { return hashTexto; }
    public void setHashTexto(String hashTexto) { this.hashTexto = hashTexto; }
    public Instant getFechaAceptacion() { return fechaAceptacion; }
    public void setAceptado() { this.aceptado = true; this.fechaAceptacion = Instant.now(); }
    /** Registra un rechazo de trazabilidad con su momento exacto. */
    public void setRechazado() { this.aceptado = false; this.fechaAceptacion = Instant.now(); }
}
