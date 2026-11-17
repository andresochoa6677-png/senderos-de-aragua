package com.senderos.service;

import com.senderos.dto.LeccionDetalleDTO;
import com.senderos.dto.LeccionResumenDTO;
import com.senderos.dto.ProgresoViviendaDTO;
import com.senderos.entity.Ejercicio;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Modulo educativo con recorrido obligatorio: las lecciones se completan
 * resolviendo sus ejercicios y cada leccion desbloquea la siguiente.
 * Todo el estado se resuelve server-side a partir de la vivienda
 * autenticada (la unidad de datos del sistema); el cliente nunca envia
 * ids de vivienda ni puede forzar completados.
 */
@Service
public class LeccionService {

    public static final String ESTADO_COMPLETADA = "COMPLETADA";
    public static final String ESTADO_ACTUAL = "ACTUAL";
    public static final String ESTADO_BLOQUEADA = "BLOQUEADA";

    public static final String MENSAJE_BLOQUEADA =
            "Esta lección está bloqueada: completa los ejercicios de la lección anterior para desbloquearla.";

    private final LeccionRepository leccionRepository;
    private final ProgresoLeccionRepository progresoRepository;
    private final ViviendaRepository viviendaRepository;
    private final UsuarioService usuarioService;
    private final EjercicioRepository ejercicioRepository;
    private final IntentoEjercicioRepository intentoRepository;

    public LeccionService(LeccionRepository leccionRepository,
                          ProgresoLeccionRepository progresoRepository,
                          ViviendaRepository viviendaRepository,
                          UsuarioService usuarioService,
                          EjercicioRepository ejercicioRepository,
                          IntentoEjercicioRepository intentoRepository) {
        this.leccionRepository = leccionRepository;
        this.progresoRepository = progresoRepository;
        this.viviendaRepository = viviendaRepository;
        this.usuarioService = usuarioService;
        this.ejercicioRepository = ejercicioRepository;
        this.intentoRepository = intentoRepository;
    }

    /** Lista de lecciones activas con el estado del recorrido de la vivienda autenticada. */
    @Transactional(readOnly = true)
    public List<LeccionResumenDTO> listar(String email) {
        Long viviendaId = usuarioService.buscarViviendaIdPorEmail(email);
        List<Leccion> activas = leccionRepository.findByActivaTrueOrderByOrdenAsc();
        Integer ordenActual = ordenPrimeraPendiente(viviendaId, activas);
        Map<Long, ProgresoLeccion> progresoPorLeccion = progresoPorVivienda(viviendaId);

        return activas.stream()
                .map(l -> {
                    String estado = estadoDe(l, ordenActual, progresoPorLeccion);
                    List<Ejercicio> activos = ejercicioRepository
                            .findByLeccionIdAndActivoTrueOrderByOrdenAsc(l.getId());
                    int correctos = (int) intentoRepository
                            .findEjerciciosCorrectosDeLeccion(viviendaId, l.getId()).stream().distinct().count();
                    return LeccionResumenDTO.de(l, progresoPorLeccion.get(l.getId()),
                            estado, activos.size(), correctos);
                })
                .toList();
    }

    /** Detalle de una leccion. Solo accesible si el recorrido la tiene desbloqueada. */
    @Transactional
    public LeccionDetalleDTO obtenerDetalle(String email, Long leccionId) {
        Long viviendaId = usuarioService.buscarViviendaIdPorEmail(email);
        Leccion leccion = leccionRepository.findByIdAndActivaTrue(leccionId)
                .orElseThrow(() -> new NotFoundException("Lección no encontrada"));
        asegurarDesbloqueada(viviendaId, leccion);

        ProgresoLeccion progreso = progresoRepository.findByViviendaAndLeccion(viviendaId, leccionId)
                .orElseGet(() -> {
                    Vivienda vivienda = viviendaRepository.getReferenceById(viviendaId);
                    return progresoRepository.save(new ProgresoLeccion(vivienda, leccion));
                });

        return LeccionDetalleDTO.de(leccion, progreso);
    }

