package com.senderos.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "leccion")
public class Leccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer orden;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, length = 300)
    private String resumen;

    @Column(nullable = false, length = 30)
    private String icono;

    @Column(nullable = false, length = 20)
    private String color;

    /** Ruta publica de la imagen del recurso visual (ej. /images/aprendizaje/...). */
    @Column(length = 255)
    private String imagen;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    /** Ejemplo cotidiano que ilustra la leccion. */
    @Column(columnDefinition = "TEXT")
    private String ejemplo;

    /** Frase sintesis que se muestra en la tarjeta "Idea clave". */
    @Column(name = "idea_clave", length = 300)
    private String ideaClave;

    @Column(nullable = false)
    private boolean activa = true;

    public Leccion() {}

    public Long getId() { return id; }
    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getResumen() { return resumen; }
    public void setResumen(String resumen) { this.resumen = resumen; }
    public String getIcono() { return icono; }
    public void setIcono(String icono) { this.icono = icono; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public String getEjemplo() { return ejemplo; }
    public void setEjemplo(String ejemplo) { this.ejemplo = ejemplo; }
    public String getIdeaClave() { return ideaClave; }
    public void setIdeaClave(String ideaClave) { this.ideaClave = ideaClave; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
}
