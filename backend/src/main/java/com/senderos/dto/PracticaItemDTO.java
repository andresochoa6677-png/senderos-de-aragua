package com.senderos.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Actividad de práctica para el cliente, sin solución ni explicación:
 * la explicación llega solo como resultado de responder.
 */
public record PracticaItemDTO(
        Long id,
        String modo,
        String tipo,
        String escenario,
        String enunciado,
        String imagen,
        JsonNode contenido
) {}
