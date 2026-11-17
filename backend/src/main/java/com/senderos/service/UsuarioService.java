package com.senderos.service;

import com.senderos.dto.RegistroViviendaRequest;
import com.senderos.dto.UsuarioResponse;
import com.senderos.entity.Rol;
import com.senderos.entity.Usuario;
import com.senderos.entity.Vivienda;
import com.senderos.exception.BusinessException;
import com.senderos.exception.ConflictException;
import com.senderos.exception.NotFoundException;
import com.senderos.repository.RolRepository;
import com.senderos.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    public static final String MENSAJE_EMAIL_DUPLICADO =
        "Esta plataforma utiliza una cuenta por vivienda. Si tu vivienda ya tiene una cuenta registrada, "
            + "inicia sesión con esa cuenta o utiliza la recuperación de contraseña.";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AsignacionViviendaService asignacionViviendaService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                          PasswordEncoder passwordEncoder, AsignacionViviendaService asignacionViviendaService,
                          org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.asignacionViviendaService = asignacionViviendaService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Registro de residentes. El residente ESCOGE su vivienda (bloque +
     * número del censo oficial BLOQUE-NUMERO) y el backend valida que
     * exista y esté disponible; la garantía de unicidad la impone la BD
     * (uk_usuario_vivienda). Si dos registros simultáneos eligen la misma
     * vivienda, el perdedor recibe el mensaje de vivienda ocupada.
     */
    public com.senderos.dto.RegistroResponse registrarVivienda(RegistroViviendaRequest request) {
        String email = request.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflictException(MENSAJE_EMAIL_DUPLICADO);
        }
        Vivienda vivienda = asignacionViviendaService.viviendaElegida(request.bloque(), request.numero());
        try {
            UsuarioResponse respuesta = asignacionViviendaService.registrarEnVivienda(
                    construirUsuario(request, email), vivienda);
            // Sin verificación por correo (decisión de implementación): la
            // cuenta nace activa y el residente entra de inmediato. La
            // recuperación de contraseña sigue por correo (Brevo).
            return new com.senderos.dto.RegistroResponse(respuesta, true);
        } catch (DataIntegrityViolationException e) {
            if (esViolacionDe(e, "uk_usuario_email")) {
                throw new ConflictException(MENSAJE_EMAIL_DUPLICADO);
            }
            // La vivienda fue tomada por un registro concurrente entre la
            // validación y el insert: mensaje claro de vivienda ocupada.
            throw new ConflictException(AsignacionViviendaService.mensajeOcupada(vivienda.getCodigo()));
        }
    }

    private Usuario construirUsuario(RegistroViviendaRequest request, String email) {
        Rol residente = rolRepository.findByNombre("RESIDENTE")
            .orElseThrow(() -> new NotFoundException("Rol RESIDENTE no encontrado"));
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setNombre(request.nombre().trim());
        usuario.setRoles(Set.of(residente));
        // Nace verificada: sin paso de correo para empezar a usar la plataforma.
        usuario.setEmailVerificado(true);
        usuario.setFechaVerificacion(Instant.now());
        return usuario;
    }

    private boolean esViolacionDe(DataIntegrityViolationException e, String constraint) {
        String mensaje = e.getMostSpecificCause().getMessage();
        return mensaje != null && mensaje.toLowerCase().contains(constraint);
    }

    @Transactional
    public void cambiarPassword(Long usuarioId, String passwordActual, String passwordNueva) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        if (!passwordEncoder.matches(passwordActual, usuario.getPasswordHash())) {
            throw new BusinessException("Contrasena actual incorrecta");
        }
        usuario.setPasswordHash(passwordEncoder.encode(passwordNueva));
        // Un dispositivo con "mantener sesión activa" deja de estar
        // autenticado tras un cambio de contraseña.
        jdbcTemplate.update("DELETE FROM persistent_logins WHERE username = ?", usuario.getEmail());
    }

    /**
     * Vivienda del usuario autenticado. Lanza NotFound si el usuario no tiene
     * vivienda asociada (p.ej. admin): así ningún endpoint deriva datos de una
     * vivienda que no sea la propia.
     */
    @Transactional(readOnly = true)
    public Vivienda obtenerViviendaDeUsuario(String email) {
        return usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException("Usuario no encontrado"))
            .getVivienda();
    }

    @Transactional(readOnly = true)
    public Long buscarViviendaIdPorEmail(String email) {
        return usuarioRepository.findViviendaIdByEmail(email)
            .orElseThrow(() -> new NotFoundException("Usuario sin vivienda asociada"));
    }
}
