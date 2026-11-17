package com.senderos.consentimiento;

import com.senderos.service.ConsentimientoService;
import com.senderos.service.CapturaEmailService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.security.MessageDigest;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Consentimiento informado digital REG-2.0 (V17): trazabilidad completa.
 *
 * Verifica que el documento lo sirve el SERVIDOR (versión + texto + hash
 * SHA-256 del archivo consentimiento/REG-2.0.txt), que la aceptación
 * guarda usuario, IP, hash y fecha, que el rechazo queda registrado sin
 * impedir un consentimiento posterior, y que sin aceptación vigente no
 * se avanza a la encuesta (estado CONSENTIMIENTO + 422 al responder).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConsentimientoIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired CapturaEmailService capturaEmailService;
    @Autowired ConsentimientoService consentimientoService;

    record SesionAutenticada(MockHttpSession session, Cookie xsrf, String email) {}

    private Cookie xsrf() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        return csrf.getResponse().getCookie("XSRF-TOKEN");
    }

    /** Registro + verificación de correo + login, SIN consentir. */
    private SesionAutenticada registrarYEntrar(String etiqueta) throws Exception {
        String sufijo = Long.toHexString(System.nanoTime());
        String email = "consent-" + etiqueta + "-" + sufijo + "@senderos.local";
        Cookie token = xsrf();
        mockMvc.perform(post("/api/onboarding/registro")
                .cookie(token)
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType("application/json")
                .content("{\"nombre\":\"Familia Consentimiento\",\"email\":\"" + email
                    + "\",\"password\":\"clave-segura-123\",\"aceptaTratamientoDatos\":true," + com.senderos.support.CatalogoPruebas.campos() + "}"))
            .andExpect(status().isCreated());
        Cookie token2 = xsrf();
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                .cookie(token2)
                .header("X-XSRF-TOKEN", token2.getValue())
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"clave-segura-123\"}"))
            .andExpect(status().isOk())
            .andReturn();
        return new SesionAutenticada((MockHttpSession) login.getRequest().getSession(false), token2, email);
    }

    /** SHA-256 del archivo del documento (misma fuente que el servicio). */
    private String hashEsperado() throws Exception {
        byte[] contenido = getClass().getClassLoader()
            .getResourceAsStream("consentimiento/REG-2.0.txt").readAllBytes();
        return HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(contenido));
    }

    @Test
    void documentoVigenteTraeVersionTextoYHashCoherente() throws Exception {
        // Sin sesión: 401 (el documento pertenece al onboarding autenticado).
        mockMvc.perform(get("/api/onboarding/consentimiento/texto"))
            .andExpect(status().isUnauthorized());

        SesionAutenticada sesion = registrarYEntrar("texto");
        mockMvc.perform(get("/api/onboarding/consentimiento/texto").session(sesion.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value("REG-2.0"))
            .andExpect(jsonPath("$.texto").exists())
            .andExpect(jsonPath("$.hashTexto").value(hashEsperado()));

        // El texto entregado es exactamente el que produce el hash registrado.
        MvcResult r = mockMvc.perform(get("/api/onboarding/consentimiento/texto").session(sesion.session()))
            .andExpect(status().isOk()).andReturn();
        String cuerpo = r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        String texto = com.jayway.jsonpath.JsonPath.read(cuerpo, "$.texto");
        assertThat(com.senderos.security.Tokens.sha256(texto)).isEqualTo(hashEsperado());
    }

    @Test
    void aceptarGuardaTrazabilidadCompleta() throws Exception {
        SesionAutenticada sesion = registrarYEntrar("acepta");
        Long usuarioId = jdbcTemplate.queryForObject(
            "SELECT id FROM usuario WHERE email = ?", Long.class, sesion.email());
        Long viviendaId = jdbcTemplate.queryForObject(
            "SELECT vivienda_id FROM usuario WHERE email = ?", Long.class, sesion.email());

        mockMvc.perform(post("/api/onboarding/consentimiento")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue()))
            .andExpect(status().isOk());

        var fila = jdbcTemplate.queryForMap(
            "SELECT c.version_documento, c.aceptado, c.usuario_id, c.vivienda_id, c.ip_origen, "
                + "c.hash_texto, c.fecha_aceptacion FROM consentimiento c "
                + "WHERE c.usuario_id = ?", usuarioId);

        assertThat(fila.get("version_documento")).isEqualTo("REG-2.0");
        assertThat(((Number) fila.get("aceptado")).intValue()).isEqualTo(1);
        assertThat(((Number) fila.get("usuario_id")).longValue()).isEqualTo(usuarioId);
        assertThat(((Number) fila.get("vivienda_id")).longValue()).isEqualTo(viviendaId);
        assertThat((String) fila.get("ip_origen"))
            .as("la IP de origen queda registrada (MockMvc: 127.0.0.1)").isNotBlank();
        assertThat((String) fila.get("hash_texto")).isEqualTo(hashEsperado());
        assertThat(fila.get("fecha_aceptacion")).as("queda fecha y hora de aceptación").isNotNull();

        // La decisión queda además en la bitácora inmutable de eventos.
        Integer eventos = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM consentimiento_evento WHERE vivienda_id = ? "
                + "AND version_documento = 'REG-2.0' AND aceptado = 1 AND hash_texto = ?",
            Integer.class, viviendaId, hashEsperado());
        assertThat(eventos).as("la aceptación queda registrada en consentimiento_evento").isEqualTo(1);

        // El onboarding avanza a la encuesta y un segundo consentimiento choca.
        mockMvc.perform(get("/api/onboarding/estado").session(sesion.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.consentimientoAceptado").value(true))
            .andExpect(jsonPath("$.siguientePaso").value("ENCUESTA_INICIAL"));
        mockMvc.perform(post("/api/onboarding/consentimiento")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue()))
            .andExpect(status().isConflict());
    }

    @Test
    void rechazarRegistraTrazabilidadYNoHabilitaElAvance() throws Exception {
        SesionAutenticada sesion = registrarYEntrar("rechaza");
        Long usuarioId = jdbcTemplate.queryForObject(
            "SELECT id FROM usuario WHERE email = ?", Long.class, sesion.email());
        Long viviendaId = jdbcTemplate.queryForObject(
            "SELECT vivienda_id FROM usuario WHERE email = ?", Long.class, sesion.email());

        mockMvc.perform(post("/api/onboarding/consentimiento/rechazo")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue()))
            .andExpect(status().isNoContent());

        // El rechazo queda en la bitácora con trazabilidad completa.
        var evento = jdbcTemplate.queryForMap(
            "SELECT version_documento, aceptado, ip_origen, hash_texto, fecha_evento "
                + "FROM consentimiento_evento WHERE usuario_id = ?", usuarioId);
        assertThat(evento.get("version_documento")).isEqualTo("REG-2.0");
        assertThat(((Number) evento.get("aceptado")).intValue())
            .as("el rechazo queda registrado con aceptado=0").isZero();
        assertThat((String) evento.get("hash_texto")).isEqualTo(hashEsperado());
        assertThat(evento.get("fecha_evento")).isNotNull();
        // Y no existe aceptación vigente en el estado.
        Integer aceptadas = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM consentimiento WHERE vivienda_id = ? "
                + "AND aceptado = 1 AND version_documento = 'REG-2.0'", Integer.class, viviendaId);
        assertThat(aceptadas).isZero();

        // Sin aceptación vigente: el estado vuelve a pedir consentimiento y
        // la encuesta rechaza el envío (el cierre de sesión lo hace el frontend).
        mockMvc.perform(get("/api/onboarding/estado").session(sesion.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.consentimientoAceptado").value(false))
            .andExpect(jsonPath("$.siguientePaso").value("CONSENTIMIENTO"));
        mockMvc.perform(post("/api/onboarding/encuesta")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content("{\"encuestaId\":1,\"respuestas\":[{\"preguntaId\":1,\"opcionId\":1}]}"))
            .andExpect(status().isUnprocessableEntity());

        // Tras rechazar, el residente puede reconsiderar y aceptar.
        mockMvc.perform(post("/api/onboarding/consentimiento")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue()))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/onboarding/estado").session(sesion.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.siguientePaso").value("ENCUESTA_INICIAL"));
    }

    @Test
    void encuestaSinConsentimientoVigenteEsRechazada() throws Exception {
        SesionAutenticada sesion = registrarYEntrar("sinconsent");
        // Registro NO auto-consiente desde REG-2.0: sin fila aceptada.
        Integer filas = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM consentimiento c JOIN usuario u ON u.vivienda_id = c.vivienda_id "
                + "WHERE u.email = ? AND c.aceptado = 1", Integer.class, sesion.email());
        assertThat(filas).as("el registro no debe auto-aceptar el consentimiento").isZero();

        mockMvc.perform(get("/api/onboarding/estado").session(sesion.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.siguientePaso").value("CONSENTIMIENTO"));

        // La encuesta exige consentimiento vigente antes de responder.
        Long primeraPregunta = jdbcTemplate.queryForObject(
            "SELECT p.id FROM pregunta p JOIN encuesta e ON e.id = p.encuesta_id "
                + "WHERE e.tipo = 'INICIAL' ORDER BY p.orden LIMIT 1", Long.class);
        mockMvc.perform(post("/api/onboarding/encuesta")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content("{\"encuestaId\":1,\"respuestas\":[{\"preguntaId\":"
                    + primeraPregunta + ",\"opcionId\":1}]}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error").value("Debes aceptar el consentimiento antes de responder"));
    }
}
