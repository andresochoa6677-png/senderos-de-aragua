package com.senderos.controller;

import com.senderos.dto.CambiarPasswordRequest;
import com.senderos.dto.LoginRequest;
import com.senderos.dto.UsuarioResponse;
import com.senderos.entity.Usuario;
import com.senderos.repository.UsuarioRepository;
import com.senderos.security.ActiveSessionStore;
import com.senderos.security.RateLimiter;
import com.senderos.security.SessionCleanupListener;
import com.senderos.service.RegistroAccesoService;
import com.senderos.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final SecurityContextRepository securityContextRepository;
    private final RateLimiter rateLimiter;
    private final ActiveSessionStore activeSessionStore;
    private final UsuarioService usuarioService;
    private final RegistroAccesoService registroAccesoService;
    private final SessionRegistry sessionRegistry;
    private final PersistentTokenBasedRememberMeServices rememberMeServices;

    public AuthController(AuthenticationManager authenticationManager, UsuarioRepository usuarioRepository,
                          SecurityContextRepository securityContextRepository, RateLimiter rateLimiter,
                          ActiveSessionStore activeSessionStore, UsuarioService usuarioService,
                          RegistroAccesoService registroAccesoService, SessionRegistry sessionRegistry,
                          PersistentTokenBasedRememberMeServices rememberMeServices) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.securityContextRepository = securityContextRepository;
        this.rateLimiter = rateLimiter;
        this.activeSessionStore = activeSessionStore;
        this.usuarioService = usuarioService;
        this.registroAccesoService = registroAccesoService;
        this.sessionRegistry = sessionRegistry;
        this.rememberMeServices = rememberMeServices;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse httpResponse) {
        // Doble tasa: por IP (atacante desde una maquina) y por cuenta
        // (fuerza bruta dirigida a un usuario aunque rote IPs, y bloqueo
        // compartido por CGNAT no castiga a un solo correo).
        String claveIp = "login:ip:" + httpRequest.getRemoteAddr();
        String claveCuenta = "login:cta:" + request.email().trim().toLowerCase();
        if (!rateLimiter.tryAcquire(claveIp) || !rateLimiter.tryAcquire(claveCuenta)) {
            return ResponseEntity.status(429).body(Map.of("error", "Demasiados intentos. Espera unos minutos"));
        }
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

            // Cuenta pendiente de verificación: credenciales válidas pero sin
            // acceso hasta confirmar el correo. No se crea sesión ni se
            // resetea el rate limiter (no es un inicio de sesión exitoso).
            Usuario usuario = usuarioRepository.findByEmail(request.email()).orElseThrow();
            if (!usuario.isEmailVerificado() && !usuario.isDemo()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "Debes verificar tu correo antes de iniciar sesión. Revisa tu bandeja o solicita un nuevo enlace.",
                    "codigo", "PENDIENTE_VERIFICACION",
                    "email", request.email()));
            }

            rateLimiter.reset(claveIp);
            rateLimiter.reset(claveCuenta);

            // Proteccion de fijacion de sesion: crear la sesion y rotar el id
            // ANTES de vincularla al usuario. changeSessionId() no crea sesion,
            // por eso getSession() va primero.
            HttpSession session = httpRequest.getSession(true);
            httpRequest.changeSessionId();
            session = httpRequest.getSession();
            session.setAttribute(SessionCleanupListener.ATTR_EMAIL, authentication.getName());

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, httpRequest, httpResponse);

            // "Mantener sesión activa": cookie persistente que re-autentica
            // al residente aunque la sesión expire o el servidor se reinicie.
            if (Boolean.TRUE.equals(request.recordar())) {
                rememberMeServices.loginSuccess(httpRequest, httpResponse, authentication);
            }

            // Sesión HTTP real registrada para poder invalidarla al cambiar/resetear contraseña.
            // Control de sesiones concurrentes REAL para el login manual:
            // registrar la sesión nueva en el registry y expirar las anteriores
            // del mismo usuario (maximumSessions(1) solo actúa sobre sesiones
            // registradas; sin esto el login manual las acumulaba).
            sessionRegistry.registerNewSession(session.getId(), authentication.getPrincipal());
            for (SessionInformation previa : sessionRegistry.getAllSessions(authentication.getPrincipal(), false)) {
                if (!previa.getSessionId().equals(session.getId())) {
                    previa.expireNow();
                }
            }
            // La sesión anterior muere DE VERDAD aquí (maximumSessions la
            // expira solo si está registrada; invalidarla garantiza el 401).
            activeSessionStore.invalidarOtras(authentication.getName(), session.getId());
            activeSessionStore.registrar(authentication.getName(), session);
            // Trazabilidad del estudio: inicio de sesión de la vivienda.
            registroAccesoService.registrar(authentication.getName(), httpRequest.getRemoteAddr(),
                RegistroAccesoService.MODULO_SESION);

            Usuario usuarioAutenticado = usuarioRepository.findByEmail(request.email()).orElseThrow();
            return ResponseEntity.ok(toResponse(usuarioAutenticado));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Credenciales inválidas"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Usuario usuario = usuarioRepository.findByEmail(principal.getUsername()).orElseThrow();
        return ResponseEntity.ok(toResponse(usuario));
    }

    /**
     * Emite la cookie XSRF-TOKEN para el primer POST (login/registro) en el SPA.
     * Al serializar el token se escribe la cookie via CookieCsrfTokenRepository.
     */
    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }

    /**
     * Cambio de contraseña autenticado: invalida TODAS las sesiones activas
     * del usuario de inmediato. La sesión actual también muere y el cliente
     * debe volver a iniciar sesión.
     */
    @PostMapping("/password")
    public ResponseEntity<Map<String, String>> cambiarPassword(@Valid @RequestBody CambiarPasswordRequest request,
                                                               @AuthenticationPrincipal UserDetails principal) {
        Usuario usuario = usuarioRepository.findByEmail(principal.getUsername()).orElseThrow();
        usuarioService.cambiarPassword(usuario.getId(), request.passwordActual(), request.passwordNueva());
        int invalidadas = activeSessionStore.invalidarTodas(principal.getUsername());
        return ResponseEntity.ok(Map.of(
            "mensaje", "Contrasena actualizada. Inicia sesion de nuevo",
            "sesionesInvalidadas", String.valueOf(invalidadas)));
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        String rol = usuario.getRoles().stream().map(r -> r.getNombre()).findFirst().orElse("RESIDENTE");
        return new UsuarioResponse(
            usuario.getId(), usuario.getEmail(), usuario.getNombre(), usuario.isDemo(),
            usuario.getVivienda() != null ? usuario.getVivienda().getId() : null,
            usuario.getVivienda() != null ? usuario.getVivienda().getCodigo() : null,
            rol);
    }
}

