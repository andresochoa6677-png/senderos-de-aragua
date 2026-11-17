package com.senderos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de participación del residente. La vivienda y el usuario se
 * derivan de la sesión autenticada: nunca se aceptan del cliente.
 */
public record ComentarioRequest(
        @NotBlank @Size(max = 20) String tipo,
        @Size(max = 40) String temaDeclarado,
        @NotBlank @Size(max = 500) String contenido
) {}
