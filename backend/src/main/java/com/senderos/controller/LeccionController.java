package com.senderos.controller;

import com.senderos.dto.LeccionDetalleDTO;
import com.senderos.dto.LeccionResumenDTO;
import com.senderos.dto.ProgresoViviendaDTO;
import com.senderos.service.LeccionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Modulo educativo. Toda la informacion de progreso se resuelve desde la
 * sesion autenticada: el cliente nunca envia el id de vivienda.
 */
@RestController
@RequestMapping("/api/lecciones")
public class LeccionController {

    private final LeccionService leccionService;

    public LeccionController(LeccionService leccionService) {
        this.leccionService = leccionService;
    }

    @GetMapping
    public ResponseEntity<List<LeccionResumenDTO>> listar(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(leccionService.listar(principal.getUsername()));
    }

    @GetMapping("/progreso")
    public ResponseEntity<ProgresoViviendaDTO> progreso(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(leccionService.progreso(principal.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeccionDetalleDTO> detalle(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(leccionService.obtenerDetalle(principal.getUsername(), id));
    }
}
