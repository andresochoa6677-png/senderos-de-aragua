package com.senderos.service;

import com.senderos.entity.Consentimiento;
import com.senderos.entity.ConsentimientoEvento;
import com.senderos.entity.Usuario;
import com.senderos.entity.Vivienda;
import com.senderos.exception.ConflictException;
import com.senderos.repository.ConsentimientoEventoRepository;
import com.senderos.repository.ConsentimientoRepository;
import com.senderos.repository.UsuarioRepository;
import com.senderos.repository.ViviendaRepository;
import com.senderos.security.Tokens;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Consentimiento informado digital REG-2.0 (Anexo C).
 *
 * El texto vive en un archivo estático del backend
 * (consentimiento/REG-2.0.txt) y es la ÚNICA fuente de verdad: la
 * versión, el texto y su hash SHA-256 los fija el SERVIDOR, jamás el
 * cliente. Cada decisión (aceptar o rechazar) se registra con
 * trazabilidad completa en `consentimiento_evento` (usuario, vivienda,
 * versión, IP, hash, fecha) y la aceptación actualiza además el estado
 * vigente en `consentimiento`. La puerta del onboarding exige aceptación
 * de la VERSIÓN VIGENTE: una fila legacy REG-1.0 vuelve a pedir el
 * consentimiento actualizado. Un rechazo no impide reconsiderar después.
 */
@Service
public class ConsentimientoService {

    public static final String VERSION_ACTUAL = "REG-2.0";

    private final ConsentimientoRepository consentimientoRepository;
    private final ConsentimientoEventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ViviendaRepository viviendaRepository;
    private final String texto;
    private final String hashTexto;

    public ConsentimientoService(ConsentimientoRepository consentimientoRepository,
                                 ConsentimientoEventoRepository eventoRepository,
                                 UsuarioRepository usuarioRepository,
                                 ViviendaRepository viviendaRepository) {
        this.consentimientoRepository = consentimientoRepository;
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
        this.viviendaRepository = viviendaRepository;
        this.texto = cargarTexto();
        this.hashTexto = Tokens.sha256(texto);
    }

    private static String cargarTexto() {
        try (InputStream recurso = new ClassPathResource("consentimiento/REG-2.0.txt").getInputStream()) {
            return new String(recurso.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer consentimiento/REG-2.0.txt", e);
        }
    }

    /** Versión, texto íntegro y hash del documento vigente (para la pantalla). */
    public com.senderos.dto.ConsentimientoTextoDTO obtenerTexto() {
        return new com.senderos.dto.ConsentimientoTextoDTO(VERSION_ACTUAL, texto, hashTexto);
    }

    /** Acepta el documento vigente: 409 si ya está aceptada esa versión. */
    @Transactional
    public void aceptar(Long viviendaId, String email, String ipOrigen) {
        if (tieneConsentimientoVigente(viviendaId)) {
            throw new ConflictException("El consentimiento ya fue registrado");
        }
        Usuario usuario = usuarioRepository.findByEmail(email.trim().toLowerCase())
            .orElseThrow(() -> new ConflictException("Usuario no encontrado"));

        // Estado vigente: upsert (cada nueva versión actualiza la fila).
        Consentimiento estado = consentimientoRepository.findFirstByViviendaId(viviendaId)
            .orElseGet(() -> {
                Consentimiento nuevo = new Consentimiento();
                nuevo.setVivienda(viviendaRepository.getReferenceById(viviendaId));
                return nuevo;
            });
        estado.setUsuario(usuario);
        estado.setVersionDocumento(VERSION_ACTUAL);
        estado.setIpOrigen(ipOrigen);
        estado.setHashTexto(hashTexto);
        estado.setAceptado();
        consentimientoRepository.save(estado);

        registrarEvento(viviendaId, usuario, ipOrigen, true);
    }

    /** Registra el RECHAZO (bitácora) sin impedir un consentimiento futuro. */
    @Transactional
    public void rechazar(Long viviendaId, String email, String ipOrigen) {
        Usuario usuario = usuarioRepository.findByEmail(email.trim().toLowerCase())
            .orElseThrow(() -> new ConflictException("Usuario no encontrado"));
        registrarEvento(viviendaId, usuario, ipOrigen, false);
    }

    private void registrarEvento(Long viviendaId, Usuario usuario, String ipOrigen, boolean aceptado) {
        ConsentimientoEvento evento = new ConsentimientoEvento();
        evento.setVivienda(viviendaRepository.getReferenceById(viviendaId));
        evento.setUsuario(usuario);
        evento.setVersionDocumento(VERSION_ACTUAL);
        evento.setAceptado(aceptado);
        evento.setIpOrigen(ipOrigen);
        evento.setHashTexto(hashTexto);
        eventoRepository.save(evento);
    }

    /** ¿La vivienda ya aceptó la versión vigente del documento? */
    @Transactional(readOnly = true)
    public boolean tieneConsentimientoVigente(Long viviendaId) {
        return consentimientoRepository
            .existsByViviendaIdAndAceptadoTrueAndVersionDocumento(viviendaId, VERSION_ACTUAL);
    }

    /** Bitácora completa de decisiones de una vivienda (auditoría). */
    @Transactional(readOnly = true)
    public List<ConsentimientoEvento> historial(Long viviendaId) {
        return eventoRepository.findByViviendaIdOrderByFechaEventoDesc(viviendaId);
    }
}
