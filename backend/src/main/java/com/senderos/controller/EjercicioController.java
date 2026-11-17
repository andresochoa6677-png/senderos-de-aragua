package com.senderos.controller;

import com.senderos.dto.EjercicioDTO;
import com.senderos.dto.RespuestaEjercicioDTO;
import com.senderos.dto.ResultadoIntentoDTO;
import com.senderos.service.EjercicioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Modulo de ejercicios. La vivienda se resuelve siempre desde la
 * sesion autenticada; el cliente nunca envia ids de vivienda.
 */
@RestController
@RequestMapping("/api/ejercicios")
public class EjercicioController {

    private final EjercicioService ejercicioService;

    public EjercicioController(EjercicioService ejercicioService) {
        this.ejercicioService = ejercicioService;
    }

    @GetMapping("/leccion/{leccionId}")
    public ResponseEntity<List<EjercicioDTO>> porLeccion(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long leccionId) {
        return ResponseEntity.ok(ejercicioService.listarPorLeccion(principal.getUsername(), leccionId));
    }

    @PostMapping("/{id}/responder")
    public ResponseEntity<ResultadoIntentoDTO> responder(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody RespuestaEjercicioDTO dto) {
        return ResponseEntity.ok(ejercicioService.responder(principal.getUsername(), id, dto));
    }
}
