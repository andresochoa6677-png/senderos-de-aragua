package com.senderos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.senderos.dto.PracticaItemDTO;
import com.senderos.dto.ResultadoPracticaDTO;
import com.senderos.entity.IntentoPractica;
import com.senderos.entity.PracticaItem;
import com.senderos.entity.Vivienda;
import com.senderos.exception.BusinessException;
import com.senderos.exception.NotFoundException;
import com.senderos.repository.IntentoPracticaRepository;
import com.senderos.repository.PracticaItemRepository;
import com.senderos.repository.ViviendaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Práctica libre del módulo Clasificación. La vivienda se resuelve
 * siempre desde la sesión autenticada; la solución de cada actividad
 * vive solo en el backend y cada intento queda registrado por vivienda
 * para análisis posterior de errores.
 */
@Service
public class PracticaService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int LONGITUD_MAXIMA_RESPUESTA = 1000;
    /** Tope de la lista "excluir" del cliente: evita cláusulas IN gigantes (A-8). */
    private static final int MAX_EXCLUIDAS = 200;

    private final PracticaItemRepository itemRepository;
    private final IntentoPracticaRepository intentoRepository;
    private final ViviendaRepository viviendaRepository;
    private final UsuarioService usuarioService;

    public PracticaService(PracticaItemRepository itemRepository,
                           IntentoPracticaRepository intentoRepository,
                           ViviendaRepository viviendaRepository,
                           UsuarioService usuarioService) {
        this.itemRepository = itemRepository;
        this.intentoRepository = intentoRepository;
        this.viviendaRepository = viviendaRepository;
        this.usuarioService = usuarioService;
    }

    /** Actividad aleatoria del modo pedido, evitando las excluidas (ronda sin repetir). */
    @Transactional(readOnly = true)
    public PracticaItemDTO itemAleatorio(String email, String modo, List<Long> excluir) {
        usuarioService.buscarViviendaIdPorEmail(email); // acceso solo para viviendas
        if (!List.of("CLASIFICA", "PREPARA", "SITUACION").contains(modo)) {
            throw new BusinessException("Modo de práctica inválido");
        }
        if (excluir != null && excluir.size() > MAX_EXCLUIDAS) {
            throw new BusinessException("Lista de actividades excluidas demasiado grande");
        }
        List<PracticaItem> items = itemRepository.findByModoAndActivoTrueOrderByIdAsc(modo);
        if (items.isEmpty()) {
            throw new NotFoundException("No hay actividades disponibles para este modo");
        }
        List<PracticaItem> candidatas = items.stream()
                .filter(i -> excluir == null || !excluir.contains(i.getId()))
                .toList();
        if (candidatas.isEmpty()) {
            candidatas = items; // ronda agotada: se permite repetir
        }
        PracticaItem item = candidatas.get(ThreadLocalRandom.current().nextInt(candidatas.size()));
        return new PracticaItemDTO(
                item.getId(), item.getModo(), item.getTipo(), item.getEscenario(),
                item.getEnunciado(), item.getImagen(),
                com.senderos.dto.EjercicioDTO.parseConfig(item.getContenido()));
    }

    /** Califica, registra el intento de la vivienda y devuelve feedback con puntuación. */
    @Transactional
    public ResultadoPracticaDTO responder(String email, Long itemId, String respuesta) {
        Long viviendaId = usuarioService.buscarViviendaIdPorEmail(email);
        PracticaItem item = itemRepository.findById(itemId)
                .filter(PracticaItem::isActivo)
                .orElseThrow(() -> new NotFoundException("Actividad no encontrada"));
        String respuestaLimpia = respuesta == null ? "" : truncar(respuesta.trim());

        boolean correcto;
        List<ResultadoPracticaDTO.DetalleItemDTO> detalleItems = null;
        switch (item.getTipo()) {
            case "GRUPO" -> {
                Map<String, String> esperado = parseMapa(item.getRespuesta());
                Map<String, String> enviada = parseMapa(respuestaLimpia);
                correcto = !esperado.isEmpty() && esperado.equals(enviada);
                detalleItems = esperado.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(e -> new ResultadoPracticaDTO.DetalleItemDTO(
                                e.getKey(), e.getValue().equals(enviada.get(e.getKey()))))
                        .toList();
            }
            case "DESTINO", "ELECCION", "ORDENAR" ->
                correcto = normalizar(item.getRespuesta()).equals(normalizar(respuestaLimpia));
            default -> throw new BusinessException("Tipo de actividad no soportado");
        }

        Vivienda vivienda = viviendaRepository.getReferenceById(viviendaId);
        intentoRepository.save(new IntentoPractica(vivienda, item, item.getModo(), respuestaLimpia, correcto));

        return new ResultadoPracticaDTO(correcto, item.getExplicacion(), detalleItems, resumenDe(viviendaId));
    }

    /** Puntuación acumulada de la vivienda: total, correctas y rachas. */
    @Transactional(readOnly = true)
    public ResultadoPracticaDTO.PuntuacionDTO resumen(String email) {
        Long viviendaId = usuarioService.buscarViviendaIdPorEmail(email);
        return resumenDe(viviendaId);
    }

    private ResultadoPracticaDTO.PuntuacionDTO resumenDe(Long viviendaId) {
        long total = intentoRepository.countByViviendaId(viviendaId);
        long correctas = intentoRepository.countByViviendaIdAndCorrectoTrue(viviendaId);

        // Racha actual = aciertos consecutivos desde el intento mas reciente
        // (se cierra con el primer fallo). Racha maxima = mejor serie del historial.
        int rachaActual = 0;
        int rachaMaxima = 0;
        int serieEnCurso = 0;
        boolean rachaCerrada = false;
        for (IntentoPractica intento : intentoRepository.findTop50ByViviendaIdOrderByCreadoEnDescIdDesc(viviendaId)) {
            if (intento.isCorrecto()) {
                serieEnCurso++;
                rachaMaxima = Math.max(rachaMaxima, serieEnCurso);
                if (!rachaCerrada) rachaActual = serieEnCurso;
            } else {
                rachaMaxima = Math.max(rachaMaxima, serieEnCurso);
                serieEnCurso = 0;
                rachaCerrada = true;
                rachaActual = 0;
            }
        }
        return new ResultadoPracticaDTO.PuntuacionDTO(total, correctas, rachaActual, rachaMaxima);
    }

    private Map<String, String> parseMapa(String json) {
        try {
            return MAPPER.readValue(com.senderos.dto.EjercicioDTO.limpiarJson(json),
                    MAPPER.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String normalizar(String s) {
        return s == null ? "" : java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .trim().toLowerCase();
    }

    private String truncar(String s) {
        return s != null && s.length() > LONGITUD_MAXIMA_RESPUESTA ? s.substring(0, LONGITUD_MAXIMA_RESPUESTA) : s;
    }
}
