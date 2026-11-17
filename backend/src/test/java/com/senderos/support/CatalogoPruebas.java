package com.senderos.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Catálogo de viviendas del censo oficial (V25) para los tests.
 *
 * Los tests registran cuentas reales que consumen viviendas del catálogo
 * compartido (H2 persiste entre clases del mismo contexto). {@link #campos()}
 * entrega SIEMPRE la siguiente vivienda no usada en este JVM (rotativa y
 * determinista): con ~50 registros de la suite completa hay de sobra dentro
 * de las 139 del censo.
 */
public final class CatalogoPruebas {

    /** Censo oficial: bloque → cantidad de casas (orden alfabético español). */
    private static final Map<String, Integer> CENSO = new LinkedHashMap<>();
    static {
        CENSO.put("A", 4); CENSO.put("B", 4); CENSO.put("C", 4);
        CENSO.put("D", 4); CENSO.put("E", 4); CENSO.put("F", 4);
        CENSO.put("G", 12);
        CENSO.put("H", 4); CENSO.put("I", 4); CENSO.put("J", 4);
        CENSO.put("K", 4); CENSO.put("L", 4); CENSO.put("M", 4);
        CENSO.put("N", 18);
        CENSO.put("Ñ", 4);
        CENSO.put("O", 4); CENSO.put("P", 4); CENSO.put("Q", 4);
        CENSO.put("R", 4);
        CENSO.put("RR", 4);
        CENSO.put("S", 4); CENSO.put("T", 4); CENSO.put("U", 4);
        CENSO.put("V", 4); CENSO.put("W", 4); CENSO.put("X", 4);
        CENSO.put("Y", 4);
        CENSO.put("Z", 9);
    }

    /** Códigos en orden de censo (A-1…A-4, B-1…, …, Ñ-4, Z-9). */
    public static final List<String> CODIGOS;
    static {
        List<String> codigos = new ArrayList<>();
        CENSO.forEach((bloque, casas) -> {
            for (int i = 1; i <= casas; i++) codigos.add(bloque + "-" + i);
        });
        CODIGOS = List.copyOf(codigos);
    }

    /**
     * Códigos usados por tests que eligen viviendas CONCRETAS (colisiones,
     * liberación tras limpieza…): la rotación de {@link #campos()} nunca
     * los entrega, así que su ocupación es determinista. La suite completa
     * consume ~100 viviendas y el censo tiene 139, de sobra.
     */
    public static final List<String> RESERVADAS =
        List.of("G-5", "Ñ-3", "Ñ-4", "Z-8", "Z-9");

    private static final List<String> ROTACION = CODIGOS.stream()
        .filter(c -> !RESERVADAS.contains(c))
        .toList();
    private static final AtomicInteger INDICE = new AtomicInteger(0);

    private CatalogoPruebas() {}

    /** Siguiente código del censo no usado en este JVM (rotativo). */
    public static String siguiente() {
        return ROTACION.get(Math.floorMod(INDICE.getAndIncrement(), ROTACION.size()));
    }

    /** Campos JSON "bloque":"…","numero":N de la siguiente vivienda libre. */
    public static String campos() {
        return camposDe(siguiente());
    }

    /** Campos JSON de una vivienda concreta del censo ("A-1", "Ñ-2"…). */
    public static String camposDe(String codigo) {
        int separador = codigo.indexOf('-');
        return "\"bloque\":\"" + codigo.substring(0, separador)
            + "\",\"numero\":" + codigo.substring(separador + 1);
    }
}
