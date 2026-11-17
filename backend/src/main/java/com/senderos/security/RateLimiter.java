package com.senderos.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rate limiter simple en memoria con ventana fija por clave.
 * Suficiente para ~200 usuarios en un solo nodo; no usar en despliegue multi-instancia.
 *
 * Seguridad (auditoría 2026-09-12):
 * - Umbrales configurables por propiedades (app.rate-limiter.*): la suite de
 *   integración los sube para no agotarlos entre casos, y los tests dedicados
 *   los bajan para disparar el 429.
 * - El mapa de claves es un LRU acotado (LinkedHashMap con acceso + sincronización):
 *   un atacante rotando IPv6 ya no puede crecer la memoria sin límite.
 */
@Component
public class RateLimiter {

    /** Tope de claves vivas; expulsa las más antiguas en usarse (LRU). */
    private static final int MAX_CLAVES = 10_000;

    private final int maxIntentos;
    private final long ventanaMillis;
    private final Map<String, Ventana> ventanas;

    public RateLimiter(@Value("${app.rate-limiter.max-intentos:10}") int maxIntentos,
                       @Value("${app.rate-limiter.ventana-minutos:10}") long ventanaMinutos) {
        this.maxIntentos = maxIntentos;
        this.ventanaMillis = ventanaMinutos * 60_000L;
        this.ventanas = Collections.synchronizedMap(new LinkedHashMap<>(128, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Ventana> eldest) {
                return size() > MAX_CLAVES;
            }
        });
    }

    public boolean tryAcquire(String clave) {
        long ahora = System.currentTimeMillis();
        synchronized (ventanas) {
            Ventana v = ventanas.get(clave);
            if (v == null || ahora - v.inicio >= ventanaMillis) {
                v = new Ventana(ahora, 1);
                ventanas.put(clave, v);
            } else {
                v.intentos++;
            }
            return v.intentos <= maxIntentos;
        }
        // Nota: la ventana se reinicia al agotar y transcurrir ventanaMillis.
    }

    public void reset(String clave) {
        synchronized (ventanas) {
            ventanas.remove(clave);
        }
    }

    private static final class Ventana {
        final long inicio;
        int intentos;

        Ventana(long inicio, int intentos) {
            this.inicio = inicio;
            this.intentos = intentos;
        }
    }
}
