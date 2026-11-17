package com.senderos.dto;

import java.time.Instant;
import java.util.List;

/** Progreso educativo de la vivienda autenticada. */
public record ProgresoViviendaDTO(
        int totalLecciones,
        int completadas,
        int porcentaje,
        Instant ultimaActividad,
        Long leccionActualId,
        List<ItemProgresoDTO> lecciones
) {
    public record ItemProgresoDTO(
            Long leccionId,
            Integer orden,
            String titulo,
            String icono,
            String color,
            String estado,
            Instant iniciadoEn,
            Instant completadoEn
    ) {}
}
