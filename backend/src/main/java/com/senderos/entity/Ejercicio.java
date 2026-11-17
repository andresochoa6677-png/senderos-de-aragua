package com.senderos.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ejercicio")
public class Ejercicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leccion_id")
    private Leccion leccion;

    @Column(nullable = false)
    private Integer orden;

    /** OPCION_MULTIPLE | VERDADERO_FALSO | ORDENAR */
    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false, length = 400)
    private String enunciado;

    @Column(nullable = false, length = 400)
    private String explicacion;

    /** JSON de opciones; se convierte a lista en el DTO. */
    @Column(nullable = false, columnDefinition = "json")
    private String opciones;

    /** Respuesta correcta. Nunca se expone al cliente. */
    @Column(nullable = false, length = 120)
    private String respuesta;

    @Column(nullable = false)
    private Integer puntos;

    @Column(nullable = false)
    private Boolean activo = true;

    public Ejercicio() {}

    public Long getId() { return id; }
    public Leccion getLeccion() { return leccion; }
    public Integer getOrden() { return orden; }
    public String getTipo() { return tipo; }
    public String getEnunciado() { return enunciado; }
    public String getExplicacion() { return explicacion; }
    public String getOpciones() { return opciones; }
    public String getRespuesta() { return respuesta; }
    public Integer getPuntos() { return puntos; }
    public Boolean getActivo() { return activo; }
}
