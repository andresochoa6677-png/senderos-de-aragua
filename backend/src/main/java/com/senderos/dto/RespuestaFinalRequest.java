package com.senderos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Envío de la encuesta FINAL. La encuesta se resuelve SIEMPRE en el
 * servidor (tipo FINAL): el cliente no envía ids de encuesta.
 */
public record RespuestaFinalRequest(
    @NotNull @Size(min = 1) List<EncuestaRespuestaRequest.RespuestaItem> respuestas
) {}
