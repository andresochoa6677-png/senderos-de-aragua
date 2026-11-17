package com.senderos.controller;

import com.senderos.dto.EncuestaFinalDisponibleDTO;
import com.senderos.dto.EncuestaRespuestaRequest;
import com.senderos.service.EncuestaFinalService;
import com.senderos.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Encuesta FINAL (posprueba). La ventana (día 22-35 desde la primera
 * preprueba del estudio) la valida el servicio: 404 sin preprueba,
 * 403 fuera de ventana, 409 ya respondida, 422 ítems faltantes.
 */
@RestController
@RequestMapping("/api/encuesta-final")
public class EncuestaFinalController {

    private final EncuestaFinalService encuestaFinalService;
    private final UsuarioService usuarioService;

    public EncuestaFinalController(EncuestaFinalService encuestaFinalService,
                                   UsuarioService usuarioService) {
        this.encuestaFinalService = encuestaFinalService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/disponible")
    public ResponseEntity<EncuestaFinalDisponibleDTO> disponible(
            @AuthenticationPrincipal UserDetails principal) {
        Long viviendaId = usuarioService.buscarViviendaIdPorEmail(principal.getUsername());
        return ResponseEntity.ok(encuestaFinalService.estado(viviendaId));
    }

    @GetMapping
    public ResponseEntity<com.senderos.dto.EncuestaResponse> encuesta(
            @AuthenticationPrincipal UserDetails principal) {
        Long viviendaId = usuarioService.buscarViviendaIdPorEmail(principal.getUsername());
        return ResponseEntity.ok(encuestaFinalService.obtenerEncuesta(viviendaId));
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> responder(
            @Valid @RequestBody com.senderos.dto.RespuestaFinalRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Long viviendaId = usuarioService.buscarViviendaIdPorEmail(principal.getUsername());
        int puntaje = encuestaFinalService.responder(viviendaId, request.respuestas());
        return ResponseEntity.ok(Map.of(
            "mensaje", "Respuestas registradas",
            "puntajeConocimiento", String.valueOf(puntaje)));
    }
}
