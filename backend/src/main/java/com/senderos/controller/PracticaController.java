package com.senderos.controller;

import com.senderos.dto.PracticaItemDTO;
import com.senderos.dto.ResultadoPracticaDTO;
import com.senderos.service.PracticaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Práctica libre del módulo Clasificación. Requiere sesión; la vivienda
 * se deriva del usuario autenticado y los intentos se registran por
 * vivienda para investigación posterior.
 */
@RestController
@RequestMapping("/api/practica")
public class PracticaController {

    public record RespuestaPracticaRequest(@NotBlank @Size(max = 1000) String respuesta) {}

    private final PracticaService practicaService;

    public PracticaController(PracticaService practicaService) {
        this.practicaService = practicaService;
    }

    @GetMapping("/item")
    public ResponseEntity<PracticaItemDTO> item(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam String modo,
            @RequestParam(required = false, name = "excluir") List<Long> excluir) {
        return ResponseEntity.ok(practicaService.itemAleatorio(principal.getUsername(), modo, excluir));
    }

    @PostMapping("/item/{id}/responder")
    public ResponseEntity<ResultadoPracticaDTO> responder(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody RespuestaPracticaRequest request) {
        return ResponseEntity.ok(practicaService.responder(principal.getUsername(), id, request.respuesta()));
    }

    @GetMapping("/resumen")
    public ResponseEntity<ResultadoPracticaDTO.PuntuacionDTO> resumen(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(practicaService.resumen(principal.getUsername()));
    }
}
