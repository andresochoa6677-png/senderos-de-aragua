package com.senderos.controller;

import com.senderos.security.RateLimiter;
import com.senderos.service.VerificacionEmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Verificación de correo: activación de cuentas y reenvío del enlace.
 * Ambos endpoints son públicos pero con rate limiting y respuestas
 * genéricas para no revelar qué correos existen.
 */
@RestController
@RequestMapping("/api/auth")
public class VerificacionEmailController {

    public record VerificarCorreoRequest(@NotBlank String token) {}

    public record ReenvioRequest(@NotBlank String email) {}

    private final VerificacionEmailService verificacionEmailService;
    private final RateLimiter rateLimiter;

    public VerificacionEmailController(VerificacionEmailService verificacionEmailService, RateLimiter rateLimiter) {
        this.verificacionEmailService = verificacionEmailService;
        this.rateLimiter = rateLimiter;
    }

    /** Activa la cuenta con el token del correo. Errores sin detalle interno. */
    @PostMapping("/verificar-correo")
    public ResponseEntity<Map<String, String>> verificar(@Valid @RequestBody VerificarCorreoRequest request,
                                                         HttpServletRequest httpRequest) {
        // Mismo criterio que el reenvio: tasa por IP aunque el token sea de
        // 256 bits; mantiene la defensa uniforme en todos los endpoints publicos.
        if (!rateLimiter.tryAcquire("verificar:" + httpRequest.getRemoteAddr())) {
            return ResponseEntity.status(429).body(Map.of(
                "mensaje", "Demasiadas solicitudes. Espera unos minutos antes de reintentar"));
        }
        verificacionEmailService.verificar(request.token());
        return ResponseEntity.ok(Map.of(
            "mensaje", "Correo verificado. Tu cuenta ya esta activa, puedes iniciar sesion"));
    }

    /** Reenvío del enlace: respuesta genérica siempre + rate limit por IP. */
    @PostMapping("/reenviar-verificacion")
    public ResponseEntity<Map<String, String>> reenviar(@Valid @RequestBody ReenvioRequest request,
                                                        HttpServletRequest httpRequest) {
        if (!rateLimiter.tryAcquire("reenvio:" + httpRequest.getRemoteAddr())) {
            return ResponseEntity.status(429).body(Map.of(
                "mensaje", "Demasiadas solicitudes. Espera unos minutos antes de reintentar"));
        }
        try {
            verificacionEmailService.solicitarReenvio(request.email());
        } catch (Exception e) {
            // Fallo del envío (SMTP caido, p. ej.): jamas se reporta como enviado.
            // El mensaje es generico: no revela datos ni secretos.
            return ResponseEntity.status(500).body(Map.of(
                "mensaje", "No pudimos enviar el correo en este momento. Intenta de nuevo en unos minutos"));
        }
        return ResponseEntity.ok(Map.of(
            "mensaje", "Si el correo esta registrado y pendiente de verificacion, te enviamos un nuevo enlace"));
    }
}
