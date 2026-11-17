package com.senderos.controller;

import com.senderos.dto.RestablecerPasswordRequest;
import com.senderos.dto.SolicitudResetRequest;
import com.senderos.security.RateLimiter;
import com.senderos.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/password")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;
    private final RateLimiter rateLimiter;

    public PasswordResetController(PasswordResetService passwordResetService, RateLimiter rateLimiter) {
        this.passwordResetService = passwordResetService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/solicitud")
    public ResponseEntity<Map<String, String>> solicitar(@Valid @RequestBody SolicitudResetRequest request,
                                                         HttpServletRequest httpRequest) {
        if (!rateLimiter.tryAcquire("reset:" + httpRequest.getRemoteAddr())) {
            return ResponseEntity.status(429).body(Map.of("mensaje", "Demasiadas solicitudes. Espera unos minutos"));
        }
        passwordResetService.solicitarReset(request);
        return ResponseEntity.ok(Map.of(
            "mensaje", "Si el correo esta registrado, recibiras instrucciones para restablecer tu contrasena"));
    }

    @PostMapping("/restablecer")
    public ResponseEntity<Map<String, String>> restablecer(@Valid @RequestBody RestablecerPasswordRequest request,
                                                           HttpServletRequest httpRequest) {
        // Defensa uniforme: el token es de 256 bits (fuerza bruta inviable),
        // pero el endpoint consume trabajo (BCrypt) y merece su propia tasa.
        if (!rateLimiter.tryAcquire("restablecer:" + httpRequest.getRemoteAddr())) {
            return ResponseEntity.status(429).body(Map.of("mensaje", "Demasiadas solicitudes. Espera unos minutos"));
        }
        passwordResetService.restablecer(request);
        return ResponseEntity.ok(Map.of("mensaje", "Contrasena actualizada. Ya puedes iniciar sesion"));
    }
}
