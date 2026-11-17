package com.senderos.controller;

import com.senderos.dto.ComentarioRequest;
import com.senderos.dto.ComentarioResponse;
import com.senderos.service.ComentarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Participación del residente: canal privado para inquietudes,
 * sugerencias, percepciones y situaciones. La identidad y la vivienda
 * provienen SIEMPRE de la sesión autenticada.
 */
@RestController
@RequestMapping("/api/comentarios")
public class ComentarioController {

    private final ComentarioService comentarioService;

    public ComentarioController(ComentarioService comentarioService) {
        this.comentarioService = comentarioService;
    }

    @PostMapping
    public ResponseEntity<ComentarioResponse> crear(
            @Valid @RequestBody ComentarioRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        ComentarioResponse respuesta = comentarioService.crear(principal.getUsername(), request);
        return ResponseEntity.status(201).body(respuesta);
    }

    /** Aportes de la propia vivienda: jamás de otras viviendas. */
    @GetMapping("/mios")
    public ResponseEntity<List<ComentarioResponse>> mios(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(comentarioService.misAportes(principal.getUsername()));
    }
}
