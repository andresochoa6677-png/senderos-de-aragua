package com.senderos.dto;

import java.time.Instant;
import java.util.List;

/**
 * Resultado de calificar un intento de ejercicio. Incluye la explicacion
 * pedagogica pero no revela la respuesta correcta. En actividades
 * CLASIFICAR agrega el detalle acierto/fallo por elemento para permitir
 * corregir; el detalle nunca indica el destino correcto.
 */
public record ResultadoIntentoDTO(
        Long ejercicioId,
        boolean correcto,
        boolean primeraCorrecta,
        Integer puntosObtenidos,
        Integer puntosTotales,
        String explicacion,
        Instant creadoEn,
        boolean leccionCompletada,
        Long siguienteLeccionId,
        List<DetalleItemDTO> detalleItems) {

    /** Estado de cada elemento de una actividad CLASIFICAR tras el intento. */
    public record DetalleItemDTO(String itemId, boolean correcto) {}
}
