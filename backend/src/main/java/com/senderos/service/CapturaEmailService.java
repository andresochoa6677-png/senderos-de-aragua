package com.senderos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Adaptador de correo para desarrollo y pruebas (app.mail.modo=captura,
 * modo por defecto): escribe cada correo como archivo de texto en
 * correo-saliente/ para poder abrir el enlace sin SMTP real, y conserva
 * los últimos envíos en memoria para las pruebas de integración.
 * NO crea cuentas ni sustituye el flujo real: es solo el buzón local.
 */
@Service
@ConditionalOnProperty(name = "app.mail.modo", havingValue = "captura", matchIfMissing = true)
public class CapturaEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(CapturaEmailService.class);

    public record EmailCapturado(String tipo, String destino, String enlace, Instant momento) {}

    private static final int MAX_HISTORIAL = 50;

    private final Path directorio;
    private final Deque<EmailCapturado> enviados = new ConcurrentLinkedDeque<>();

    public CapturaEmailService(@Value("${app.mail.captura-dir:correo-saliente}") String directorio) {
        this.directorio = Path.of(directorio);
    }

    @Override
    public void enviarVerificacionCorreo(String destino, String enlace) {
        enviar("verificacion", destino, "Verifica tu cuenta de Senderos de Aragua", enlace,
            "Gracias por registrarte. Abre el siguiente enlace para activar tu cuenta (expira en 24 horas):");
    }

    @Override
    public void enviarCorreoPrueba(String destino) {
        enviar("prueba", destino, "Correo de prueba - Senderos de Aragua", "(este correo no contiene enlaces)",
            "Este es un correo de PRUEBA del sistema de envío. Si lo recibiste, el canal de correo funciona.");
    }

    /** ¿Existe un correo capturado de ese tipo para ese destino? (verificación en pruebas) */
    public boolean capturado(String tipo, String destino) {
        return enviados.stream().anyMatch(e -> e.tipo().equals(tipo) && e.destino().equals(destino));
    }

    @Override
    public void enviarRecuperacionContrasena(String destino, String enlace) {
        enviar("recuperacion", destino, "Recupera tu contrasena - Senderos de Aragua", enlace,
            "Abre el siguiente enlace para crear una contrasena nueva (expira en 30 minutos):");
    }

    /** Último enlace enviado a un destino para un tipo (para pruebas). */
    public String ultimoEnlace(String tipo, String destino) {
        // El envío mas reciente queda en la cabeza del deque: el iterador
        // normal lo recorre de mas nuevo a mas viejo.
        for (Iterator<EmailCapturado> it = enviados.iterator(); it.hasNext();) {
            EmailCapturado e = it.next();
            if (e.tipo().equals(tipo) && e.destino().equals(destino)) {
                return e.enlace();
            }
        }
        throw new IllegalStateException("No hay correo capturado de tipo " + tipo + " para " + destino);
    }

    private synchronized void enviar(String tipo, String destino, String asunto, String enlace, String introduccion) {
        String cuerpo = asunto + "\n\n"
            + introduccion + "\n\n"
            + enlace + "\n\n"
            + "Si no fuiste tu, ignora este mensaje.\n"
            + "Senderos de Aragua - Gestion de residuos solidos\n";
        try {
            Files.createDirectories(directorio);
            String archivo = tipo + "-" + System.currentTimeMillis() + "-" + destino.replaceAll("[^a-zA-Z0-9@.]", "_") + ".txt";
            Files.writeString(directorio.resolve(archivo), cuerpo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el correo capturado", e);
        }
        enviados.push(new EmailCapturado(tipo, destino, enlace, Instant.now()));
        while (enviados.size() > MAX_HISTORIAL) {
            enviados.pollLast();
        }
        log.info("[CORREO CAPTURADO {}] para {}: enlace {} (bean={})", tipo, destino, enlace,
            System.identityHashCode(this));
    }
}
