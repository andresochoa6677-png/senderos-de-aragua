package com.senderos.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.senderos.entity.Ejercicio;
import com.senderos.entity.IntentoEjercicio;

import java.time.Instant;
import java.util.List;

/**
 * Ejercicio para el cliente. No incluye la respuesta correcta: se
 * entrega solo como resultado de calificar un intento. Las actividades
 * CLASIFICAR exponen su configuracion (elementos, destinos, tablero y
 * refuerzo) sin el mapa de destinos correctos.
 */
public record EjercicioDTO(
        Long id,
        Long leccionId,
        Integer orden,
        String tipo,
        String enunciado,
        List<String> opciones,
        JsonNode config,
        Integer puntos,
        boolean resuelto,
        Integer intentos,
        Instant primeraCorrectaEn) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static EjercicioDTO de(Ejercicio e, List<IntentoEjercicio> intentos) {
        boolean resuelto = intentos.stream().anyMatch(i -> Boolean.TRUE.equals(i.getCorrecto()));
        boolean esClasificar = "CLASIFICAR".equals(e.getTipo());
        return new EjercicioDTO(
                e.getId(), e.getLeccion().getId(), e.getOrden(), e.getTipo(),
                e.getEnunciado(), esClasificar ? List.of() : parseOpciones(e.getOpciones()),
                esClasificar ? parseConfig(e.getOpciones()) : null,
                e.getPuntos(),
                resuelto, intentos.size(),
                intentos.stream()
                        .filter(i -> Boolean.TRUE.equals(i.getCorrecto()))
                        .findFirst().map(IntentoEjercicio::getCreadoEn)
                        .orElse(null));
    }

    /** El JSON de opciones es un array simple de strings. */
    static List<String> parseOpciones(String json) {
        try {
            return MAPPER.readValue(limpiarJson(json), MAPPER.getTypeFactory()
                    .constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            return List.of();
        }
    }

    /** Configuracion de una actividad CLASIFICAR, sin la solucion. */
    public static JsonNode parseConfig(String json) {
        try {
            return MAPPER.readTree(limpiarJson(json));
        } catch (Exception e) {
            return MAPPER.createObjectNode();
        }
    }

    /**
     * H2 (perfil de tests) devuelve la columna JSON serializada como
     * string de JSON (comillas externas + escapes); MySQL la devuelve
     * como texto plano. Se normaliza en ambos casos antes de parsear.
     */
    public static String limpiarJson(String json) {
        if (json == null || json.isBlank()) {
            return "{}";
        }
        String limpio = json.trim();
        if (limpio.startsWith("\"") && limpio.endsWith("\"")) {
            limpio = limpio.substring(1, limpio.length() - 1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        }
        return limpio;
    }
}
