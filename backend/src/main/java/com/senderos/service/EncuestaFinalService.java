package com.senderos.service;

import com.senderos.dto.EncuestaFinalDisponibleDTO;
import com.senderos.dto.EncuestaResponse;
import com.senderos.entity.Encuesta;
import com.senderos.entity.Opcion;
import com.senderos.entity.Pregunta;
import com.senderos.entity.Respuesta;
import com.senderos.exception.BusinessException;
import com.senderos.exception.ConflictException;
import com.senderos.exception.NotFoundException;
import com.senderos.repository.EncuestaRepository;
import com.senderos.repository.OpcionRepository;
import com.senderos.repository.PreguntaRepository;
import com.senderos.repository.RespuestaRepository;
import com.senderos.repository.ViviendaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encuesta FINAL (posprueba), habilitación MANUAL del administrador
 * (ver PanelAdminService): deshabilitada por defecto; el admin la
 * habilita y deshabilita desde el panel, sin cronómetro ni ventana de
 * días. La disponibilidad NO depende de haber completado lecciones:
 * basta haber respondido la preprueba. El aviso a los residentes lo da
 * el equipo investigador por WhatsApp; el trabajo programado diario
 * SOLO registra en el log el estado (no envía correos). El
 * consentimiento REG-2.0 ya cubre la participación en la medición final.
 */
@Service
public class EncuestaFinalService {

    private static final Logger log = LoggerFactory.getLogger(EncuestaFinalService.class);

    private static final String TIPO_PREPRUEBA = "INICIAL";
    private static final String TIPO_POSPRUEBA = "FINAL";

    private final EncuestaRepository encuestaRepository;
    private final PreguntaRepository preguntaRepository;
    private final OpcionRepository opcionRepository;
    private final RespuestaRepository respuestaRepository;
    private final ViviendaRepository viviendaRepository;
    private final PanelAdminService panelAdminService;
    private final JdbcTemplate jdbcTemplate;

    public EncuestaFinalService(EncuestaRepository encuestaRepository,
                                PreguntaRepository preguntaRepository,
                                OpcionRepository opcionRepository,
                                RespuestaRepository respuestaRepository,
                                ViviendaRepository viviendaRepository,
                                PanelAdminService panelAdminService,
                                JdbcTemplate jdbcTemplate) {
        this.encuestaRepository = encuestaRepository;
        this.preguntaRepository = preguntaRepository;
        this.opcionRepository = opcionRepository;
        this.respuestaRepository = respuestaRepository;
        this.viviendaRepository = viviendaRepository;
        this.panelAdminService = panelAdminService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** La posprueba solo requiere que el administrador la haya habilitado. */
    private boolean pospruebaHabilitada() {
        return panelAdminService.habilitacion().habilitada();
    }

    private Encuesta encuestaFinal() {
        Encuesta encuesta = encuestaRepository.findByTipoIgnoreCase(TIPO_POSPRUEBA)
            .orElseThrow(() -> new NotFoundException("Encuesta no encontrada"));
        if (!encuesta.isActiva()) {
            throw new NotFoundException("Encuesta no encontrada");
        }
        return encuesta;
    }

    private void validarAcceso(Long viviendaId) {
        if (!respuestaRepository.existsByViviendaIdAndEncuestaTipo(viviendaId, TIPO_PREPRUEBA)) {
            throw new NotFoundException("Tu vivienda aún no ha respondido la encuesta inicial");
        }
        // Deshabilitada por defecto: solo el administrador la habilita.
        if (!pospruebaHabilitada()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "La encuesta final aún no está disponible. Te avisaremos cuando se habilite");
        }
    }

    /** Estado de la posprueba para la vivienda (pantalla y gating del frontend). */
    @Transactional(readOnly = true)
    public EncuestaFinalDisponibleDTO estado(Long viviendaId) {
        if (!respuestaRepository.existsByViviendaIdAndEncuestaTipo(viviendaId, TIPO_PREPRUEBA)) {
            throw new NotFoundException("Tu vivienda aún no ha respondido la encuesta inicial");
        }
        boolean yaRespondida = respuestaRepository
            .existsByViviendaIdAndEncuestaTipo(viviendaId, TIPO_POSPRUEBA);
        boolean disponible = pospruebaHabilitada() && !yaRespondida;
        return new EncuestaFinalDisponibleDTO(disponible, yaRespondida);
    }