    /**
     * Orden de la primera leccion activa pendiente (la "actual" del
     * recorrido). Null si todas las lecciones estan completadas.
     * Es la fuente de verdad del desbloqueo: una leccion con orden mayor
     * esta bloqueada; una menor o igual, accesible.
     */
    @Transactional(readOnly = true)
    public Integer ordenLeccionActual(Long viviendaId) {
        return ordenPrimeraPendiente(viviendaId, leccionRepository.findByActivaTrueOrderByOrdenAsc());
    }

    /** Progreso consolidado de la vivienda autenticada. */
    @Transactional(readOnly = true)
    public ProgresoViviendaDTO progreso(String email) {
        Long viviendaId = usuarioService.buscarViviendaIdPorEmail(email);
        List<Leccion> activas = leccionRepository.findByActivaTrueOrderByOrdenAsc();
        Integer ordenActual = ordenPrimeraPendiente(viviendaId, activas);
        Map<Long, ProgresoLeccion> progresoPorLeccion = progresoPorVivienda(viviendaId);

        List<ProgresoViviendaDTO.ItemProgresoDTO> items = new ArrayList<>();
        int completadas = 0;
        Instant ultimaActividad = null;
        Long leccionActualId = null;

        for (Leccion leccion : activas) {
            ProgresoLeccion p = progresoPorLeccion.get(leccion.getId());
            Instant completadoEn = p != null ? p.getCompletadoEn() : null;
            Instant iniciadoEn = p != null ? p.getIniciadoEn() : null;
            if (completadoEn != null) completadas++;
            ultimaActividad = masReciente(ultimaActividad, completadoEn, iniciadoEn);
            String estado = estadoDe(leccion, ordenActual, progresoPorLeccion);
            if (ESTADO_ACTUAL.equals(estado)) leccionActualId = leccion.getId();
            items.add(new ProgresoViviendaDTO.ItemProgresoDTO(
                    leccion.getId(), leccion.getOrden(), leccion.getTitulo(),
                    leccion.getIcono(), leccion.getColor(), estado, iniciadoEn, completadoEn));
        }

        int total = activas.size();
        int porcentaje = total == 0 ? 0 : (int) Math.round(completadas * 100.0 / total);
        return new ProgresoViviendaDTO(total, completadas, porcentaje, ultimaActividad, leccionActualId, items);
    }

    /** COMPLETADA si ya marco completado_en; ACTUAL si es la primera pendiente; BLOQUEADA en otro caso. */
    private String estadoDe(Leccion leccion, Integer ordenActual, Map<Long, ProgresoLeccion> progresoPorLeccion) {
        ProgresoLeccion p = progresoPorLeccion.get(leccion.getId());
        if (p != null && p.getCompletadoEn() != null) return ESTADO_COMPLETADA;
        if (ordenActual != null && leccion.getOrden() == ordenActual) return ESTADO_ACTUAL;
        return ESTADO_BLOQUEADA;
    }

    private Integer ordenPrimeraPendiente(Long viviendaId, List<Leccion> activas) {
        Map<Long, ProgresoLeccion> progreso = progresoPorVivienda(viviendaId);
        return activas.stream()
                .filter(l -> {
                    ProgresoLeccion p = progreso.get(l.getId());
                    return p == null || p.getCompletadoEn() == null;
                })
                .map(Leccion::getOrden)
                .findFirst()
                .orElse(null);
    }

    /** Rechaza el acceso a lecciones que el recorrido aun tiene bloqueadas. */
    private void asegurarDesbloqueada(Long viviendaId, Leccion leccion) {
        Integer ordenActual = ordenPrimeraPendiente(viviendaId, leccionRepository.findByActivaTrueOrderByOrdenAsc());
        if (ordenActual != null && leccion.getOrden() > ordenActual) {
            throw new BusinessException(MENSAJE_BLOQUEADA);
        }
    }

    private Map<Long, ProgresoLeccion> progresoPorVivienda(Long viviendaId) {
        return progresoRepository.findByViviendaOrderByOrden(viviendaId).stream()
                .collect(Collectors.toMap(p -> p.getLeccion().getId(), Function.identity()));
    }

    private static Instant masReciente(Instant actual, Instant... candidatas) {
        Instant resultado = actual;
        for (Instant c : candidatas) {
            if (c != null && (resultado == null || c.isAfter(resultado))) resultado = c;
        }
        return resultado;
    }
}
