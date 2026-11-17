package com.senderos.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "vivienda")
public class Vivienda {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20, unique = true)
    private String codigo;

    /** Bloque oficial del censo (A…Z, RR, Ñ). NULL solo para DEMO-001. */
    @Column(name = "bloque", length = 2)
    private String bloque;

    /** Número de la casa dentro del bloque. NULL solo para DEMO-001. */
    @Column(name = "numero")
    private Integer numero;

    @Column(length = 120)
    private String direccion;

    @Column(length = 60)
    private String sector;

    @Column(nullable = false)
    private boolean demo = false;

    @Column(nullable = false)
    private boolean activa = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @PrePersist
    void onCreate() { this.creadoEn = Instant.now(); }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getBloque() { return bloque; }
    public void setBloque(String bloque) { this.bloque = bloque; }
    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }
    public boolean isDemo() { return demo; }
    public void setDemo(boolean demo) { this.demo = demo; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
    public Instant getCreadoEn() { return creadoEn; }
}
