package com.senderos.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Respuesta enviada por la vivienda a un ejercicio. Para
 * OPCION_MULTIPLE y VERDADERO_FALSO es una opcion de la lista;
 * para ORDENAR es el orden elegido separado por "|".
 */
public record RespuestaEjercicioDTO(
        @NotBlank(message = "La respuesta es obligatoria")
        String respuesta) {
}
