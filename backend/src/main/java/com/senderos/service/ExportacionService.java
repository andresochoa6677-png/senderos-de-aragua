package com.senderos.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exportaciones CSV para la investigación. Cada método devuelve el
 * contenido del archivo (con BOM para que Excel respete los acentos) y el
 * controlador le pone Content-Disposition con la fecha en el nombre.
 *
 * Formato: cabeceras claras en la primera fila, campos entre comillas dobles
 * (una comilla interior se escapa doblandola), filas separadas por CRLF.
 */
@Service
public class ExportacionService {

    private static final ZoneId ZONA = ZoneId.of("America/Caracas");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JdbcTemplate jdbcTemplate;

    public ExportacionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static String celda(Object valor) {
        if (valor == null) {
            return "\"\"";
        }
        String texto = valor instanceof Timestamp ts
            ? ts.toInstant().atZone(ZONA).format(FECHA)
            : valor.toString();
        return "\"" + texto.replace("\"", "\"\"") + "\"";
    }

    private static String armar(List<String[]> filas) {
        StringBuilder sb = new StringBuilder("\uFEFF");
        for (String[] fila : filas) {
            for (int i = 0; i < fila.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(fila[i]);
            }
            sb.append("\r\n");
        }
        return sb.toString();
    }

    /**
     * Filas de respuesta por vivienda para una encuesta: codigo → [celda por orden].
     * Conocimiento → 1 si la opción elegida era la clave, 0 si no.
     * Prácticas y percepción (escala 1-5) → el valor numérico guardado
     * (respuesta.valor_texto = '1'..'5').
     */
    private Map<String, Object[]> respuestasPorVivienda(String tipoEncuesta, int totalItems) {
        List<Map<String, Object>> filas = jdbcTemplate.queryForList(
            "SELECT v.codigo AS codigo, "
                + "CASE WHEN p.tipo_pregunta = 'CONOCIMIENTO' "
                + "      THEN COALESCE(CAST(o.es_correcta AS CHAR), '0') "
                + "      ELSE COALESCE(NULLIF(r.valor_texto, ''), o.valor) END AS respuesta, "
                + "p.orden "
                + "FROM respuesta r "
                + "JOIN pregunta p ON p.id = r.pregunta_id "
                + "JOIN encuesta e ON e.id = p.encuesta_id AND e.tipo = '" + tipoEncuesta + "' "
                + "JOIN vivienda v ON v.id = r.vivienda_id "
                + "LEFT JOIN opcion o ON o.id = r.opcion_id "
                + "ORDER BY v.codigo, p.orden");
        Map<String, Object[]> porVivienda = new LinkedHashMap<>();
        for (Map<String, Object> fila : filas) {
            String codigo = (String) fila.get("codigo");
            int orden = ((Number) fila.get("orden")).intValue();
            Object[] filaVivienda = porVivienda.computeIfAbsent(codigo,
                k -> new Object[totalItems + 1]);
            if (orden >= 1 && orden <= totalItems) {
                filaVivienda[orden] = fila.get("respuesta");
            }
        }
        return porVivienda;
    }

    private Map<String, Object[]> fechasPorEncuesta(String tipoEncuesta) {
        Map<String, Object[]> fechas = new HashMap<>();
        for (Map<String, Object> fila : jdbcTemplate.queryForList(
                "SELECT v.codigo AS codigo, MIN(r.respondido_en) AS fecha "
                    + "FROM respuesta r JOIN pregunta p ON p.id = r.pregunta_id "
                    + "JOIN encuesta e ON e.id = p.encuesta_id AND e.tipo = '" + tipoEncuesta + "' "
                    + "JOIN vivienda v ON v.id = r.vivienda_id GROUP BY v.codigo")) {
            fechas.put((String) fila.get("codigo"), new Object[]{fila.get("fecha")});
        }
        return fechas;
    }

    private Map<String, Number> puntajes(String tipoEncuesta) {
        Map<String, Number> puntajes = new HashMap<>();
        for (Map<String, Object> fila : jdbcTemplate.queryForList(
                "SELECT v.codigo AS codigo, COUNT(*) AS aciertos "
                    + "FROM respuesta r JOIN opcion o ON o.id = r.opcion_id "
                    + "JOIN pregunta p ON p.id = r.pregunta_id "
                    + "JOIN encuesta e ON e.id = p.encuesta_id "
                    + "JOIN vivienda v ON v.id = r.vivienda_id "
                    + "WHERE e.tipo = '" + tipoEncuesta + "' AND o.es_correcta = 1 "
                    + "GROUP BY v.codigo")) {
            puntajes.put((String) fila.get("codigo"), (Number) fila.get("aciertos"));
        }
        return puntajes;
    }

    /** Preprueba: 18 respuestas + puntaje por vivienda. */
    @Transactional(readOnly = true)
    public String csvPreprueba() {
        int items = 18;
        Map<String, Object[]> respuestas = respuestasPorVivienda("INICIAL", items);
        Map<String, Object[]> fechas = fechasPorEncuesta("INICIAL");
        Map<String, Number> aciertos = puntajes("INICIAL");

        List<String[]> filas = new ArrayList<>();
        String[] cabecera = new String[items + 3];
        cabecera[0] = "vivienda_codigo";
        cabecera[1] = "fecha_preprueba";
        cabecera[2] = "puntaje_conocimiento_0a10";
        for (int i = 1; i <= items; i++) {
            cabecera[i + 2] = String.format("respuesta_%02d", i);
        }
        filas.add(cabecera);
        for (Map.Entry<String, Object[]> entrada : respuestas.entrySet()) {
            String codigo = entrada.getKey();
            String[] fila = new String[items + 3];
            fila[0] = celda(codigo);
            Object[] fecha = fechas.get(codigo);
            fila[1] = celda(fecha != null ? fecha[0] : null);
            fila[2] = celda(aciertos.getOrDefault(codigo, 0));
            for (int i = 1; i <= items; i++) {
                fila[i + 2] = celda(entrada.getValue()[i]);
            }
            filas.add(fila);
        }
        return armar(filas);
    }

