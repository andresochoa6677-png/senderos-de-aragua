package com.senderos.dto;

/**
 * Respuesta del registro de residentes: la cuenta asignada más la
 * señal de si el correo de verificación pudo enviarse. Si
 * correoEnviado=false el frontend debe indicar que hubo un problema
 * con el envío (la cuenta sí quedó creada y pendiente).
 */
public record RegistroResponse(
        UsuarioResponse usuario,
        boolean correoEnviado
) {}
