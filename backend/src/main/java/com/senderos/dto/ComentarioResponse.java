package com.senderos.dto;

import java.time.Instant;

/** Aporte del propio residente (vista "Mis aportes"): solo sus datos. */
public record ComentarioResponse(
        Long id,
        String tipo,
        String temaDeclarado,
        String contenido,
        Instant creadoEn
) {}
