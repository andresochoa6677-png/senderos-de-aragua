package com.senderos.dto;

import com.senderos.entity.Leccion;
import com.senderos.entity.ProgresoLeccion;

import java.time.Instant;

/**
 * Tarjeta de leccion para la lista de Aprendizaje. Incluye el estado del
 * recorrido obligatorio: COMPLETADA, ACTUAL (desbloqueada) o BLOQUEADA,
 * mas el avance de ejercicios de la vivienda.
 */
public record LeccionResumenDTO(
        Long id,
        Integer orden,
        String titulo,
        String resumen,
        String icono,
        String color,
        String imagen,
        Instant iniciadoEn,
        Instant completadoEn,
        String estado,
        int ejerciciosTotales,
        int ejerciciosCorrectos
) {
    public static LeccionResumenDTO de(Leccion leccion, ProgresoLeccion progreso,
                                       String estado, int ejerciciosTotales, int ejerciciosCorrectos) {
        return new LeccionResumenDTO(
                leccion.getId(),
                leccion.getOrden(),
                leccion.getTitulo(),
                leccion.getResumen(),
                leccion.getIcono(),
                leccion.getColor(),
                leccion.getImagen(),
                progreso != null ? progreso.getIniciadoEn() : null,
                progreso != null ? progreso.getCompletadoEn() : null,
                estado,
                ejerciciosTotales,
                ejerciciosCorrectos
        );
    }
}
