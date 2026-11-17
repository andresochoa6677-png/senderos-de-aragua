package com.senderos.service;

import com.senderos.entity.RegistroAcceso;
import com.senderos.entity.Usuario;
import com.senderos.entity.Vivienda;
import com.senderos.repository.RegistroAccesoRepository;
import com.senderos.repository.UsuarioRepository;
import com.senderos.repository.ViviendaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Registro de accesos por vivienda: inicio de sesión y consultas de
 * módulos. Es la trazabilidad de "accesos y módulos consultados"
 * declarada en el consentimiento REG-2.0. Falla en silencio: un error
 * de registro JAMÁS debe interrumpir la petición del residente.
 */
@Service
public class RegistroAccesoService {

    public static final String MODULO_SESION = "SESION";

    private final RegistroAccesoRepository registroAccesoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ViviendaRepository viviendaRepository;

    public RegistroAccesoService(RegistroAccesoRepository registroAccesoRepository,
                                 UsuarioRepository usuarioRepository,
                                 ViviendaRepository viviendaRepository) {
        this.registroAccesoRepository = registroAccesoRepository;
        this.usuarioRepository = usuarioRepository;
        this.viviendaRepository = viviendaRepository;
    }

    /** Registra un acceso si el usuario tiene vivienda (el admin no la tiene). */
    @Transactional
    public void registrar(String email, String ipOrigen, String modulo) {
        try {
            usuarioRepository.findByEmail(email.trim().toLowerCase()).ifPresent(usuario -> {
                Vivienda vivienda = usuario.getVivienda();
                if (vivienda == null) {
                    return; // cuentas sin vivienda (administración): no se registran accesos
                }
                RegistroAcceso acceso = new RegistroAcceso();
                acceso.setVivienda(vivienda);
                acceso.setUsuario(usuario);
                acceso.setIpOrigen(ipOrigen);
                acceso.setModulo(modulo);
                registroAccesoRepository.save(acceso);
            });
        } catch (RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger(RegistroAccesoService.class)
                .warn("No se pudo registrar el acceso ({}): {}", modulo, e.getMessage());
        }
    }

    /** Últimos accesos de una vivienda (más recientes primero). */
    @Transactional(readOnly = true)
    public List<RegistroAcceso> ultimosAccesos(Long viviendaId, int limite) {
        return registroAccesoRepository.findByViviendaIdOrderByFechaHoraDesc(viviendaId, PageRequest.of(0, limite));
    }

    @Transactional(readOnly = true)
    public long contarPorModulo(Long viviendaId, String modulo) {
        return registroAccesoRepository.countByViviendaIdAndModulo(viviendaId, modulo);
    }
}