    /** Postprueba: fecha de aplicación + 22 respuestas + puntaje por vivienda. */
    @Transactional(readOnly = true)
    public String csvPostprueba() {
        int items = 22;
        Map<String, Object[]> respuestas = respuestasPorVivienda("FINAL", items);
        Map<String, Object[]> fechas = fechasPorEncuesta("FINAL");
        Map<String, Number> aciertos = puntajes("FINAL");

        List<String[]> filas = new ArrayList<>();
        String[] cabecera = new String[items + 3];
        cabecera[0] = "vivienda_codigo";
        cabecera[1] = "fecha_posprueba";
        cabecera[2] = "puntaje_conocimiento_0a10";
        for (int i = 1; i <= items; i++) {
            cabecera[i + 2] = String.format("respuesta_%02d", i);
        }
        filas.add(cabecera);
        for (Map.Entry<String, Object[]> entrada : respuestas.entrySet()) {
            String codigo = entrada.getKey();
            String[] fila = new String[items + 3];
            fila[0] = celda(codigo);
            Object[] fecha = fechas.get(codigo);
            fila[1] = celda(fecha != null ? fecha[0] : null);
            fila[2] = celda(aciertos.getOrDefault(codigo, 0));
            for (int i = 1; i <= items; i++) {
                fila[i + 2] = celda(entrada.getValue()[i]);
            }
            filas.add(fila);
        }
        return armar(filas);
    }

    /** Métricas de uso por vivienda. */
    @Transactional(readOnly = true)
    public String csvUso() {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"vivienda_codigo", "fecha_registro", "sesiones_iniciadas",
            "lecciones_completadas", "intentos_ejercicios", "intentos_practica",
            "intentos_practica_correctos", "puntaje_pre_0a10", "puntaje_post_0a10", "comentarios"});
        for (Map<String, Object> fila : jdbcTemplate.queryForList(
                "SELECT v.codigo AS codigo, "
                    + "(SELECT MIN(u.creado_en) FROM usuario u WHERE u.vivienda_id = v.id) AS fecha_registro, "
                    + "(SELECT COUNT(*) FROM registro_acceso ra WHERE ra.vivienda_id = v.id AND ra.modulo = 'SESION') AS sesiones, "
                    + "(SELECT COUNT(*) FROM progreso_leccion p WHERE p.vivienda_id = v.id AND p.completado_en IS NOT NULL) AS lecciones, "
                    + "(SELECT COUNT(*) FROM intento_ejercicio i WHERE i.vivienda_id = v.id) AS intentos_ejercicios, "
                    + "(SELECT COUNT(*) FROM intento_practica ip WHERE ip.vivienda_id = v.id) AS intentos_practica, "
                    + "(SELECT COUNT(*) FROM intento_practica ip2 WHERE ip2.vivienda_id = v.id AND ip2.correcto = 1) AS practica_correctos, "
                    + "(SELECT COUNT(*) FROM respuesta r JOIN opcion o ON o.id = r.opcion_id "
                    + "   JOIN pregunta p ON p.id = r.pregunta_id JOIN encuesta e ON e.id = p.encuesta_id "
                    + "   WHERE r.vivienda_id = v.id AND e.tipo = 'INICIAL' AND o.es_correcta = 1) AS puntaje_pre, "
                    + "(SELECT COUNT(*) FROM respuesta r JOIN opcion o ON o.id = r.opcion_id "
                    + "   JOIN pregunta p ON p.id = r.pregunta_id JOIN encuesta e ON e.id = p.encuesta_id "
                    + "   WHERE r.vivienda_id = v.id AND e.tipo = 'FINAL' AND o.es_correcta = 1) AS puntaje_post, "
                    + "(SELECT COUNT(*) FROM comentario c WHERE c.vivienda_id = v.id) AS comentarios "
                    + "FROM vivienda v WHERE EXISTS (SELECT 1 FROM usuario u WHERE u.vivienda_id = v.id) "
                    + "ORDER BY v.codigo")) {
            filas.add(new String[]{
                celda(fila.get("codigo")), celda(fila.get("fecha_registro")),
                celda(fila.get("sesiones")), celda(fila.get("lecciones")),
                celda(fila.get("intentos_ejercicios")), celda(fila.get("intentos_practica")),
                celda(fila.get("practica_correctos")), celda(fila.get("puntaje_pre")),
                celda(fila.get("puntaje_post")), celda(fila.get("comentarios"))});
        }
        return armar(filas);
    }

    /** Comentarios con categoría temática. */
    @Transactional(readOnly = true)
    public String csvComentarios() {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"id", "fecha", "vivienda_codigo", "tipo", "tema_declarado", "contenido", "categoria_tematica"});
        for (Map<String, Object> fila : jdbcTemplate.queryForList(
                "SELECT c.id, c.creado_en, v.codigo AS codigo, c.tipo, c.tema_declarado, c.contenido, c.categoria_tematica "
                    + "FROM comentario c JOIN vivienda v ON v.id = c.vivienda_id ORDER BY c.creado_en")) {
            filas.add(new String[]{
                celda(fila.get("id")), celda(fila.get("creado_en")), celda(fila.get("codigo")),
                celda(fila.get("tipo")), celda(fila.get("tema_declarado")),
                celda(fila.get("contenido")), celda(fila.get("categoria_tematica"))});
        }
        return armar(filas);
    }
}
