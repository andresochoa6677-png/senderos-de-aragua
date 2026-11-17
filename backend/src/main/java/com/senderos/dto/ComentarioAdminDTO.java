package com.senderos.dto;

import java.time.Instant;

/**
 * Registro para el equipo investigador (ruta administrativa): incluye
 * vivienda para la matriz de análisis cualitativo
 * (Fecha | Vivienda | Tipo | Comentario | Categoría temática).
 * Solo accesible con rol ADMIN.
 */
public record ComentarioAdminDTO(
        Long id,
        Instant fecha,
        String vivienda,
        String tipo,
        String temaDeclarado,
        String contenido,
        String categoriaTematica
) {}