    /** Los 23 ítems de la posprueba, SIN claves ni valores internos. */
    @Transactional(readOnly = true)
    public EncuestaResponse obtenerEncuesta(Long viviendaId) {
        validarAcceso(viviendaId);
        Encuesta encuesta = encuestaFinal();
        List<Pregunta> preguntas = preguntaRepository.findByEncuestaIdOrderByOrdenAsc(encuesta.getId());
        Map<Long, List<Opcion>> opcionesPorPregunta = new HashMap<>();
        for (Pregunta p : preguntas) {
            opcionesPorPregunta.put(p.getId(), opcionRepository.findByPreguntaIdOrderByOrdenAsc(p.getId()));
        }
        List<EncuestaResponse.PreguntaDTO> preguntasDTO = preguntas.stream()
            .map(p -> new EncuestaResponse.PreguntaDTO(
                p.getId(), p.getTexto(), p.getTipo(), p.getTipoPregunta(), p.getOrden(), p.isRequerida(),
                opcionesPorPregunta.getOrDefault(p.getId(), List.of()).stream()
                    .map(o -> new EncuestaResponse.PreguntaDTO.OpcionDTO(o.getId(), o.getTexto(), o.getOrden()))
                    .toList()))
            .toList();
        return new EncuestaResponse(encuesta.getId(), encuesta.getTipo(), encuesta.getTitulo(), preguntasDTO);
    }

    /**
     * Registra las 22 respuestas y devuelve el puntaje de conocimiento 0-10
     * (mismos ítems de conocimiento, claves reordenadas).
     */
    @Transactional
    public int responder(Long viviendaId, List<com.senderos.dto.EncuestaRespuestaRequest.RespuestaItem> respuestas) {
        validarAcceso(viviendaId);
        Encuesta encuesta = encuestaFinal();
        if (respuestaRepository.existsByViviendaIdAndEncuestaId(viviendaId, encuesta.getId())) {
            throw new ConflictException("Tu vivienda ya respondió la encuesta final");
        }
        String especificaOtra = "Seleccionaste \"Otra\": escribe tu respuesta en el campo de texto";
        Map<Long, Pregunta> preguntas = preguntaRepository
            .findByEncuestaIdOrderByOrdenAsc(encuesta.getId())
            .stream().collect(java.util.stream.Collectors.toMap(Pregunta::getId, java.util.function.Function.identity()));

        java.util.Set<Long> respondidas = new java.util.HashSet<>();
        for (com.senderos.dto.EncuestaRespuestaRequest.RespuestaItem item : respuestas) {
            Pregunta pregunta = preguntas.get(item.preguntaId());
            if (pregunta == null) {
                throw new BusinessException("Pregunta invalida: " + item.preguntaId());
            }
            if (!respondidas.add(item.preguntaId())) {
                throw new BusinessException("Pregunta duplicada: " + item.preguntaId());
            }
            boolean sinValor = item.opcionId() == null
                && (item.valorTexto() == null || item.valorTexto().isBlank());
            if (sinValor) {
                if (pregunta.isRequerida()) {
                    throw new BusinessException("La pregunta " + item.preguntaId() + " es requerida");
                }
                continue;
            }
            Respuesta respuesta = new Respuesta();
            respuesta.setVivienda(viviendaRepository.getReferenceById(viviendaId));
            respuesta.setPregunta(pregunta);
            if (item.opcionId() != null) {
                Opcion opcion = opcionRepository.findById(item.opcionId())
                    .orElseThrow(() -> new NotFoundException("Opcion no encontrada"));
                if (!opcion.getPregunta().getId().equals(pregunta.getId())) {
                    throw new BusinessException("La opcion no pertenece a la pregunta " + item.preguntaId());
                }
                respuesta.setOpcion(opcion);
                if ("OTRA".equals(opcion.getValor())) {
                    if (item.valorTexto() == null || item.valorTexto().isBlank()) {
                        throw new BusinessException(especificaOtra);
                    }
                    respuesta.setValorTexto(item.valorTexto().trim());
                } else {
                    respuesta.setValorTexto(opcion.getValor());
                }
            } else {
                respuesta.setValorTexto(item.valorTexto().trim());
            }
            respuestaRepository.save(respuesta);
        }
        for (Pregunta pregunta : preguntas.values()) {
            if (pregunta.isRequerida() && !respondidas.contains(pregunta.getId())) {
                throw new BusinessException("Falta responder la pregunta " + pregunta.getId());
            }
        }
        return (int) respuestaRepository.countCorrectasByViviendaAndEncuesta(viviendaId, encuesta.getId());
    }

