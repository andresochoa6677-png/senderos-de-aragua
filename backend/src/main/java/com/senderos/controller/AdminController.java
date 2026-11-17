package com.senderos.controller;

import com.senderos.dto.ComentarioAdminDTO;
import com.senderos.entity.Usuario;
import com.senderos.repository.UsuarioRepository;
import com.senderos.security.ActiveSessionStore;
import com.senderos.service.ComentarioService;
import com.senderos.service.ExportacionService;
import com.senderos.service.EmailService;
import com.senderos.service.EncuestaFinalService;
import com.senderos.service.PanelAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Endpoints de administracion. Protegidos por hasRole(ADMIN) en SecurityConfig
 * y reforzados con @PreAuthorize. El historial de comentarios está destinado
 * al equipo investigador (análisis cualitativo posterior).
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ActiveSessionStore activeSessionStore;
    private final UsuarioRepository usuarioRepository;
    private final ComentarioService comentarioService;
    private final EncuestaFinalService encuestaFinalService;
    private final PanelAdminService panelAdminService;
    private final EmailService emailService;
    private final ExportacionService exportacionService;
    private final String mailModo;

    public AdminController(ActiveSessionStore activeSessionStore, UsuarioRepository usuarioRepository,
                           ComentarioService comentarioService, EncuestaFinalService encuestaFinalService,
                           PanelAdminService panelAdminService, EmailService emailService,
                           ExportacionService exportacionService,
                           @Value("${app.mail.modo:captura}") String mailModo) {
        this.activeSessionStore = activeSessionStore;
        this.usuarioRepository = usuarioRepository;
        this.comentarioService = comentarioService;
        this.encuestaFinalService = encuestaFinalService;
        this.panelAdminService = panelAdminService;
        this.emailService = emailService;
        this.exportacionService = exportacionService;
        this.mailModo = mailModo;
    }

    /** Sesiones activas reales por usuario; excluir cuentas DEMO. */
    @GetMapping("/sesiones")
    public ResponseEntity<List<Map<String, Object>>> sesionesActivas() {
        return ResponseEntity.ok(usuarioRepository.findAll().stream()
            .filter(u -> !u.isDemo())
            .map(u -> Map.<String, Object>of(
                "email", u.getEmail(),
                "sesiones", activeSessionStore.cantidadSesiones(u.getEmail())))
            .toList());
    }

    /**
     * Historial de participación del proyecto para el análisis cualitativo:
     * fecha | vivienda | tipo | contenido | tema declarado. Solo equipo
     * autorizado; los residentes jamás pueden consultar aportes ajenos.
     */
    @GetMapping("/comentarios")
    public ResponseEntity<List<ComentarioAdminDTO>> comentarios() {
        return ResponseEntity.ok(comentarioService.historialInvestigacion());
    }

    /**
     * Estado de la posprueba por vivienda: código, fecha de preprueba,
     * ¿disponible?, ¿respondió la FINAL? y puntajes de conocimiento
     * pre vs. post (escala 0-10). Para el seguimiento del estudio.
     */
    @GetMapping("/encuesta-final/estado")
    public ResponseEntity<List<Map<String, Object>>> encuestaFinalEstado() {
        return ResponseEntity.ok(encuestaFinalService.estadoGeneral());
    }

    /**
     * Estado del estudio para el panel: estado de la posprueba
     * (habilitación manual con trazabilidad) y contadores de participación.
     */
    @GetMapping("/estudio")
    public ResponseEntity<com.senderos.dto.PanelEstudioDTO> estadoEstudio() {
        return ResponseEntity.ok(panelAdminService.estadoPanel());
    }

    /** Habilita la encuesta final de inmediato (409 si ya lo está). */
    @PostMapping("/estudio/posprueba/habilitar")
    public ResponseEntity<Map<String, Object>> habilitarPosprueba(
            @AuthenticationPrincipal UserDetails principal) {
        Instant habilitadaEn = panelAdminService.habilitarPosprueba(principal.getUsername());
        return ResponseEntity.ok(Map.of(
            "mensaje", "Encuesta final habilitada",
            "habilitadaEn", habilitadaEn.toString()));
    }

    /** Deshabilita la encuesta final (409 si ya lo está). */
    @PostMapping("/estudio/posprueba/deshabilitar")
    public ResponseEntity<Map<String, Object>> deshabilitarPosprueba() {
        panelAdminService.deshabilitarPosprueba();
        return ResponseEntity.ok(Map.of("mensaje", "Encuesta final deshabilitada"));
    }

    /** Viviendas con su estado de participación (registro, consentimiento, pre y post). */
    @GetMapping("/viviendas")
    public ResponseEntity<List<Map<String, Object>>> viviendas() {
        return ResponseEntity.ok(panelAdminService.viviendasPanel());
    }

    /**
     * Envía un correo de PRUEBA al destino indicado para verificar el canal
     * real (Brevo SMTP) sin registrar usuarios. Responde el modo activo
     * (smtp = envío real; captura = buzón local de desarrollo).
     */
    @PostMapping("/mail/test")
    public ResponseEntity<Map<String, Object>> correoPrueba(
            @Valid @RequestBody MailTestRequest request) {
        emailService.enviarCorreoPrueba(request.destino());
        return ResponseEntity.ok(Map.of(
            "modo", mailModo,
            "destino", request.destino(),
            "mensaje", "Correo de prueba enviado. Revisa la bandeja (y spam) del destino."));
    }

    public record MailTestRequest(@NotBlank @Email String destino) {}

    public record CategoriaRequest(String categoriaTematica) {}

    private ResponseEntity<String> csv(String cuerpo, String nombre) {
        String fecha = java.time.LocalDate.now().toString();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nombre + "-" + fecha + ".csv")
            .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
            .body(cuerpo);
    }

    @GetMapping("/export/preprueba")
    public ResponseEntity<String> exportPreprueba() {
        return csv(exportacionService.csvPreprueba(), "preprueba");
    }

    @GetMapping("/export/postprueba")
    public ResponseEntity<String> exportPostprueba() {
        return csv(exportacionService.csvPostprueba(), "postprueba");
    }

    @GetMapping("/export/uso")
    public ResponseEntity<String> exportUso() {
        return csv(exportacionService.csvUso(), "uso");
    }

    @GetMapping("/export/comentarios")
    public ResponseEntity<String> exportComentarios() {
        return csv(exportacionService.csvComentarios(), "comentarios");
    }

    /** Categorización temática de un comentario (posterior y manual; solo ADMIN). */
    @PatchMapping("/comentarios/{id}/categoria")
    public ResponseEntity<ComentarioAdminDTO> categoria(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.ok(comentarioService.actualizarCategoria(id, request.categoriaTematica()));
    }

    /** Registro COMPLETO de una vivienda: accesos, módulos, lecciones con puntaje, práctica y comentarios. */
    @GetMapping("/viviendas/{codigo}/registro")
    public ResponseEntity<Map<String, Object>> registroVivienda(@PathVariable String codigo) {
        return ResponseEntity.ok(panelAdminService.registroDeVivienda(codigo));
    }
}
