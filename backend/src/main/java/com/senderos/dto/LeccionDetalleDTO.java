package com.senderos.dto;

import com.senderos.entity.Leccion;
import com.senderos.entity.ProgresoLeccion;

import java.time.Instant;

/** Detalle de leccion con contenido markdown y estado para la vivienda actual. */
public record LeccionDetalleDTO(
        Long id,
        Integer orden,
        String titulo,
        String resumen,
        String imagen,
        String contenido,
        String ejemplo,
        String ideaClave,
        Instant iniciadoEn,
        Instant completadoEn
) {
    public static LeccionDetalleDTO de(Leccion leccion, ProgresoLeccion progreso) {
        return new LeccionDetalleDTO(
                leccion.getId(),
                leccion.getOrden(),
                leccion.getTitulo(),
                leccion.getResumen(),
                leccion.getImagen(),
                leccion.getContenido(),
                leccion.getEjemplo(),
                leccion.getIdeaClave(),
                progreso != null ? progreso.getIniciadoEn() : null,
                progreso != null ? progreso.getCompletadoEn() : null
        );
    }
}
