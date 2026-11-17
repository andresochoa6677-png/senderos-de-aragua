package com.senderos.dto;

import java.time.Instant;

/**
 * Estado del estudio para el panel de administración: estado de la
 * posprueba (habilitación manual con trazabilidad) y contadores de
 * participación.
 */
public record PanelEstudioDTO(
    boolean pospruebaHabilitada,
    Instant pospruebaHabilitadaEn,
    long viviendasRegistradas,
    long conConsentimiento,
    long conPreprueba,
    long conPostprueba,
    long comentarios
) {}
