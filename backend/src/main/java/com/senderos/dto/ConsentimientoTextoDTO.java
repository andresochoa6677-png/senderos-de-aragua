package com.senderos.dto;

/**
 * Documento de consentimiento vigente para la pantalla del onboarding:
 * la versión, el texto íntegro y el hash SHA-256 los fija el servidor
 * (fuente única: consentimiento/REG-2.0.txt).
 */
public record ConsentimientoTextoDTO(
    String version,
    String texto,
    String hashTexto
) {}
