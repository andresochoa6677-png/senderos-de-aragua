package com.senderos.support;

import com.senderos.entity.PasswordResetToken;
import com.senderos.entity.Usuario;
import com.senderos.repository.PasswordResetTokenRepository;
import com.senderos.repository.UsuarioRepository;
import com.senderos.security.Tokens;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * SOLO PARA PRUEBAS: genera un token de recuperación de contraseña en claro
 * para un usuario, replicando lo que haría PasswordResetService.solicitarReset
 * (sin enviar correo). Sustituye al método público generarToken() que vivía en
 * el servicio de producción (retirado para que la API de producción no exponga
 * utilidades de prueba).
 */
@Component
public class TokensDePrueba {

    private static final Duration VALIDEZ = Duration.ofMinutes(30);

    @Autowired
    private PasswordResetTokenRepository tokenRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Transactional
    public String crear(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + email));
        tokenRepository.deleteByUsuarioId(usuario.getId());
        String token = Tokens.generarToken();
        PasswordResetToken entidad = new PasswordResetToken();
        entidad.setUsuario(usuario);
        entidad.setTokenHash(Tokens.sha256(token));
        entidad.setExpiraEn(Instant.now().plus(VALIDEZ));
        tokenRepository.save(entidad);
        return token;
    }
}
