package com.senderos.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Token de verificación de correo: un solo uso, expiración de 24 h,
 * almacenado como hash SHA-256 (el token en claro solo viaja por correo).
 */
@Entity
@Table(name = "email_verification_token")
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, length = 128, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private boolean usado = false;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @PrePersist
    void onCreate() { this.creadoEn = Instant.now(); }

    public EmailVerificationToken() {}

    public EmailVerificationToken(Usuario usuario, String tokenHash, Instant expiraEn) {
        this.usuario = usuario;
        this.tokenHash = tokenHash;
        this.expiraEn = expiraEn;
    }

    /** Vigente = no usado y no expirado. */
    public boolean isVigente() {
        return !usado && Instant.now().isBefore(expiraEn);
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getTokenHash() { return tokenHash; }
    public boolean isUsado() { return usado; }
    public void setUsado(boolean usado) { this.usado = usado; }
    public Instant getExpiraEn() { return expiraEn; }
    public Instant getCreadoEn() { return creadoEn; }
}
