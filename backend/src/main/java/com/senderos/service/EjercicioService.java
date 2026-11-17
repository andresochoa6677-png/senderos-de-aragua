package com.senderos.service;

import com.senderos.dto.EjercicioDTO;
import com.senderos.dto.RespuestaEjercicioDTO;
import com.senderos.dto.ResultadoIntentoDTO;
import com.senderos.entity.Ejercicio;
import com.senderos.entity.IntentoEjercicio;
import com.senderos.entity.Leccion;
import com.senderos.entity.ProgresoLeccion;
import com.senderos.entity.Vivienda;
import com.senderos.exception.BusinessException;
import com.senderos.exception.NotFoundException;
import com.senderos.repository.EjercicioRepository;
import com.senderos.repository.IntentoEjercicioRepository;
import com.senderos.repository.LeccionRepository;
import com.senderos.repository.ProgresoLeccionRepository;
import com.senderos.repository.ViviendaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ejercicios del modulo educativo. La calificacion es server-side y la
 * leccion se completa automaticamente cuando la vivienda resuelve
 * correctamente todos sus ejercicios; el recorrido obligatorio se aplica
 * aqui tambien: no se puede responder ejercicios de una leccion bloqueada.
 */
@Service
public class EjercicioService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final EjercicioRepository ejercicioRepository;
    private final IntentoEjercicioRepository intentoRepository;
    private final ViviendaRepository viviendaRepository;
    private final UsuarioService usuarioService;
    private final LeccionService leccionService;
    private final ProgresoLeccionRepository progresoRepository;
    private final LeccionRepository leccionRepository;

    public EjercicioService(EjercicioRepository ejercicioRepository,
                            IntentoEjercicioRepository intentoRepository,
                            ViviendaRepository viviendaRepository,
                            UsuarioService usuarioService,
                            LeccionService leccionService,
                            ProgresoLeccionRepository progresoRepository,
                            LeccionRepository leccionRepository) {
        this.ejercicioRepository = ejercicioRepository;
        this.intentoRepository = intentoRepository;
        this.viviendaRepository = viviendaRepository;
        this.usuarioService = usuarioService;
        this.leccionService = leccionService;
        this.progresoRepository = progresoRepository;
        this.leccionRepository = leccionRepository;
    }

    /** Ejercicios activos de una leccion con el estado de la vivienda autenticada. */
    @Transactional(readOnly = true)
    public List<EjercicioDTO> listarPorLeccion(String email, Long leccionId) {
        Long viviendaId = usuarioService.buscarViviendaIdPorEmail(email);
        List<Ejercicio> ejercicios = ejercicioRepository
                .findByLeccionIdAndActivoTrueOrderByOrdenAsc(leccionId);
        if (ejercicios.isEmpty()) {
            throw new NotFoundException("La leccion no tiene ejercicios");
        }
        asegurarLeccionDesbloqueada(viviendaId, ejercicios.get(0).getLeccion());
        return ejercicios.stream()
                .map(e -> EjercicioDTO.de(e, intentoRepository
                        .findByViviendaAndEjercicio(viviendaId, e.getId())))
                .toList();
    }

    /**
     * Registra un intento y lo califica. La respuesta correcta nunca
     * se envia al cliente: solo correcto/incorrecto y explicacion.
     * Si con este intento la vivienda resuelve todos los ejercicios de la
     * leccion, la marca como completada (recorrido obligatorio) y devuelve
     * la siguiente leccion desbloqueada.
     */
    @Transactional
    public ResultadoIntentoDTO responder(String email, Long ejercicioId, RespuestaEjercicioDTO dto) {
        Long viviendaId = usuarioService.buscarViviendaIdPorEmail(email);
        Ejercicio ejercicio = ejercicioRepository.findById(ejercicioId)
                .orElseThrow(() -> new NotFoundException("Ejercicio no encontrado"));
        Leccion leccion = ejercicio.getLeccion();
        asegurarLeccionDesbloqueada(viviendaId, leccion);

        boolean yaResuelto = intentoRepository
                .findByViviendaAndEjercicio(viviendaId, ejercicioId)
                .stream().anyMatch(i -> Boolean.TRUE.equals(i.getCorrecto()));

        boolean correcto;
        List<ResultadoIntentoDTO.DetalleItemDTO> detalleItems = null;
        if ("CLASIFICAR".equals(ejercicio.getTipo())) {
            Map<String, String> esperado = parseMapa(ejercicio.getRespuesta());
            Map<String, String> enviada = parseMapa(dto.respuesta());
            // Correcto solo si TODOS los elementos estan en su destino y no
            // sobra ninguno. El detalle marca el estado de cada elemento sin
            // revelar su destino correcto.
            correcto = !esperado.isEmpty() && esperado.equals(enviada);
            detalleItems = esperado.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> new ResultadoIntentoDTO.DetalleItemDTO(
                            e.getKey(), e.getValue().equals(enviada.get(e.getKey()))))
                    .toList();
        } else {
            correcto = normalizar(ejercicio.getRespuesta())
                    .equals(normalizar(dto.respuesta()));
        }

        IntentoEjercicio intento = new IntentoEjercicio(
                viviendaRepository.getReferenceById(viviendaId), ejercicio,
                truncar(dto.respuesta()), correcto);
        intentoRepository.save(intento);

        boolean leccionCompletada = false;
        Long siguienteLeccionId = null;
        if (correcto) {
            leccionCompletada = marcarLeccionSiCompleta(viviendaId, leccion);
            if (leccionCompletada) {
                siguienteLeccionId = leccionRepository
                        .findFirstByActivaTrueAndOrdenGreaterThanOrderByOrdenAsc(leccion.getOrden())
                        .map(Leccion::getId)
                        .orElse(null);
            }
        }

        boolean primeraCorrecta = correcto && !yaResuelto;
        return new ResultadoIntentoDTO(
                ejercicioId, correcto, primeraCorrecta,
                primeraCorrecta ? ejercicio.getPuntos() : 0,
                ejercicio.getPuntos(), ejercicio.getExplicacion(), intento.getCreadoEn(),
                leccionCompletada, siguienteLeccionId, detalleItems);
    }

    /** Mapa elemento -> destino de una actividad CLASIFICAR. Vacio si el JSON es invalido. */
    private Map<String, String> parseMapa(String json) {
        try {
            return MAPPER.readValue(com.senderos.dto.EjercicioDTO.limpiarJson(json),
                    MAPPER.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * Marca la leccion como completada si la vivienda ya resolvio
     * correctamente todos sus ejercicios activos. Idempotente: conserva
     * la primera fecha de completado.
     */
    private boolean marcarLeccionSiCompleta(Long viviendaId, Leccion leccion) {
        List<Ejercicio> activos = ejercicioRepository
                .findByLeccionIdAndActivoTrueOrderByOrdenAsc(leccion.getId());
        if (activos.isEmpty()) return false;
        List<Long> correctos = intentoRepository.findEjerciciosCorrectosDeLeccion(viviendaId, leccion.getId());
        boolean todosCorrectos = activos.stream().allMatch(e -> correctos.contains(e.getId()));
        if (!todosCorrectos) return false;

        ProgresoLeccion progreso = progresoRepository.findByViviendaAndLeccion(viviendaId, leccion.getId())
                .orElseGet(() -> progresoRepository.save(
                        new ProgresoLeccion(viviendaRepository.getReferenceById(viviendaId), leccion)));
        progreso.completar();
        return true;
    }

    private void asegurarLeccionDesbloqueada(Long viviendaId, Leccion leccion) {
        Integer ordenActual = leccionService.ordenLeccionActual(viviendaId);
        if (ordenActual != null && leccion.getOrden() > ordenActual) {
            throw new BusinessException(LeccionService.MENSAJE_BLOQUEADA);
        }
    }

    /** Comparacion tolerante a mayusculas, acentos y espacios. */
    private String normalizar(String s) {
        return s == null ? "" : java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .trim().toLowerCase();
    }

    private String truncar(String s) {
        return s != null && s.length() > 1000 ? s.substring(0, 1000) : s;
    }
}
