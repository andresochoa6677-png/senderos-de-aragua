package com.senderos.service;

import com.senderos.dto.UsuarioResponse;
import com.senderos.entity.Usuario;
import com.senderos.entity.Vivienda;
import com.senderos.exception.BusinessException;
import com.senderos.exception.ConflictException;
import com.senderos.repository.UsuarioRepository;
import com.senderos.repository.ViviendaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vivienda elegida por el residente en el registro (censo oficial
 * BLOQUE-NUMERO: A-1, G-5, N-12, RR-3, Ñ-2).
 *
 * El backend valida que la vivienda exista en el catálogo y esté
 * disponible. La garantía UNA VIVIENDA -> MAXIMO UNA CUENTA la impone
 * la base de datos (uk_usuario_vivienda): si dos residentes eligen la
 * misma vivienda al mismo tiempo, el insert del segundo viola la
 * restricción y el orquestador lo traduce en un error claro para que
 * escoja otra.
 */
@Service
public class AsignacionViviendaService {

    /** Mensaje claro con salida, exigido para el caso de vivienda ocupada. */
    public static String mensajeOcupada(String codigo) {
        return "La vivienda " + codigo + " ya está registrada. Escoge otra vivienda, "
            + "o comunícate con el equipo investigador si crees que es un error";
    }

    private final ViviendaRepository viviendaRepository;
    private final UsuarioRepository usuarioRepository;

    public AsignacionViviendaService(ViviendaRepository viviendaRepository,
                                     UsuarioRepository usuarioRepository) {
        this.viviendaRepository = viviendaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Resuelve la vivienda elegida (bloque + número) y verifica que
     * exista en el censo, esté activa, no sea la demo y no tenga cuenta.
     * 422 si no existe (bloque/número inválidos), 409 si está ocupada.
     */
    @Transactional(readOnly = true)
    public Vivienda viviendaElegida(String bloque, Integer numero) {
        String codigo = bloque.trim().toUpperCase() + "-" + numero;
        Vivienda vivienda = viviendaRepository.findByCodigo(codigo)
            .orElseThrow(() -> new BusinessException(
                "La vivienda " + codigo + " no existe en el catálogo del condominio. "
                    + "Revisa el bloque y el número"));
        if (vivienda.isDemo() || !vivienda.isActiva()) {
            throw new BusinessException(
                "La vivienda " + codigo + " no está disponible para registro");
        }
        if (usuarioRepository.existsByViviendaId(vivienda.getId())) {
            throw new ConflictException(mensajeOcupada(codigo));
        }
        return vivienda;
    }

    /**
     * Crea la cuenta sobre la vivienda elegida. Desde REG-2.0 el registro
     * NO graba consentimiento: la aceptación firmada (con IP y hash) la
     * registra el residente en la pantalla del onboarding.
     *
     * @throws com.springframework.dao.DataIntegrityViolationException si la
     *         vivienda acaba de ser tomada por un registro concurrente;
     *         el orquestador lo traduce en el mensaje de vivienda ocupada.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UsuarioResponse registrarEnVivienda(Usuario usuario, Vivienda vivienda) {
        usuario.setVivienda(vivienda);
        Usuario guardado = usuarioRepository.saveAndFlush(usuario);
        return UsuarioResponse.de(guardado);
    }
}
