package com.senderos.controller;

import com.senderos.dto.EncuestaRespuestaRequest;
import com.senderos.dto.EncuestaResponse;
import com.senderos.dto.OnboardingEstadoResponse;
import com.senderos.dto.RegistroViviendaRequest;
import com.senderos.dto.UsuarioResponse;
import com.senderos.security.RateLimiter;
import com.senderos.service.ConsentimientoService;
import com.senderos.service.OnboardingService;
import com.senderos.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final UsuarioService usuarioService;
    private final RateLimiter rateLimiter;
    private final ConsentimientoService consentimientoService;

    public OnboardingController(OnboardingService onboardingService, UsuarioService usuarioService,
                                RateLimiter rateLimiter, ConsentimientoService consentimientoService) {
        this.onboardingService = onboardingService;
        this.usuarioService = usuarioService;
        this.rateLimiter = rateLimiter;
        this.consentimientoService = consentimientoService;
    }

    /**
     * Endpoint público de escritura: cada intento consume una vivienda del
     * catálogo y dispara un correo. Sin tasa límite, un script automatizado
     * agota las viviendas del condominio con cuentas fantasma y convierte a
     * la plataforma en emisor de spam (auditoría 2026-09-12, A-1).
     */
    @PostMapping("/registro")
    public ResponseEntity<?> registrar(
            @Valid @RequestBody RegistroViviendaRequest request,
            HttpServletRequest httpRequest) {
        if (!rateLimiter.tryAcquire("registro:" + httpRequest.getRemoteAddr())) {
            return ResponseEntity.status(429).body(Map.of(
                "error", "Demasiados intentos de registro. Espera unos minutos"));
        }
        com.senderos.dto.RegistroResponse respuesta = usuarioService.registrarVivienda(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping("/encuesta/{tipo}")
    public ResponseEntity<EncuestaResponse> obtenerEncuesta(@PathVariable String tipo) {
        return ResponseEntity.ok(onboardingService.obtenerEncuesta(tipo));
    }

    /** Documento REG-2.0 vigente: versión, texto íntegro y hash (fuente: servidor). */
    @GetMapping("/consentimiento/texto")
    public ResponseEntity<com.senderos.dto.ConsentimientoTextoDTO> textoConsentimiento() {
        return ResponseEntity.ok(consentimientoService.obtenerTexto());
    }

    /**
     * Aceptación del consentimiento REG-2.0 con trazabilidad completa:
     * versión y hash los fija el servidor; la IP proviene de la conexión.
     */
    @PostMapping("/consentimiento")
    public ResponseEntity<Map<String, String>> aceptarConsentimiento(
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest httpRequest) {
        Long viviendaId = obtenerViviendaId(principal);
        consentimientoService.aceptar(viviendaId, principal.getUsername(), httpRequest.getRemoteAddr());
        return ResponseEntity.ok(Map.of("mensaje", "Consentimiento registrado"));
    }

    /** Rechazo del consentimiento: se registra para trazabilidad y el frontend cierra la sesión. */
    @PostMapping("/consentimiento/rechazo")
    public ResponseEntity<Void> rechazarConsentimiento(
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest httpRequest) {
        Long viviendaId = obtenerViviendaId(principal);
        consentimientoService.rechazar(viviendaId, principal.getUsername(), httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/encuesta")
    public ResponseEntity<Map<String, String>> responderEncuesta(
            @Valid @RequestBody EncuestaRespuestaRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Long viviendaId = obtenerViviendaId(principal);
        int puntajeConocimiento = onboardingService.responderEncuesta(viviendaId, request);
        return ResponseEntity.ok(Map.of(
            "mensaje", "Respuestas registradas",
            "puntajeConocimiento", String.valueOf(puntajeConocimiento)));
    }

    @GetMapping("/estado")
    public ResponseEntity<OnboardingEstadoResponse> estado(
            @AuthenticationPrincipal UserDetails principal) {
        Long viviendaId = obtenerViviendaId(principal);
        return ResponseEntity.ok(onboardingService.estadoOnboarding(viviendaId));
    }

    private Long obtenerViviendaId(UserDetails principal) {
        return usuarioService.buscarViviendaIdPorEmail(principal.getUsername());
    }
}
