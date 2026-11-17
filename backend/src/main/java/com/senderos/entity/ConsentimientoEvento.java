package com.senderos.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Bitácora inmutable del consentimiento informado: cada decisión del
 * residente (aceptación o rechazo) con su versión, IP y hash del texto.
 * A diferencia de `consentimiento` (estado vigente), aquí el historial
 * nunca se sobrescribe.
 */
@Entity
@Table(name = "consentimiento_evento")
public class ConsentimientoEvento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vivienda_id", nullable = false)
    private Vivienda vivienda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "version_documento", nullable = false, length = 20)
    private String versionDocumento;

    @Column(nullable = false)
    private boolean aceptado;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @Column(name = "hash_texto", length = 64)
    private String hashTexto;

    @Column(name = "fecha_evento", nullable = false)
    private Instant fechaEvento;

    @PrePersist
    void onCreate() { this.fechaEvento = Instant.now(); }

    public Long getId() { return id; }
    public Vivienda getVivienda() { return vivienda; }
    public void setVivienda(Vivienda vivienda) { this.vivienda = vivienda; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getVersionDocumento() { return versionDocumento; }
    public void setVersionDocumento(String versionDocumento) { this.versionDocumento = versionDocumento; }
    public boolean isAceptado() { return aceptado; }
    public void setAceptado(boolean aceptado) { this.aceptado = aceptado; }
    public String getIpOrigen() { return ipOrigen; }
    public void setIpOrigen(String ipOrigen) { this.ipOrigen = ipOrigen; }
    public String getHashTexto() { return hashTexto; }
    public void setHashTexto(String hashTexto) { this.hashTexto = hashTexto; }
    public Instant getFechaEvento() { return fechaEvento; }
}
