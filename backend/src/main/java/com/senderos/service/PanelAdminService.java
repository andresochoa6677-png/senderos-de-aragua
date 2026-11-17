package com.senderos.service;

import com.senderos.entity.EstudioConfig;
import com.senderos.entity.RegistroAcceso;
import com.senderos.exception.ConflictException;
import com.senderos.repository.EstudioConfigRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Panel de administración: control MANUAL de la posprueba y contadores
 * de participación.
 *
 * La encuesta final está DESHABILITADA por defecto. El administrador la
 * habilita y deshabilita con un botón desde el panel; cada acción queda
 * registrada con fecha/hora y responsable. No hay cronómetro ni ventana
 * por días: habilitada = disponible de inmediato para todos (mientras
 * no se deshabilite).
 */
@Service
public class PanelAdminService {

    private final EstudioConfigRepository estudioConfigRepository;
    private final com.senderos.repository.RegistroAccesoRepository registroAccesoRepository;
    private final com.senderos.repository.ViviendaRepository viviendaRepository;
    private final JdbcTemplate jdbcTemplate;

    public PanelAdminService(EstudioConfigRepository estudioConfigRepository,
                             com.senderos.repository.RegistroAccesoRepository registroAccesoRepository,
                             com.senderos.repository.ViviendaRepository viviendaRepository,
                             JdbcTemplate jdbcTemplate) {
        this.estudioConfigRepository = estudioConfigRepository;
        this.registroAccesoRepository = registroAccesoRepository;
        this.viviendaRepository = viviendaRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public record Habilitacion(boolean habilitada, Instant habilitadaEn) {}

    /** Fuente única del estado de la posprueba para TODO el backend. */
    public Habilitacion habilitacion() {
        EstudioConfig estudio = estudioConfigRepository.findById(1L).orElse(null);
        boolean habilitada = estudio != null && estudio.isPospruebaHabilitada();
        return new Habilitacion(habilitada, habilitada ? estudio.getPospruebaHabilitadaEn() : null);
    }

    /** Habilita la posprueba de inmediato (409 si ya está habilitada). */
    @Transactional
    public Instant habilitarPosprueba(String emailAdmin) {
        EstudioConfig estudio = estudioConfigRepository.findById(1L)
            .orElseThrow(() -> new ConflictException("Configuración del estudio no encontrada"));
        if (estudio.isPospruebaHabilitada()) {
            throw new ConflictException("La encuesta final ya está habilitada");
        }
        Instant ahora = Instant.now();
        estudio.setPospruebaHabilitada(true);
        estudio.setPospruebaHabilitadaEn(ahora);
        estudio.setPospruebaHabilitadaPor(emailAdmin);
        estudioConfigRepository.save(estudio);
        return ahora;
    }

    /** Deshabilita la posprueba (409 si ya está deshabilitada). */
    @Transactional
    public void deshabilitarPosprueba() {
        EstudioConfig estudio = estudioConfigRepository.findById(1L)
            .orElseThrow(() -> new ConflictException("Configuración del estudio no encontrada"));
        if (!estudio.isPospruebaHabilitada()) {
            throw new ConflictException("La encuesta final ya está deshabilitada");
        }
        estudio.setPospruebaHabilitada(false);
        // La marca de la ÚLTIMA habilitación se conserva como trazabilidad.
        estudioConfigRepository.save(estudio);
    }

    /** Estado del estudio + contadores de participación del panel. */
    @Transactional(readOnly = true)
    public com.senderos.dto.PanelEstudioDTO estadoPanel() {
        Habilitacion habilitacion = habilitacion();
        Map<String, Object> conteos = jdbcTemplate.queryForMap(
            "SELECT "
                + "(SELECT COUNT(*) FROM usuario WHERE vivienda_id IS NOT NULL) AS registradas, "
                + "(SELECT COUNT(*) FROM consentimiento WHERE aceptado = 1) AS con_consentimiento, "
                + "(SELECT COUNT(DISTINCT r.vivienda_id) FROM respuesta r "
                + "   JOIN pregunta p ON p.id = r.pregunta_id "
                + "   JOIN encuesta e ON e.id = p.encuesta_id WHERE e.tipo = 'INICIAL') AS con_preprueba, "
                + "(SELECT COUNT(DISTINCT r.vivienda_id) FROM respuesta r "
                + "   JOIN pregunta p ON p.id = r.pregunta_id "
                + "   JOIN encuesta e ON e.id = p.encuesta_id WHERE e.tipo = 'FINAL') AS con_postprueba, "
                + "(SELECT COUNT(*) FROM comentario) AS comentarios");
        return new com.senderos.dto.PanelEstudioDTO(
            habilitacion.habilitada(),
            habilitacion.habilitadaEn(),
            ((Number) conteos.get("registradas")).longValue(),
            ((Number) conteos.get("con_consentimiento")).longValue(),
            ((Number) conteos.get("con_preprueba")).longValue(),
            ((Number) conteos.get("con_postprueba")).longValue(),
            ((Number) conteos.get("comentarios")).longValue());
    }

    /**
     * Registro COMPLETO de una vivienda para la investigación: accesos
     * (sesiones y módulos consultados), lecciones completadas con
     * puntaje, intentos de práctica y comentarios. Trazabilidad crítica
     * del estudio — debe ser preciso y auditable.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> registroDeVivienda(String codigo) {
        com.senderos.entity.Vivienda vivienda = viviendaRepository.findByCodigo(codigo)
            .orElseThrow(() -> new com.senderos.exception.NotFoundException("Vivienda no encontrada"));
        Long viviendaId = vivienda.getId();

        long sesionesIniciadas = registroAccesoRepository.countByViviendaIdAndModulo(viviendaId, "SESION");
        List<RegistroAcceso> accesos = registroAccesoRepository
            .findByViviendaIdOrderByFechaHoraDesc(viviendaId, org.springframework.data.domain.PageRequest.of(0, 200));
        List<Object[]> porModulo = registroAccesoRepository.conteoPorModulo(viviendaId);

        List<Map<String, Object>> lecciones = jdbcTemplate.queryForList(
            "SELECT l.orden, l.titulo, p.completado_en, "
                + "(SELECT COALESCE(SUM(ej.puntos), 0) FROM ejercicio ej "
                + "   WHERE ej.leccion_id = l.id AND EXISTS (SELECT 1 FROM intento_ejercicio i "
                + "     WHERE i.ejercicio_id = ej.id AND i.vivienda_id = p.vivienda_id AND i.correcto = 1)) AS puntaje "
                + "FROM progreso_leccion p JOIN leccion l ON l.id = p.leccion_id "
                + "WHERE p.vivienda_id = ? AND p.completado_en IS NOT NULL ORDER BY l.orden", viviendaId);

        Map<String, Object> practica = jdbcTemplate.queryForMap(
            "SELECT COUNT(*) AS intentos, COALESCE(SUM(correcto), 0) AS correctos "
                + "FROM intento_practica WHERE vivienda_id = ?", viviendaId);

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("codigo", codigo);
        resultado.put("sesionesIniciadas", sesionesIniciadas);
        resultado.put("accesos", accesos.stream().map(a -> Map.of(
                "fechaHora", a.getFechaHora(),
                "ipOrigen", a.getIpOrigen() == null ? "" : a.getIpOrigen(),
                "modulo", a.getModulo())).toList());
        resultado.put("modulosConsultados", porModulo.stream()
            .map(fila -> Map.of("modulo", fila[0], "consultas", ((Number) fila[1]).longValue())).toList());
        resultado.put("leccionesCompletadas", lecciones);
        resultado.put("practica", practica);
        resultado.put("comentarios", jdbcTemplate.queryForMap(
            "SELECT COUNT(*) AS total, MAX(creado_en) AS ultimo FROM comentario WHERE vivienda_id = ?", viviendaId));
        return resultado;
    }

    /** Una fila por vivienda con cuenta: registro, consentimiento, pre y post. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> viviendasPanel() {
        boolean pospruebaHabilitada = habilitacion().habilitada();
        List<Map<String, Object>> filas = jdbcTemplate.queryForList(
            "SELECT v.codigo AS codigo, "
                + "(SELECT MIN(u.creado_en) FROM usuario u WHERE u.vivienda_id = v.id) AS fecha_registro, "
                + "EXISTS (SELECT 1 FROM consentimiento c WHERE c.vivienda_id = v.id AND c.aceptado = 1) AS con_consentimiento, "
                + "(SELECT MIN(r.respondido_en) FROM respuesta r JOIN pregunta p ON p.id = r.pregunta_id "
                + "   JOIN encuesta e ON e.id = p.encuesta_id "
                + "   WHERE r.vivienda_id = v.id AND e.tipo = 'INICIAL') AS fecha_preprueba, "
                + "EXISTS (SELECT 1 FROM respuesta rf JOIN pregunta pf ON pf.id = rf.pregunta_id "
                + "   JOIN encuesta ef ON ef.id = pf.encuesta_id "
                + "   WHERE rf.vivienda_id = v.id AND ef.tipo = 'FINAL') AS con_postprueba, "
                + "(SELECT COUNT(*) FROM respuesta r JOIN opcion o ON o.id = r.opcion_id "
                + "   JOIN pregunta p ON p.id = r.pregunta_id JOIN encuesta e ON e.id = p.encuesta_id "
                + "   WHERE r.vivienda_id = v.id AND e.tipo = 'INICIAL' AND o.es_correcta = 1) AS puntaje_pre, "
                + "(SELECT COUNT(*) FROM respuesta r JOIN opcion o ON o.id = r.opcion_id "
                + "   JOIN pregunta p ON p.id = r.pregunta_id JOIN encuesta e ON e.id = p.encuesta_id "
                + "   WHERE r.vivienda_id = v.id AND e.tipo = 'FINAL' AND o.es_correcta = 1) AS puntaje_post "
                + "FROM vivienda v "
                + "WHERE EXISTS (SELECT 1 FROM usuario u WHERE u.vivienda_id = v.id) ORDER BY v.codigo");
        return filas.stream().map(fila -> {
            Map<String, Object> fila2 = new HashMap<>(fila);
            normalizarBoolean(fila2, "con_consentimiento");
            normalizarBoolean(fila2, "con_postprueba");
            boolean conPreprueba = fila.get("fecha_preprueba") != null;
            fila2.put("con_preprueba", conPreprueba);
            Object respondio = fila.get("con_postprueba");
            boolean respondioFinal = respondio instanceof Boolean b
                ? b
                : ((Number) respondio).intValue() == 1;
            fila2.put("disponible_final", pospruebaHabilitada && conPreprueba && !respondioFinal);
            Object fecha = fila.get("fecha_preprueba");
            if (fecha instanceof java.sql.Timestamp ts) {
                fila2.put("fecha_preprueba", ts.toInstant());
            }
            Object registro = fila.get("fecha_registro");
            if (registro instanceof java.sql.Timestamp ts2) {
                fila2.put("fecha_registro", ts2.toInstant());
            }
            return fila2;
        }).toList();
    }

    private static void normalizarBoolean(Map<String, Object> fila, String clave) {
        Object valor = fila.get(clave);
        if (valor instanceof Boolean b) {
            fila.put(clave, b);
        } else if (valor instanceof Number n) {
            fila.put(clave, n.intValue() == 1);
        }
    }
}
