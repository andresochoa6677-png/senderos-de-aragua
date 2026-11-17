package com.senderos.service;

import com.senderos.dto.EncuestaRespuestaRequest;
import com.senderos.dto.EncuestaResponse;
import com.senderos.dto.OnboardingEstadoResponse;
import com.senderos.entity.Encuesta;
import com.senderos.entity.Opcion;
import com.senderos.entity.Pregunta;
import com.senderos.entity.Respuesta;
import com.senderos.entity.Vivienda;
import com.senderos.exception.BusinessException;
import com.senderos.exception.ConflictException;
import com.senderos.exception.NotFoundException;
import com.senderos.repository.EncuestaRepository;
import com.senderos.repository.OpcionRepository;
import com.senderos.repository.PreguntaRepository;
import com.senderos.repository.RespuestaRepository;
import com.senderos.repository.ViviendaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OnboardingService {

    /** Valor de la opcion abierta de la barrera (item 18): exige texto libre. */
    private static final String VALOR_ABIERTA = "OTRA";

    private final ConsentimientoService consentimientoService;
    private final EncuestaRepository encuestaRepository;
    private final PreguntaRepository preguntaRepository;
    private final OpcionRepository opcionRepository;
    private final RespuestaRepository respuestaRepository;
    private final ViviendaRepository viviendaRepository;

    public OnboardingService(ConsentimientoService consentimientoService,
                             EncuestaRepository encuestaRepository,
                             PreguntaRepository preguntaRepository,
                             OpcionRepository opcionRepository,
                             RespuestaRepository respuestaRepository,
                             ViviendaRepository viviendaRepository) {
        this.consentimientoService = consentimientoService;
        this.encuestaRepository = encuestaRepository;
        this.preguntaRepository = preguntaRepository;
        this.opcionRepository = opcionRepository;
        this.respuestaRepository = respuestaRepository;
        this.viviendaRepository = viviendaRepository;
    }

    @Transactional(readOnly = true)
    public EncuestaResponse obtenerEncuesta(String tipo) {
        Encuesta encuesta = encuestaRepository.findByTipoIgnoreCase(tipo)
            .orElseThrow(() -> new NotFoundException("Encuesta no encontrada"));
        return toDTO(encuesta);
    }

    /**
     * Registra las respuestas de la encuesta de la vivienda (una fila por
     * ítem) y devuelve el puntaje de conocimiento 0-10 (1 punto por ítem
     * CONOCIMIENTO con la opcion clave; practicas y percepción no puntúan).
     * La clave (es_correcta) nunca viaja al cliente: la corrección es
     * íntegramente server-side.
     */
    @Transactional
    public int responderEncuesta(Long viviendaId, EncuestaRespuestaRequest request) {
        Encuesta encuesta = encuestaRepository.findById(request.encuestaId())
            .orElseThrow(() -> new NotFoundException("Encuesta no encontrada"));
        if (!encuesta.isActiva()) {
            throw new BusinessException("La encuesta no esta activa");
        }
        if (respuestaRepository.existsByViviendaIdAndEncuestaId(viviendaId, encuesta.getId())) {
            throw new ConflictException("La vivienda ya respondio esta encuesta");
        }
        if ("INICIAL".equals(encuesta.getTipo())
                && !consentimientoService.tieneConsentimientoVigente(viviendaId)) {
            throw new BusinessException("Debes aceptar el consentimiento antes de responder");
        }

        Map<Long, Pregunta> preguntas = preguntaRepository.findByEncuestaIdOrderByOrdenAsc(encuesta.getId())
            .stream().collect(Collectors.toMap(Pregunta::getId, Function.identity()));

        Set<Long> preguntasRespondidas = new HashSet<>();
        for (EncuestaRespuestaRequest.RespuestaItem item : request.respuestas()) {
            Pregunta pregunta = preguntas.get(item.preguntaId());
            if (pregunta == null) {
                throw new BusinessException("Pregunta invalida: " + item.preguntaId());
            }
            if (!preguntasRespondidas.add(item.preguntaId())) {
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
                if (VALOR_ABIERTA.equals(opcion.getValor())) {
                    // "Otra": la especificación libre es obligatoria y se
                    // conserva junto a la opcion elegida.
                    if (item.valorTexto() == null || item.valorTexto().isBlank()) {
                        throw new BusinessException(
                            "Seleccionaste \"Otra\": escribe tu respuesta en el campo de texto");
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
            if (pregunta.isRequerida() && !preguntasRespondidas.contains(pregunta.getId())) {
                throw new BusinessException("Falta responder la pregunta " + pregunta.getId());
            }
        }
        return (int) respuestaRepository.countCorrectasByViviendaAndEncuesta(viviendaId, encuesta.getId());
    }

    @Transactional(readOnly = true)
    public OnboardingEstadoResponse estadoOnboarding(Long viviendaId) {
        // Solo la aceptación de la versión VIGENTE habilita el avance: una
        // fila legacy REG-1.0 vuelve a pedir el consentimiento actualizado.
        boolean consentimiento = consentimientoService.tieneConsentimientoVigente(viviendaId);
        boolean encuestaInicial = respuestaRepository.existsByViviendaIdAndEncuestaTipo(viviendaId, "INICIAL");
        String siguientePaso;
        if (!consentimiento) {
            siguientePaso = "CONSENTIMIENTO";
        } else if (!encuestaInicial) {
            siguientePaso = "ENCUESTA_INICIAL";
        } else {
            siguientePaso = "DASHBOARD";
        }
        return new OnboardingEstadoResponse(consentimiento, encuestaInicial, siguientePaso);
    }

    private EncuestaResponse toDTO(Encuesta encuesta) {
        List<Pregunta> preguntas = preguntaRepository.findByEncuestaIdOrderByOrdenAsc(encuesta.getId());
        Map<Long, List<Opcion>> opcionesPorPregunta = new java.util.HashMap<>();
        for (Pregunta p : preguntas) {
            opcionesPorPregunta.put(p.getId(),
                opcionRepository.findByPreguntaIdOrderByOrdenAsc(p.getId()));
        }
        List<EncuestaResponse.PreguntaDTO> preguntasDTO = preguntas.stream()
            .map(p -> new EncuestaResponse.PreguntaDTO(
                p.getId(), p.getTexto(), p.getTipo(), p.getTipoPregunta(), p.getOrden(), p.isRequerida(),
                opcionesPorPregunta.getOrDefault(p.getId(), List.of()).stream()
                    .map(o -> new EncuestaResponse.PreguntaDTO.OpcionDTO(
                        o.getId(), o.getTexto(), o.getOrden()))
                    .toList()))
            .toList();
        return new EncuestaResponse(encuesta.getId(), encuesta.getTipo(), encuesta.getTitulo(), preguntasDTO);
    }
}