    /** Trabajo programado: SOLO loguea el estado (aviso por WhatsApp). */
    @Scheduled(cron = "0 0 9 * * *", zone = "America/Caracas")
    public void revisionDiaria() {
        try {
            long elegibles = contarViviendasElegibles();
            log.info("Posprueba: {}habilitada por el administrador; viviendas elegibles (con preprueba sin posprueba): {}",
                    pospruebaHabilitada() ? "" : "NO ", elegibles);
        } catch (DataAccessException e) {
            log.error("Posprueba: fallo al revisar el estado: {}", e.getMessage());
        }
    }

    private long contarPrepruebas() {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(DISTINCT vivienda_id) FROM respuesta r "
                + "JOIN pregunta p ON p.id = r.pregunta_id "
                + "JOIN encuesta e ON e.id = p.encuesta_id WHERE e.tipo = 'INICIAL'", Long.class);
    }

    private long contarViviendasElegibles() {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(DISTINCT r.vivienda_id) FROM respuesta r "
                + "JOIN pregunta p ON p.id = r.pregunta_id "
                + "JOIN encuesta e ON e.id = p.encuesta_id WHERE e.tipo = 'INICIAL' "
                + "AND NOT EXISTS (SELECT 1 FROM respuesta rf JOIN pregunta pf ON pf.id = rf.pregunta_id "
                + "JOIN encuesta ef ON ef.id = pf.encuesta_id "
                + "WHERE rf.vivienda_id = r.vivienda_id AND ef.tipo = 'FINAL')", Long.class);
    }

    /** Panel admin: una fila por vivienda con cuenta — preprueba, estado y puntajes. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> estadoGeneral() {
        boolean habilitada = pospruebaHabilitada();
        List<Map<String, Object>> filas = jdbcTemplate.queryForList(
            "SELECT v.codigo AS codigo, "
                + "(SELECT MIN(r.respondido_en) FROM respuesta r JOIN pregunta p ON p.id = r.pregunta_id "
                + "   JOIN encuesta e ON e.id = p.encuesta_id "
                + "   WHERE r.vivienda_id = v.id AND e.tipo = 'INICIAL') AS fecha_preprueba, "
                + "EXISTS (SELECT 1 FROM respuesta rf JOIN pregunta pf ON pf.id = rf.pregunta_id "
                + "   JOIN encuesta ef ON ef.id = pf.encuesta_id "
                + "   WHERE rf.vivienda_id = v.id AND ef.tipo = 'FINAL') AS respondio_final, "
                + "(SELECT COUNT(*) FROM respuesta r JOIN opcion o ON o.id = r.opcion_id "
                + "   JOIN pregunta p ON p.id = r.pregunta_id JOIN encuesta e ON e.id = p.encuesta_id "
                + "   WHERE r.vivienda_id = v.id AND e.tipo = 'INICIAL' AND o.es_correcta = 1) AS puntaje_pre, "
                + "(SELECT COUNT(*) FROM respuesta r JOIN opcion o ON o.id = r.opcion_id "
                + "   JOIN pregunta p ON p.id = r.pregunta_id JOIN encuesta e ON e.id = p.encuesta_id "
                + "   WHERE r.vivienda_id = v.id AND e.tipo = 'FINAL' AND o.es_correcta = 1) AS puntaje_post "
                + "FROM vivienda v "
                + "WHERE EXISTS (SELECT 1 FROM usuario u WHERE u.vivienda_id = v.id) ORDER BY v.codigo");
        return filas.stream().map(fila -> {
            // H2 devuelve EXISTS como Boolean y MySQL como número: aceptar ambos.
            Object respondio = fila.get("respondio_final");
            boolean respondioFinal = respondio instanceof Boolean b
                ? b
                : ((Number) respondio).intValue() == 1;
            boolean conPreprueba = fila.get("fecha_preprueba") != null;
            Map<String, Object> fila2 = new HashMap<>(fila);
            fila2.put("disponible", habilitada && conPreprueba && !respondioFinal);
            fila2.put("respondio_final", respondioFinal);
            Object fecha = fila.get("fecha_preprueba");
            if (fecha instanceof java.sql.Timestamp ts) {
                fila2.put("fecha_preprueba", ts.toInstant());
            }
            return fila2;
        }).toList();
    }
}
