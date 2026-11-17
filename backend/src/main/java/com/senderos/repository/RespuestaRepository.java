package com.senderos.repository;

import com.senderos.entity.Respuesta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RespuestaRepository extends JpaRepository<Respuesta, Long> {
    List<Respuesta> findByViviendaId(Long viviendaId);

    @Query("select r from Respuesta r where r.vivienda.id = :viviendaId and r.pregunta.encuesta.id = :encuestaId")
    List<Respuesta> findByViviendaAndEncuesta(Long viviendaId, Long encuestaId);

    boolean existsByViviendaId(Long viviendaId);

    @Query("select count(r) > 0 from Respuesta r where r.vivienda.id = :viviendaId and r.pregunta.encuesta.id = :encuestaId")
    boolean existsByViviendaIdAndEncuestaId(Long viviendaId, Long encuestaId);

    @Query("select count(r) > 0 from Respuesta r where r.vivienda.id = :viviendaId and r.pregunta.encuesta.tipo = :tipo")
    boolean existsByViviendaIdAndEncuestaTipo(Long viviendaId, String tipo);

    /**
     * Aciertos de conocimiento de una vivienda en una encuesta: respuestas
     * cuya opcion marcada tiene es_correcta = 1. Los items de practicas y
     * percepción no llevan clave, así el conteo da el puntaje 0-10 de la
     * INICIAL (1 punto por ítem de conocimiento correcto).
     */
    @Query("select count(r) from Respuesta r where r.vivienda.id = :viviendaId "
        + "and r.pregunta.encuesta.id = :encuestaId and r.opcion.esCorrecta = true")
    long countCorrectasByViviendaAndEncuesta(@Param("viviendaId") Long viviendaId,
                                             @Param("encuestaId") Long encuestaId);
}
