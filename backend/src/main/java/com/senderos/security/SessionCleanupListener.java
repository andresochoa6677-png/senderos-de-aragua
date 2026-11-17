package com.senderos.security;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.stereotype.Component;

/**
 * Limpia el ActiveSessionStore cuando una sesión HTTP termina (timeout,
 * logout o invalidación) para evitar fugas de memoria.
 */
@Component
public class SessionCleanupListener implements HttpSessionListener {

    public static final String ATTR_EMAIL = "SENDEROS_SESSION_EMAIL";

    private final ActiveSessionStore sessionStore;

    public SessionCleanupListener(ActiveSessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        String email = (String) session.getAttribute(ATTR_EMAIL);
        if (email != null) {
            sessionStore.remover(email, session);
        }
    }
}
