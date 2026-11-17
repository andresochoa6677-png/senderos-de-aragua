package com.senderos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.senderos.entity.Usuario;
import com.senderos.repository.UsuarioRepository;

import java.time.Instant;
import java.util.List;

/**
 * Limpieza de cuentas que nunca verificaron su correo (auditoría 2026-09-12, A-1).
 *
 * El registro reserva la vivienda aunque la cuenta nazca PENDIENTE; sin este
 * trabajo, un atacante automatizado (o vecinos que desisten) agota el catálogo
 * de viviendas con cuentas fantasma. Tras DIAS_PENDIENTE sin verificar, la
 * cuenta y TODO su rastro se eliminan y la vivienda vuelve a ser asignable.
 *
 * Orden de borrado: primero los datos que cuelgan de la vivienda (comentario,
 * respuesta, intento_ejercicio, progreso_leccion, intento_practica,
 * consentimiento — sus FK solo cascaden al borrar la VIVIENDA, no el usuario)
 * y al final el usuario (sus FK propias cascaden: usuario_rol, tokens,
 * comentario). Nunca toca cuentas demo ni verificadas.
 */
@Service
public class LimpiezaCuentasService {

    private static final Logger log = LoggerFactory.getLogger(LimpiezaCuentasService.class);

    /** Tope por pasada: acota la transacción en un arranque muy atrasado. */
    private static final int LOTE = 500;

    private final UsuarioRepository usuarioRepository;
    private final JdbcTemplate jdbcTemplate;
    private final long diasPendiente;

    public LimpiezaCuentasService(UsuarioRepository usuarioRepository,
                                  JdbcTemplate jdbcTemplate,
                                  @Value("${app.limpieza.dias-pendiente:7}") long diasPendiente) {
        this.usuarioRepository = usuarioRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.diasPendiente = diasPendiente;
    }

    /** Cada 6 horas, con 1 minuto de gracia tras el arranque. */
    @Scheduled(fixedDelay = 6 * 60 * 60 * 1000L, initialDelay = 60_000L)
    @Transactional
    public void limpiezaProgramada() {
        int liberadas = limpiarPendientesAntiguos();
        if (liberadas > 0) {
            log.info("Limpieza de cuentas sin verificar: {} viviendas liberadas (umbral {} días)",
                    liberadas, diasPendiente);
        }
    }

    /**
     * Elimina cuentas pendientes de verificación creadas antes del umbral.
     * @return cantidad de cuentas eliminadas (y viviendas liberadas).
     */
    @Transactional
    public int limpiarPendientesAntiguos() {
        Instant corte = Instant.now().minus(java.time.Duration.ofDays(diasPendiente));
        Pageable lote = PageRequest.of(0, LOTE);
        List<Usuario> obsoletos = usuarioRepository.findPendientesSinVerificar(corte, lote);
        for (Usuario usuario : obsoletos) {
            Long viviendaId = usuario.getVivienda() != null ? usuario.getVivienda().getId() : null;
            if (viviendaId != null) {
                jdbcTemplate.update("DELETE FROM comentario WHERE vivienda_id = ?", viviendaId);
                jdbcTemplate.update("DELETE FROM respuesta WHERE vivienda_id = ?", viviendaId);
                jdbcTemplate.update("DELETE FROM intento_ejercicio WHERE vivienda_id = ?", viviendaId);
                jdbcTemplate.update("DELETE FROM progreso_leccion WHERE vivienda_id = ?", viviendaId);
                jdbcTemplate.update("DELETE FROM intento_practica WHERE vivienda_id = ?", viviendaId);
                jdbcTemplate.update("DELETE FROM consentimiento WHERE vivienda_id = ?", viviendaId);
            }
            usuarioRepository.delete(usuario);
        }
        return obsoletos.size();
    }
}
