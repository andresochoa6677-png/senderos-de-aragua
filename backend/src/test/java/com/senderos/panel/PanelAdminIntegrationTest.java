package com.senderos.panel;

import com.senderos.service.CapturaEmailService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Panel de administración (V19/V20):
 *  - El administrador ÚNICO es andresochoa224@unisangil.edu.co (creado por
 *    migración; la semilla admin@senderos.local ya no existe).
 *  - El admin inicia sesión y va directo al panel (no pasa por onboarding:
 *    sin vivienda, los endpoints de onboarding le responden 404).
 *  - Un residente no puede usar NINGÚN endpoint de administración (403).
 *  - La posprueba es de habilitación MANUAL: el admin la habilita y
 *    deshabilita con trazabilidad (409 si se repite el estado); sin
 *    habilitarla no está disponible para nadie.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PanelAdminIntegrationTest {

    private static final String ADMIN_EMAIL = com.senderos.support.AdminDePruebas.EMAIL;
    private static final String ADMIN_PASS = com.senderos.support.AdminDePruebas.CLAVE;

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired CapturaEmailService capturaEmailService;
    @Autowired com.senderos.security.ActiveSessionStore activeSessionStore;

    @BeforeEach
    void pospruebaSinHabilitar() {
        jdbcTemplate.update("UPDATE estudio_config SET posprueba_habilitada = FALSE, "
            + "posprueba_habilitada_en = NULL, posprueba_habilitada_por = NULL WHERE id = 1");
        com.senderos.support.AdminDePruebas.fijarClaveDePruebas(jdbcTemplate);
    }

    private Cookie xsrf() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        return csrf.getResponse().getCookie("XSRF-TOKEN");
    }

    private MockHttpSession login(String email, String password) throws Exception {
        Cookie token = xsrf();
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                .cookie(token)
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
            .andReturn();
        return (MockHttpSession) login.getRequest().getSession(false);
    }

    @Test
    void elAdminUnicoIniciaSesionYConsultaSuPanel() throws Exception {
        MockHttpSession sesion = login(ADMIN_EMAIL, ADMIN_PASS);
        assertThat(sesion).as("el admin migrado debe autenticar con su contraseña").isNotNull();

        mockMvc.perform(get("/api/auth/me").session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rol").value("ADMIN"));

        mockMvc.perform(get("/api/admin/estudio").session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pospruebaHabilitada").value(false))
            .andExpect(jsonPath("$.pospruebaHabilitadaEn").doesNotExist())
            .andExpect(jsonPath("$.viviendasRegistradas").isNumber())
            .andExpect(jsonPath("$.conConsentimiento").isNumber())
            .andExpect(jsonPath("$.conPreprueba").isNumber())
            .andExpect(jsonPath("$.conPostprueba").isNumber())
            .andExpect(jsonPath("$.comentarios").isNumber());

        mockMvc.perform(get("/api/admin/viviendas").session(sesion))
            .andExpect(status().isOk());
    }

    @Test
    void laSemillaAdminAnteriorYaNoExisteYElRolEsUnico() throws Exception {
        Integer semillas = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM usuario WHERE email = 'admin@senderos.local'", Integer.class);
        assertThat(semillas).as("admin@senderos.local fue eliminada por V19").isZero();

        Cookie token = xsrf();
        mockMvc.perform(post("/api/auth/login")
                .cookie(token)
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType("application/json")
                .content("{\"email\":\"admin@senderos.local\",\"password\":\"admin1234\"}"))
            .andExpect(status().isUnauthorized());

        Integer admins = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM usuario_rol ur JOIN rol r ON r.id = ur.rol_id WHERE r.nombre = 'ADMIN'",
            Integer.class);
        assertThat(admins).as("exactly ONE admin account exists").isEqualTo(1);
        Integer nuevoAdmin = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM usuario u JOIN usuario_rol ur ON ur.usuario_id = u.id "
                + "JOIN rol r ON r.id = ur.rol_id WHERE u.email = '" + ADMIN_EMAIL + "' AND r.nombre = 'ADMIN'",
            Integer.class);
        assertThat(nuevoAdmin).isEqualTo(1);
    }

    @Test
    void elAdminNoPasaPorOnboarding() throws Exception {
        MockHttpSession sesion = login(ADMIN_EMAIL, ADMIN_PASS);

        // Sin vivienda: los endpoints de onboarding no aplican al admin.
        mockMvc.perform(get("/api/onboarding/estado").session(sesion))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/onboarding/consentimiento/texto").session(sesion))
            .andExpect(status().isOk()); // el documento sí puede consultarse (informativo)
        Integer consentimientos = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM consentimiento c JOIN usuario u ON u.id = c.usuario_id "
                + "WHERE u.email = '" + ADMIN_EMAIL + "'", Integer.class);
        assertThat(consentimientos).as("el admin no registra consentimiento ni encuesta").isZero();
    }

    @Test
    void unResidenteNoPuedeUsarNingunEndpointDeAdministracion() throws Exception {
        // Residente registrado en el momento (no demo: no interferir con su sesión).
        String email = "residente-panel-" + Long.toHexString(System.nanoTime()) + "@senderos.local";
        Cookie tokenReg = xsrf();
        mockMvc.perform(post("/api/onboarding/registro")
                .cookie(tokenReg)
                .header("X-XSRF-TOKEN", tokenReg.getValue())
                .contentType("application/json")
                .content("{\"nombre\":\"Familia Panel\",\"email\":\"" + email
                    + "\",\"password\":\"clave-segura-123\",\"aceptaTratamientoDatos\":true," + com.senderos.support.CatalogoPruebas.campos() + "}"))
            .andExpect(status().isCreated());
        MockHttpSession residente = login(email, "clave-segura-123");
        assertThat(residente).isNotNull();

        mockMvc.perform(get("/api/admin/estudio").session(residente))
            .andExpect(status().isForbidden());
        Cookie tokenDemo = xsrf();
        mockMvc.perform(post("/api/admin/estudio/posprueba/habilitar")
                .session(residente)
                .cookie(tokenDemo)
                .header("X-XSRF-TOKEN", tokenDemo.getValue()))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/viviendas").session(residente))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/encuesta-final/estado").session(residente))
            .andExpect(status().isForbidden());

        // La tentativa del residente NO habilita la posprueba.
        Integer habilitadas = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM estudio_config WHERE posprueba_habilitada = TRUE", Integer.class);
        assertThat(habilitadas).isZero();
    }

    @Test
    void elContadorDeSesionesSoloCuentaLaSesionViva() throws Exception {
        // Login #1 y login #2 del MISMO usuario: maximumSessions(1) expira la
        // primera, y el contador del panel debe reflejar exactamente 1.
        Cookie t1 = xsrf();
        MvcResult login1 = mockMvc.perform(post("/api/auth/login")
                .cookie(t1)
                .header("X-XSRF-TOKEN", t1.getValue())
                .contentType("application/json")
                .content("{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"" + ADMIN_PASS + "\"}"))
            .andExpect(status().isOk())
            .andReturn();
        MockHttpSession sesion1 = (MockHttpSession) login1.getRequest().getSession(false);

        mockMvc.perform(get("/api/admin/estudio").session(sesion1))
            .andExpect(status().isOk());

        Cookie t2 = xsrf();
        MvcResult login2 = mockMvc.perform(post("/api/auth/login")
                .cookie(t2)
                .header("X-XSRF-TOKEN", t2.getValue())
                .contentType("application/json")
                .content("{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"" + ADMIN_PASS + "\"}"))
            .andExpect(status().isOk())
            .andReturn();
        MockHttpSession sesion2 = (MockHttpSession) login2.getRequest().getSession(false);

        // El panel ya NO cuenta la sesión expulsada (antes mostraba 2).
        mockMvc.perform(get("/api/admin/sesiones").session(sesion2))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.email == '" + ADMIN_EMAIL + "')].sesiones").value(1));

        // La sesión ANTIGUA quedó expirada: el filtro de sesión concurrente
        // la rechaza; la nueva sigue viva.
        mockMvc.perform(get("/api/auth/me").session(sesion1))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me").session(sesion2))
            .andExpect(status().isOk());
    }

    @Test
    void losAccesosDeCadaViviendaQuedanRegistrados() throws Exception {
        // Residente nuevo: registro → verificación → login → consulta de módulos.
        String email = "acceso-" + Long.toHexString(System.nanoTime()) + "@senderos.local";
        Cookie t = xsrf();
        mockMvc.perform(post("/api/onboarding/registro")
                .cookie(t)
                .header("X-XSRF-TOKEN", t.getValue())
                .contentType("application/json")
                .content("{\"nombre\":\"Familia Accesos\",\"email\":\"" + email
                    + "\",\"password\":\"clave-segura-123\",\"aceptaTratamientoDatos\":true," + com.senderos.support.CatalogoPruebas.campos() + "}"))
            .andExpect(status().isCreated());
        Cookie t2 = xsrf();
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                .cookie(t2)
                .header("X-XSRF-TOKEN", t2.getValue())
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"clave-segura-123\"}"))
            .andExpect(status().isOk())
            .andReturn();
        MockHttpSession sesion = (MockHttpSession) login.getRequest().getSession(false);
        mockMvc.perform(post("/api/onboarding/consentimiento")
                .session(sesion)
                .cookie(t2)
                .header("X-XSRF-TOKEN", t2.getValue()))
            .andExpect(status().isOk());

        // Consulta de módulos (GET exitosos deben quedar registrados).
        mockMvc.perform(get("/api/lecciones").session(sesion)).andExpect(status().isOk());
        mockMvc.perform(get("/api/practica/item?modo=CLASIFICA").session(sesion)).andExpect(status().isOk());

        // El login y las consultas quedan en registro_acceso.
        Integer sesionesRegistradas = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM registro_acceso ra JOIN usuario u ON u.id = ra.usuario_id "
                + "WHERE u.email = ? AND ra.modulo = 'SESION'", Integer.class, email);
        assertThat(sesionesRegistradas).as("el inicio de sesión queda registrado").isEqualTo(1);
        Integer modulos = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM registro_acceso ra JOIN usuario u ON u.id = ra.usuario_id "
                + "WHERE u.email = ? AND ra.modulo IN ('LECCIONES','PRACTICA')", Integer.class, email);
        assertThat(modulos).as("las consultas de módulos quedan registradas").isEqualTo(2);

        // El endpoint admin expone el registro completo de la vivienda.
        String codigo = jdbcTemplate.queryForObject(
            "SELECT v.codigo FROM vivienda v JOIN usuario u ON u.vivienda_id = v.id WHERE u.email = ?",
            String.class, email);
        MockHttpSession admin = login(ADMIN_EMAIL, ADMIN_PASS);
        mockMvc.perform(get("/api/admin/viviendas/" + codigo + "/registro").session(admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.codigo").value(codigo))
            .andExpect(jsonPath("$.sesionesIniciadas").value(1))
            .andExpect(jsonPath("$.modulosConsultados[?(@.modulo == 'LECCIONES')]").isNotEmpty());

        // Un residente NO puede ver el registro de otra vivienda.
        mockMvc.perform(get("/api/admin/viviendas/" + codigo + "/registro").session(sesion))
            .andExpect(status().isForbidden());
    }

    @Test
    void correoDePruebaSoloParaAdminYSeCaptura() throws Exception {
        // Residente: 403 (el envío de correos de prueba es del administrador).
        String emailRes = "mailtest-" + Long.toHexString(System.nanoTime()) + "@senderos.local";
        Cookie tReg = xsrf();
        mockMvc.perform(post("/api/onboarding/registro")
                .cookie(tReg)
                .header("X-XSRF-TOKEN", tReg.getValue())
                .contentType("application/json")
                .content("{\"nombre\":\"Familia Mail\",\"email\":\"" + emailRes
                    + "\",\"password\":\"clave-segura-123\",\"aceptaTratamientoDatos\":true," + com.senderos.support.CatalogoPruebas.campos() + "}"))
            .andExpect(status().isCreated());
        MockHttpSession residente = login(emailRes, "clave-segura-123");
        Cookie tokenResidente = xsrf();
        mockMvc.perform(post("/api/admin/mail/test")
                .session(residente)
                .cookie(tokenResidente)
                .header("X-XSRF-TOKEN", tokenResidente.getValue())
                .contentType("application/json")
                .content("{\"destino\":\"" + emailRes + "\"}"))
            .andExpect(status().isForbidden());

        // Admin: el correo de prueba se envía y queda capturado (modo captura en tests).
        MockHttpSession admin = login(ADMIN_EMAIL, ADMIN_PASS);
        String destino = "prueba-mail-" + Long.toHexString(System.nanoTime()) + "@senderos.local";
        Cookie tokenAdmin = xsrf();
        mockMvc.perform(post("/api/admin/mail/test")
                .session(admin)
                .cookie(tokenAdmin)
                .header("X-XSRF-TOKEN", tokenAdmin.getValue())
                .contentType("application/json")
                .content("{\"destino\":\"" + destino + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.modo").value("captura"))
            .andExpect(jsonPath("$.destino").value(destino));
        assertThat(capturaEmailService.capturado("prueba", destino))
            .as("el correo de prueba queda en el buzón de captura").isTrue();
    }

    @Test
    void exportacionesCsvEstructuraCorrectaYProtegidas() throws Exception {
        MockHttpSession admin = login(ADMIN_EMAIL, ADMIN_PASS);

        // Preprueba: cabecera con 18 respuestas + puntaje.
        mockMvc.perform(get("/api/admin/export/preprueba").session(admin))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition",
                org.hamcrest.Matchers.containsString("attachment; filename=preprueba-")))
            .andExpect(content().contentTypeCompatibleWith("text/csv"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("vivienda_codigo")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("puntaje_conocimiento_0a10")));
        // Postprueba, uso y comentarios: cabeceras propias.
        mockMvc.perform(get("/api/admin/export/postprueba").session(admin))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("respuesta_22")));
        mockMvc.perform(get("/api/admin/export/uso").session(admin))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("sesiones_iniciadas")));
        mockMvc.perform(get("/api/admin/export/comentarios").session(admin))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("categoria_tematica")));

        // Residente: 403 en las cuatro exportaciones.
        String email = "export-" + Long.toHexString(System.nanoTime()) + "@senderos.local";
        Cookie t = xsrf();
        mockMvc.perform(post("/api/onboarding/registro")
                .cookie(t)
                .header("X-XSRF-TOKEN", t.getValue())
                .contentType("application/json")
                .content("{\"nombre\":\"Familia Export\",\"email\":\"" + email
                    + "\",\"password\":\"clave-segura-123\",\"aceptaTratamientoDatos\":true," + com.senderos.support.CatalogoPruebas.campos() + "}"))
            .andExpect(status().isCreated());
        MockHttpSession residente = login(email, "clave-segura-123");
        for (String tipo : List.of("preprueba", "postprueba", "uso", "comentarios")) {
            mockMvc.perform(get("/api/admin/export/" + tipo).session(residente))
                .andExpect(status().isForbidden());
        }
    }

    @Test
    void categoriaTematicaSeAsignaSoloDesdeAdmin() throws Exception {
        // Residente crea un comentario.
        String email = "cat-" + Long.toHexString(System.nanoTime()) + "@senderos.local";
        Cookie t = xsrf();
        mockMvc.perform(post("/api/onboarding/registro")
                .cookie(t)
                .header("X-XSRF-TOKEN", t.getValue())
                .contentType("application/json")
                .content("{\"nombre\":\"Familia Categoria\",\"email\":\"" + email
                    + "\",\"password\":\"clave-segura-123\",\"aceptaTratamientoDatos\":true," + com.senderos.support.CatalogoPruebas.campos() + "}"))
            .andExpect(status().isCreated());
        MockHttpSession residente = login(email, "clave-segura-123");
        mockMvc.perform(post("/api/onboarding/consentimiento")
                .session(residente)
                .cookie(t)
                .header("X-XSRF-TOKEN", t.getValue()))
            .andExpect(status().isOk());
        Cookie tokenComentario = xsrf();
        mockMvc.perform(post("/api/comentarios")
                .session(residente)
                .cookie(tokenComentario)
                .header("X-XSRF-TOKEN", tokenComentario.getValue())
                .contentType("application/json")
                .content("{\"tipo\":\"SUGERENCIA\",\"temaDeclarado\":\"SEPARACION\",\"contenido\":\"Comentario para categorizar\"}"))
            .andExpect(status().isCreated());
        Long comentarioId = jdbcTemplate.queryForObject(
            "SELECT c.id FROM comentario c JOIN usuario u ON u.id = c.usuario_id WHERE u.email = ?",
            Long.class, email);

        // El residente NO puede categorizar.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .patch("/api/admin/comentarios/" + comentarioId + "/categoria")
                .session(residente)
                .cookie(t)
                .header("X-XSRF-TOKEN", t.getValue())
                .contentType("application/json")
                .content("{\"categoriaTematica\":\"otro\"}"))
            .andExpect(status().isForbidden());

        // Admin: asigna una categoría de la whitelist → queda en BD.
        MockHttpSession admin = login(ADMIN_EMAIL, ADMIN_PASS);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .patch("/api/admin/comentarios/" + comentarioId + "/categoria")
                .session(admin)
                .cookie(t)
                .header("X-XSRF-TOKEN", t.getValue())
                .contentType("application/json")
                .content("{\"categoriaTematica\":\"sugerencias-mejora\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.categoriaTematica").value("sugerencias-mejora"));
        String enBd = jdbcTemplate.queryForObject(
            "SELECT categoria_tematica FROM comentario WHERE id = ?", String.class, comentarioId);
        assertThat(enBd).isEqualTo("sugerencias-mejora");

        // Valor fuera de la whitelist → 422.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .patch("/api/admin/comentarios/" + comentarioId + "/categoria")
                .session(admin)
                .cookie(t)
                .header("X-XSRF-TOKEN", t.getValue())
                .contentType("application/json")
                .content("{\"categoriaTematica\":\"categoria-inventada\"}"))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void habilitarYDeshabilitarLaPospruebaConTrazabilidad() throws Exception {
        MockHttpSession sesion = login(ADMIN_EMAIL, ADMIN_PASS);

        Cookie tokenHabilitar = xsrf();
        mockMvc.perform(post("/api/admin/estudio/posprueba/habilitar")
                .session(sesion)
                .cookie(tokenHabilitar)
                .header("X-XSRF-TOKEN", tokenHabilitar.getValue()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mensaje").value("Encuesta final habilitada"));

        mockMvc.perform(get("/api/admin/estudio").session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pospruebaHabilitada").value(true))
            .andExpect(jsonPath("$.pospruebaHabilitadaEn").exists())
            .andExpect(jsonPath("$.viviendasRegistradas").isNumber());

        Integer habilitada = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM estudio_config WHERE posprueba_habilitada = TRUE "
                + "AND posprueba_habilitada_por = '" + ADMIN_EMAIL + "' "
                + "AND posprueba_habilitada_en IS NOT NULL", Integer.class);
        assertThat(habilitada).as("la habilitación quedó trazada con fecha y responsable").isEqualTo(1);

        Cookie tokenReintento = xsrf();
        mockMvc.perform(post("/api/admin/estudio/posprueba/habilitar")
                .session(sesion)
                .cookie(tokenReintento)
                .header("X-XSRF-TOKEN", tokenReintento.getValue()))
            .andExpect(status().isConflict());

        Cookie tokenDeshabilitar = xsrf();
        mockMvc.perform(post("/api/admin/estudio/posprueba/deshabilitar")
                .session(sesion)
                .cookie(tokenDeshabilitar)
                .header("X-XSRF-TOKEN", tokenDeshabilitar.getValue()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mensaje").value("Encuesta final deshabilitada"));

        mockMvc.perform(get("/api/admin/estudio").session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pospruebaHabilitada").value(false));

        Cookie tokenRe = xsrf();
        mockMvc.perform(post("/api/admin/estudio/posprueba/deshabilitar")
                .session(sesion)
                .cookie(tokenRe)
                .header("X-XSRF-TOKEN", tokenRe.getValue()))
            .andExpect(status().isConflict());
    }
}
