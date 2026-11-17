package com.senderos.service;

import com.senderos.entity.EmailVerificationToken;
import com.senderos.entity.Usuario;
import com.senderos.exception.BusinessException;
import com.senderos.repository.EmailVerificationTokenRepository;
import com.senderos.repository.UsuarioRepository;
import com.senderos.security.Tokens;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Verificación de correo electrónico: al registrarse la cuenta queda
 * PENDIENTE hasta abrir el enlace. Los tokens son de un solo uso, con
 * hash en BD y expiración de 24 h; cada reenvío invalida el anterior.
 */
@Service
public class VerificacionEmailService {

    public static final String MENSAJE_ENLACE_INVALIDO =
        "El enlace no es valido o ya expiro. Solicita un nuevo correo de verificacion.";

    private static final Duration VALIDEZ = Duration.ofHours(24);

    private final EmailVerificationTokenRepository tokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final String baseUrl;

    public VerificacionEmailService(EmailVerificationTokenRepository tokenRepository,
                                    UsuarioRepository usuarioRepository,
                                    EmailService emailService,
                                    @Value("${app.base-url:http://localhost:5173}") String baseUrl) {
        this.tokenRepository = tokenRepository;
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
        this.baseUrl = baseUrl;
    }

    /** Genera un token nuevo (invalidando el anterior) y envía el correo. */
    @Transactional
    public void crearYEnviar(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email.trim().toLowerCase())
            .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        if (usuario.isEmailVerificado()) {
            return;
        }
        tokenRepository.deleteByUsuarioId(usuario.getId());
        String token = Tokens.generarToken();
        tokenRepository.save(new EmailVerificationToken(usuario, Tokens.sha256(token), Instant.now().plus(VALIDEZ)));
        emailService.enviarVerificacionCorreo(usuario.getEmail(), baseUrl + "/verificar?token=" + token);
    }

    /** Activa la cuenta si el token es válido, no usado y no expirado. */
    @Transactional
    public void verificar(String token) {
        EmailVerificationToken entidad = tokenRepository.findByTokenHash(Tokens.sha256(token))
            .orElseThrow(() -> new BusinessException(MENSAJE_ENLACE_INVALIDO));
        if (!entidad.isVigente()) {
            throw new BusinessException(MENSAJE_ENLACE_INVALIDO);
        }
        Usuario usuario = entidad.getUsuario();
        usuario.setEmailVerificado(true);
        usuario.setFechaVerificacion(Instant.now());
        entidad.setUsado(true);
        tokenRepository.save(entidad);
    }

    /**
     * Reenvío del enlace: respuesta genérica en el controlador; aquí solo
     * se envía si la cuenta existe y sigue pendiente. Invalida el token anterior.
     */
    @Transactional
    public void solicitarReenvio(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email.trim().toLowerCase()).orElse(null);
        if (usuario == null || usuario.isEmailVerificado()) {
            return;
        }
        crearYEnviar(usuario.getEmail());
    }
}
