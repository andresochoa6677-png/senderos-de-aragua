package com.senderos.config;

import com.senderos.service.RegistroAccesoService;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interceptor que registra la CONSULTA de módulos por vivienda
 * (trazabilidad declarada en el consentimiento REG-2.0). Solo GET de
 * controladores exitosos; los endpoints de administración y auth no se
 * registran. El login se registra aparte (modulo = SESION).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RegistroAccesoService registroAccesoService;

    public WebConfig(RegistroAccesoService registroAccesoService) {
        this.registroAccesoService = registroAccesoService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                        Object handler, Exception ex) {
                if (ex != null || response.getStatus() >= 400) {
                    return;
                }
                String modulo = moduloDe(request.getRequestURI());
                if (modulo == null) {
                    return;
                }
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() instanceof String) {
                    return; // anónimo
                }
                registroAccesoService.registrar(auth.getName(), request.getRemoteAddr(), modulo);
            }
        }).addPathPatterns("/api/**");
    }

    private static String moduloDe(String uri) {
        if (uri.startsWith("/api/lecciones")) return "LECCIONES";
        if (uri.startsWith("/api/ejercicios")) return "EJERCICIOS";
        if (uri.startsWith("/api/practica")) return "PRACTICA";
        if (uri.startsWith("/api/comentarios")) return "COMENTARIOS";
        if (uri.startsWith("/api/encuesta-final")) return "ENCUESTA_FINAL";
        if (uri.startsWith("/api/onboarding/encuesta")) return "ENCUESTA";
        return null;
    }
}
