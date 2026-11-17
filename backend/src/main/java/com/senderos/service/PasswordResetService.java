package com.senderos.service;

import com.senderos.dto.RestablecerPasswordRequest;
import com.senderos.dto.SolicitudResetRequest;
import com.senderos.entity.PasswordResetToken;
import com.senderos.entity.Usuario;
import com.senderos.exception.BusinessException;
import com.senderos.repository.PasswordResetTokenRepository;
import com.senderos.repository.UsuarioRepository;
import com.senderos.security.ActiveSessionStore;
import com.senderos.security.Tokens;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final Duration VALIDEZ = Duration.ofMinutes(30);

    private final PasswordResetTokenRepository tokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActiveSessionStore activeSessionStore;
    private final EmailService emailService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final String baseUrl;
    private final boolean logTokenHabilitado;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                UsuarioRepository usuarioRepository,
                                PasswordEncoder passwordEncoder,
                                ActiveSessionStore activeSessionStore,
                                EmailService emailService,
                                org.springframework.jdbc.core.JdbcTemplate jdbcTemplate,
                                @Value("${app.base-url:http://localhost:5173}") String baseUrl,
                                Environment environment,
                                @Value("${app.password-reset.log-token:false}") boolean logTokenHabilitado) {
        this.tokenRepository = tokenRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.activeSessionStore = activeSessionStore;
        this.emailService = emailService;
        this.jdbcTemplate = jdbcTemplate;
        this.baseUrl = baseUrl;
        // Doble candado: en produccion el token nunca se registra, ni aunque
        // alguien configure la variable de entorno por error.
        this.logTokenHabilitado = logTokenHabilitado && environment.acceptsProfiles(Profiles.of("prod")) == false;
    }

    /**
     * Genera el token de reset. El token JAMÁS se devuelve en la respuesta HTTP:
     * solo se entrega por el canal de correo. En entornos locales sin mailer se
     * puede registrar en el log (app.password-reset.log-token=true, solo dev).
     */
    @Transactional
    public void solicitarReset(SolicitudResetRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email().trim().toLowerCase())
            .orElse(null);
        if (usuario == null) {
            return; // respuesta genérica: no revelar existencia de cuentas
        }
        tokenRepository.deleteByUsuarioId(usuario.getId());
        String token = Tokens.generarToken();
        PasswordResetToken entidad = new PasswordResetToken();
        entidad.setUsuario(usuario);
        entidad.setTokenHash(Tokens.sha256(token));
        entidad.setExpiraEn(Instant.now().plus(VALIDEZ));
        tokenRepository.save(entidad);
        emailService.enviarRecuperacionContrasena(usuario.getEmail(), baseUrl + "/restablecer?token=" + token);
        if (logTokenHabilitado) {
            log.warn("Token de reset (solo entorno de desarrollo) para {}: {}", usuario.getEmail(), token);
        }
    }

    @Transactional
    public void restablecer(RestablecerPasswordRequest request) {
        PasswordResetToken entidad = tokenRepository.findByTokenHash(Tokens.sha256(request.token()))
            .orElseThrow(() -> new BusinessException("Token invalido o expirado"));
        if (!entidad.isVigente()) {
            throw new BusinessException("Token invalido o expirado");
        }
        Usuario usuario = entidad.getUsuario();
        usuario.setPasswordHash(passwordEncoder.encode(request.nuevaPassword()));
        entidad.setUsado(true);
        tokenRepository.save(entidad);
        // Dispositivos con "mantener sesión activa" dejan de estar autenticados.
        jdbcTemplate.update("DELETE FROM persistent_logins WHERE username = ?", usuario.getEmail());
        // Invalida TODAS las sesiones HTTP activas del usuario AHORA: la sesión
        // anterior no debe sobrevivir ninguna petición posterior.
        activeSessionStore.invalidarTodas(usuario.getEmail());
    }
}
