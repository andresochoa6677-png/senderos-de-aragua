package com.senderos.service;

import com.senderos.dto.ComentarioAdminDTO;
import com.senderos.dto.ComentarioRequest;
import com.senderos.dto.ComentarioResponse;
import com.senderos.entity.Comentario;
import com.senderos.entity.Usuario;
import com.senderos.exception.BusinessException;
import com.senderos.repository.ComentarioRepository;
import com.senderos.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Participación del residente: aportes privados (inquietudes,
 * sugerencias, percepciones y situaciones) asociados automáticamente a
 * la vivienda de la sesión autenticada. Datos de investigación: se
 * conservan tal como fueron registrados; la categorización temática es
 * posterior y manual. Nunca se exponen aportes de otras viviendas.
 */
@Service
public class ComentarioService {

    public static final List<String> TIPOS_VALIDOS =
        List.of("INQUIETUD", "SUGERENCIA", "PERCEPCION", "SITUACION");
    public static final List<String> TEMAS_VALIDOS =
        List.of("SEPARACION", "PUNTO_RESIDUOS", "HORARIOS_RECOLECCION",
            "INFORMACION_PLATAFORMA", "SITUACION_CONDOMINIO", "OTRO");

    /** Categorización temática posterior del equipo investigador (panel admin). */
    public static final List<String> CATEGORIAS_TEMATICAS =
        List.of("inquietudes-horarios", "sugerencias-mejora", "reconocimiento-informacion",
            "reportes-situacion", "otro");

    private final ComentarioRepository comentarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;

    public ComentarioService(ComentarioRepository comentarioRepository,
                             UsuarioRepository usuarioRepository,
                             UsuarioService usuarioService) {
        this.comentarioRepository = comentarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
    }

    /** Registra el aporte asociándolo a la vivienda de la sesión autenticada. */
    @Transactional
    public ComentarioResponse crear(String email, ComentarioRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(email.trim().toLowerCase())
            .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        if (usuario.getVivienda() == null) {
            throw new BusinessException("Tu cuenta no tiene una vivienda asociada");
        }

        String tipo = request.tipo() == null ? "" : request.tipo().trim();
        if (!TIPOS_VALIDOS.contains(tipo)) {
            throw new BusinessException("Tipo de participación inválido");
        }
        String contenido = request.contenido().trim();
        if (contenido.isEmpty()) {
            throw new BusinessException("El comentario no puede estar vacío");
        }

        String tema = request.temaDeclarado() == null || request.temaDeclarado().isBlank()
            ? null
            : request.temaDeclarado().trim();
        if (tema != null && !TEMAS_VALIDOS.contains(tema)) {
            throw new BusinessException("Tema declarado inválido");
        }

        Comentario guardado = comentarioRepository.save(new Comentario(
            usuario, usuario.getVivienda(), tipo, tema, contenido));

        return new ComentarioResponse(
            guardado.getId(), guardado.getTipo(), guardado.getTemaDeclarado(),
            guardado.getContenido(), guardado.getCreadoEn());
    }

    /** Aportes de la propia vivienda, del más reciente al más antiguo. */
    @Transactional(readOnly = true)
    public List<ComentarioResponse> misAportes(String email) {
        Long viviendaId = usuarioService.buscarViviendaIdPorEmail(email);
        return comentarioRepository.findByViviendaIdOrderByCreadoEnDescIdDesc(viviendaId).stream()
            .map(c -> new ComentarioResponse(
                c.getId(), c.getTipo(), c.getTemaDeclarado(), c.getContenido(), c.getCreadoEn()))
            .toList();
    }

    /**
     * Asigna la categoría temática de un comentario (solo ADMIN). La
     * categorización es POSTERIOR y manual: no toca la creación. null o
     * vacío limpia la categoría.
     */
    @Transactional
    public com.senderos.dto.ComentarioAdminDTO actualizarCategoria(Long id, String categoria) {
        String valor = (categoria == null || categoria.isBlank()) ? null : categoria.trim();
        if (valor != null && !CATEGORIAS_TEMATICAS.contains(valor)) {
            throw new BusinessException("Categoría temática inválida");
        }
        Comentario comentario = comentarioRepository.findById(id)
            .orElseThrow(() -> new com.senderos.exception.NotFoundException("Comentario no encontrado"));
        comentario.setCategoriaTematica(valor);
        Comentario guardado = comentarioRepository.save(comentario);
        return new com.senderos.dto.ComentarioAdminDTO(
            guardado.getId(), guardado.getCreadoEn(),
            guardado.getVivienda() != null ? guardado.getVivienda().getCodigo() : null,
            guardado.getTipo(), guardado.getTemaDeclarado(), guardado.getContenido(),
            guardado.getCategoriaTematica());
    }

    /** Historial completo para el equipo investigador (solo rol ADMIN). */
    @Transactional(readOnly = true)
    public List<ComentarioAdminDTO> historialInvestigacion() {
        return comentarioRepository.findAllByOrderByCreadoEnDescIdDesc().stream()
            .map(c -> new ComentarioAdminDTO(
                c.getId(), c.getCreadoEn(),
                c.getVivienda() != null ? c.getVivienda().getCodigo() : null,
                c.getTipo(), c.getTemaDeclarado(), c.getContenido(),
                c.getCategoriaTematica()))
            .toList();
    }
}
