package com.senderos.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "opcion")
public class Opcion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pregunta_id", nullable = false)
    private Pregunta pregunta;

    @Column(nullable = false, length = 200)
    private String texto;

    @Column(nullable = false, length = 50)
    private String valor;

    /** Clave de correccion (items de conocimiento). Nunca se expone al cliente. */
    @Column(name = "es_correcta", nullable = false)
    private boolean esCorrecta = false;

    @Column(nullable = false)
    private int orden = 0;

    public Long getId() { return id; }
    public Pregunta getPregunta() { return pregunta; }
    public void setPregunta(Pregunta pregunta) { this.pregunta = pregunta; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }
    public boolean isEsCorrecta() { return esCorrecta; }
    public void setEsCorrecta(boolean esCorrecta) { this.esCorrecta = esCorrecta; }
    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }
}
