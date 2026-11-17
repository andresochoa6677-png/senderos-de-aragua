package com.senderos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record EncuestaRespuestaRequest(
    @NotNull Long encuestaId,
    @NotNull @Size(min = 1) List<RespuestaItem> respuestas
) {
    public record RespuestaItem(
        @NotNull Long preguntaId,
        Long opcionId,
        @Size(max = 500) String valorTexto
    ) {}
}
