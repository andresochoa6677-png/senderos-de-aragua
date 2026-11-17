package com.senderos.dto;

/**
 * Estado de la posprueba para una vivienda: habilitada manualmente por
 * el administrador (sin cronómetro) y si la vivienda ya respondió.
 */
public record EncuestaFinalDisponibleDTO(
    boolean disponible,
    boolean yaRespondida
) {}
