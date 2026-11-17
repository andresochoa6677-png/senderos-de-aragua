package com.senderos.security;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro de sesiones HTTP activas por email. Permite invalidar de forma
 * inmediata y real (session.invalidate()) todas las sesiones de un usuario
 * cuando cambia su contraseña, garantizando que la sesión anterior no
 * sobreviva ninguna petición posterior.
 *
 * Con maximumSessions(1) solo la sesión MÁS RECIENTE por usuario está
 * realmente viva: registrar() REEMPLAZA la entrada anterior (la expulsada
 * por el login deja de contarse aunque Tomcat no la haya destruido aún),
 * de modo que el contador del panel refleja exactamente las sesiones vivas.
 */
@Component
public class ActiveSessionStore {

    private final Map<String, Set<HttpSession>> sesionesPorEmail = new ConcurrentHashMap<>();

    public void registrar(String email, HttpSession session) {
        Set<HttpSession> unica = ConcurrentHashMap.newKeySet();
        unica.add(session);
        sesionesPorEmail.put(email, unica);
    }

    public void remover(String email, HttpSession session) {
        sesionesPorEmail.computeIfPresent(email, (k, sesiones) -> {
            sesiones.remove(session);
            return sesiones.isEmpty() ? null : sesiones;
        });
    }

    /**
     * Invalida físicamente toda sesión almacenada del usuario distinta de
     * la indicada (la nueva del último login). Devuelve cuántas murieron.
     */
    public int invalidarOtras(String email, String idSesionActual) {
        Set<HttpSession> sesiones = sesionesPorEmail.get(email);
        if (sesiones == null) {
            return 0;
        }
        int invalidadas = 0;
        for (HttpSession session : sesiones) {
            if (!session.getId().equals(idSesionActual)) {
                try {
                    session.invalidate();
                    invalidadas++;
                } catch (IllegalStateException yaInvalidada) {
                    // ya muerta: el listener ya la retiró
                }
            }
        }
        return invalidadas;
    }

    /**
     * Invalida de inmediato todas las sesiones activas del usuario.
     * Devuelve la cantidad de sesiones invalidadas.
     */
    public int invalidarTodas(String email) {
        Set<HttpSession> sesiones = sesionesPorEmail.remove(email);
        if (sesiones == null) {
            return 0;
        }
        int invalidadas = 0;
        for (HttpSession session : sesiones) {
            try {
                session.invalidate();
                invalidadas++;
            } catch (IllegalStateException yaInvalidada) {
                // La sesión ya expiró o fue invalidada antes: nada por hacer.
            }
        }
        return invalidadas;
    }

    public int cantidadSesiones(String email) {
        Set<HttpSession> sesiones = sesionesPorEmail.get(email);
        return sesiones == null ? 0 : sesiones.size();
    }
}
