package com.senderos.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Configuración pública del frontend: solo indicadores no sensibles.
 * mailCaptura=true significa que la plataforma está en modo desarrollo
 * y los correos NO salen a Internet: se guardan localmente.
 */
@RestController
public class ConfigPublicaController {

    private final boolean mailCaptura;

    public ConfigPublicaController(@Value("${app.mail.modo:captura}") String mailModo) {
        this.mailCaptura = "captura".equalsIgnoreCase(mailModo);
    }

    @GetMapping("/api/config")
    public ResponseEntity<Map<String, Object>> config() {
        return ResponseEntity.ok(Map.of("mailCaptura", mailCaptura));
    }
}
