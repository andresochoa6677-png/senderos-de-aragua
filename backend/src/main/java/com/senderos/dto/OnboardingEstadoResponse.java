package com.senderos.dto;

public record OnboardingEstadoResponse(
    boolean consentimientoAceptado,
    boolean encuestaInicialCompleta,
    String siguientePaso
) {}
