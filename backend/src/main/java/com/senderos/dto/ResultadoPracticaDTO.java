package com.senderos.dto;

import java.util.List;

/**
 * Resultado de una actividad de práctica con feedback educativo y la
 * puntuación acumulada de la vivienda (total, correctas y rachas).
 */
public record ResultadoPracticaDTO(
        boolean correcto,
        String explicacion,
        List<DetalleItemDTO> detalleItems,
        PuntuacionDTO puntuacion) {

    public record DetalleItemDTO(String itemId, boolean correcto) {}

    public record PuntuacionDTO(long total, long correctas, int racha, int rachaMaxima) {}
}
