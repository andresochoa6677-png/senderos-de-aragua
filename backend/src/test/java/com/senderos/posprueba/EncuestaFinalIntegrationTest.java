package com.senderos.posprueba;

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
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Encuesta FINAL (posprueba) con habilitación MANUAL del administrador:
 * deshabilitada por defecto; sin cronómetro ni ventana de días. Al
 * habilitarla queda disponible de inmediato y puede volver a
 * deshabilitarse. Cada cambio de estado queda trazado en estudio_config
 * (fecha/hora y responsable).
 *
 * La posprueba nueva (V24) trae 22 ítems: 10 de conocimiento con
 * incisos REORGANIZADOS respecto a la preprueba (la clave correcta no
 * ocupa la misma posición), 4 de práctica y 8 de percepción (4 de
 * acuerdo + 4 sobre la plataforma, con la 19 de frecuencia 1-5).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EncuestaFinalIntegrationTest {

    private static final String ADMIN_EMAIL = com.senderos.support.AdminDePruebas.EMAIL;
    private static final String ADMIN_PASS = com.senderos.support.AdminDePruebas.CLAVE;

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired CapturaEmailService capturaEmailService;

    record SesionAutenticada(MockHttpSession session, Cookie xsrf, String email, Long viviendaId) {}

    @BeforeEach
    void limpiarEstadoDelEstudio() {
        // La habilitación es MANUAL: cada test decide el estado y el
        // cliente HTTP no lo hereda.
        jdbcTemplate.update("DELETE FROM respuesta");
        jdbcTemplate.update(
            "UPDATE estudio_config SET posprueba_habilitada = FALSE, "
                + "posprueba_habilitada_en = NULL, posprueba_habilitada_por = NULL WHERE id = 1");
    }

    private Cookie xsrf() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        return csrf.getResponse().getCookie("XSRF-TOKEN");
    }

    private String sufijo() {
        return Long.toHexString(System.nanoTime());
    }

    private SesionAutenticada registrarYEntrar(String etiqueta) throws Exception {
        String email = "posprueba-" + etiqueta + "-" + sufijo() + "@senderos.local";
        Cookie token = xsrf();
        mockMvc.perform(post("/api/onboarding/registro")
                .cookie(token)
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType("application/json")
                .content("{\"nombre\":\"Familia Posprueba\",\"email\":\"" + email
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
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        // Consentimiento REG-2.0 (la posprueba lo hereda: no vuelve a pedirse)
        mockMvc.perform(post("/api/onboarding/consentimiento")
                .session(session)
                .cookie(token2)
                .header("X-XSRF-TOKEN", token2.getValue()))
            .andExpect(status().isOk());
        Long viviendaId = jdbcTemplate.queryForObject(
            "SELECT vivienda_id FROM usuario WHERE email = ?", Long.class, email);
        return new SesionAutenticada(session, token2, email, viviendaId);
    }

    /** Responde la preprueba completa (claves correctas). */
    private void responderPreprueba(SesionAutenticada sesion) throws Exception {
        String cuerpo = mockMvc.perform(get("/api/onboarding/encuesta/INICIAL").session(sesion.session()))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        StringBuilder items = new StringBuilder();
        List<?> preguntas = com.jayway.jsonpath.JsonPath.read(cuerpo, "$.preguntas[*].id");
        for (int i = 0; i < preguntas.size(); i++) {
            long preguntaId = ((Number) preguntas.get(i)).longValue();
            String tipoPregunta = jdbcTemplate.queryForObject(
                "SELECT tipo_pregunta FROM pregunta WHERE id = ?", String.class, preguntaId);
            String opcionSql = tipoPregunta.equals("CONOCIMIENTO")
                ? "SELECT id FROM opcion WHERE pregunta_id = " + preguntaId + " AND es_correcta = 1 ORDER BY orden LIMIT 1"
                : "SELECT id FROM opcion WHERE pregunta_id = " + preguntaId + " ORDER BY orden LIMIT 1";
            Long opcionId = jdbcTemplate.queryForObject(opcionSql, Long.class);
            if (i > 0) items.append(',');
            items.append("{\"preguntaId\":").append(preguntaId)
                 .append(",\"opcionId\":").append(opcionId).append('}');
        }
        mockMvc.perform(post("/api/onboarding/encuesta")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content("{\"encuestaId\":1,\"respuestas\":[" + items + "]}"))
            .andExpect(status().isOk());
    }

    /** Envío completo de la FINAL: 22 ítems (claves correctas en conocimiento). */
    private String cuerpoEnvioFinal() {
        List<Long> ids = jdbcTemplate.queryForList(
            "SELECT p.id FROM pregunta p JOIN encuesta e ON e.id = p.encuesta_id "
                + "WHERE e.tipo = 'FINAL' ORDER BY p.orden", Long.class);
        StringBuilder items = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            Long preguntaId = ids.get(i);
            String tipoPregunta = jdbcTemplate.queryForObject(
                "SELECT tipo_pregunta FROM pregunta WHERE id = ?", String.class, preguntaId);
            String opcionSql = tipoPregunta.equals("CONOCIMIENTO")
                ? "SELECT id FROM opcion WHERE pregunta_id = " + preguntaId + " AND es_correcta = 1 ORDER BY orden LIMIT 1"
                : "SELECT id FROM opcion WHERE pregunta_id = " + preguntaId + " ORDER BY orden LIMIT 1";
            Long opcionId = jdbcTemplate.queryForObject(opcionSql, Long.class);
            if (i > 0) items.append(',');
            items.append("{\"preguntaId\":").append(preguntaId)
                 .append(",\"opcionId\":").append(opcionId).append('}');
        }
        return "{\"respuestas\":[" + items + "]}";
    }

    private MockHttpSession sesionAdmin() throws Exception {
        com.senderos.support.AdminDePruebas.fijarClaveDePruebas(jdbcTemplate);
        Cookie token = xsrf();
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                .cookie(token)
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType("application/json")
                .content("{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"" + ADMIN_PASS + "\"}"))
            .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) login.getRequest().getSession(false);
    }

    @Test
    void sinHabilitarLaFinalNoEstaDisponibleParaNadie() throws Exception {
        // Vivienda con preprueba respondida, pero la posprueba NO habilitada:
        // disponible=false y acceso 403 a ítems y envío, sin fechas ni contador.
        SesionAutenticada sesion = registrarYEntrar("sinhabilitar");
        responderPreprueba(sesion);

        mockMvc.perform(get("/api/encuesta-final/disponible").session(sesion.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.disponible").value(false))
            .andExpect(jsonPath("$.yaRespondida").value(false));

        mockMvc.perform(get("/api/encuesta-final").session(sesion.session()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error")
                .value("La encuesta final aún no está disponible. Te avisaremos cuando se habilite"));
        mockMvc.perform(post("/api/encuesta-final")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content(cuerpoEnvioFinal()))
            .andExpect(status().isForbidden());
    }

    @Test
    void sinPrepruebaResponde404() throws Exception {
        SesionAutenticada sesion = registrarYEntrar("sinpre");
        mockMvc.perform(get("/api/encuesta-final/disponible").session(sesion.session()))
            .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/encuesta-final")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content(cuerpoEnvioFinal()))
            .andExpect(status().isNotFound());
    }

    @Test
    void habilitadaSirve22ItemsClavesAlternadasYBloqueaDobleEnvio() throws Exception {
        SesionAutenticada sesion = registrarYEntrar("flujo");
        responderPreprueba(sesion);

        // El admin habilita: disponible de inmediato.
        MockHttpSession admin = sesionAdmin();
        Cookie tokenHab = xsrf();
        mockMvc.perform(post("/api/admin/estudio/posprueba/habilitar")
                .session(admin)
                .cookie(tokenHab)
                .header("X-XSRF-TOKEN", tokenHab.getValue()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mensaje").value("Encuesta final habilitada"));

        mockMvc.perform(get("/api/encuesta-final/disponible").session(sesion.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.disponible").value(true));

        // 22 ítems: 10 conocimiento, 4 práctica, 8 percepción (4 acuerdo
        // + 4 plataforma), sin claves ni valores internos expuestos.
        mockMvc.perform(get("/api/encuesta-final").session(sesion.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tipo").value("FINAL"))
            .andExpect(jsonPath("$.preguntas", hasSize(22)))
            .andExpect(jsonPath("$.preguntas[?(@.tipoPregunta=='CONOCIMIENTO')]", hasSize(10)))
            .andExpect(jsonPath("$.preguntas[?(@.tipoPregunta=='PRACTICA')]", hasSize(4)))
            .andExpect(jsonPath("$.preguntas[?(@.tipoPregunta=='PERCEPCION')]", hasSize(8)));

        String cuerpo = mockMvc.perform(get("/api/encuesta-final").session(sesion.session()))
            .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(cuerpo)
            .as("la clave de corrección no debe exponerse al cliente")
            .doesNotContain("esCorrecta").doesNotContain("es_correcta")
            .as("el valor interno de las opciones no debe exponerse")
            .doesNotContain("\"valor\"");

        // Anti-memoria: la clave de la pregunta 1 (caneca VERDE) ocupa la
        // posición 3 en la preprueba y la 2 en la posprueba.
        Integer posicionPre = jdbcTemplate.queryForObject(
            "SELECT MIN(o.orden) FROM opcion o JOIN pregunta p ON p.id = o.pregunta_id "
                + "JOIN encuesta e ON e.id = p.encuesta_id "
                + "WHERE e.tipo = 'INICIAL' AND p.orden = 1 AND o.es_correcta = 1", Integer.class);
        Integer posicionPost = jdbcTemplate.queryForObject(
            "SELECT MIN(o.orden) FROM opcion o JOIN pregunta p ON p.id = o.pregunta_id "
                + "JOIN encuesta e ON e.id = p.encuesta_id "
                + "WHERE e.tipo = 'FINAL' AND p.orden = 1 AND o.es_correcta = 1", Integer.class);
        assertThat(posicionPre).as("clave preprueba en el inciso C").isEqualTo(3);
        assertThat(posicionPost).as("clave posprueba en el inciso B (alternada)").isEqualTo(2);

        // Envío completo con todas las claves correctas: puntaje 10/10.
        mockMvc.perform(post("/api/encuesta-final")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content(cuerpoEnvioFinal()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.puntajeConocimiento").value("10"));

        // Las 22 respuestas quedaron guardadas con tipo = FINAL, incluida
        // la Parte 4 de la plataforma (19-22) con valores 1-5 en la 19.
        Integer filas = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM respuesta r JOIN pregunta p ON p.id = r.pregunta_id "
                + "JOIN encuesta e ON e.id = p.encuesta_id "
                + "WHERE r.vivienda_id = ? AND e.tipo = 'FINAL'", Integer.class, sesion.viviendaId());
        assertThat(filas).isEqualTo(22);
        Integer plataforma = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM respuesta r JOIN pregunta p ON p.id = r.pregunta_id "
                + "WHERE r.vivienda_id = ? AND p.tipo_pregunta = 'PERCEPCION' AND p.orden >= 19",
            Integer.class, sesion.viviendaId());
        assertThat(plataforma).as("los 4 ítems de plataforma se guardan").isEqualTo(4);
        String valorFrecuencia = jdbcTemplate.queryForObject(
            "SELECT r.valor_texto FROM respuesta r JOIN pregunta p ON p.id = r.pregunta_id "
                + "WHERE r.vivienda_id = ? AND p.orden = 19", String.class, sesion.viviendaId());
        assertThat(valorFrecuencia)
            .as("la frecuencia de uso guarda el valor numérico 1-5")
            .isIn("1", "2", "3", "4", "5");

        // Segundo envío: 409.
        mockMvc.perform(post("/api/encuesta-final")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content(cuerpoEnvioFinal()))
            .andExpect(status().isConflict());
    }

    @Test
    void deshabilitarVuelveABloquearYQuedaTrazabilidad() throws Exception {
        SesionAutenticada sesion = registrarYEntrar("toggle");
        responderPreprueba(sesion);
        MockHttpSession admin = sesionAdmin();

        Cookie tokenHab = xsrf();
        mockMvc.perform(post("/api/admin/estudio/posprueba/habilitar")
                .session(admin)
                .cookie(tokenHab)
                .header("X-XSRF-TOKEN", tokenHab.getValue()))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/encuesta-final/disponible").session(sesion.session()))
            .andExpect(jsonPath("$.disponible").value(true));

        // Habilitar de nuevo: 409 (ya está habilitada).
        Cookie tokenReintento = xsrf();
        mockMvc.perform(post("/api/admin/estudio/posprueba/habilitar")
                .session(admin)
                .cookie(tokenReintento)
                .header("X-XSRF-TOKEN", tokenReintento.getValue()))
            .andExpect(status().isConflict());

        // Deshabilitar: vuelve a bloquearse para el residente.
        Cookie tokenDes = xsrf();
        mockMvc.perform(post("/api/admin/estudio/posprueba/deshabilitar")
                .session(admin)
                .cookie(tokenDes)
                .header("X-XSRF-TOKEN", tokenDes.getValue()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mensaje").value("Encuesta final deshabilitada"));

        mockMvc.perform(get("/api/encuesta-final/disponible").session(sesion.session()))
            .andExpect(jsonPath("$.disponible").value(false));
        mockMvc.perform(get("/api/encuesta-final").session(sesion.session()))
            .andExpect(status().isForbidden());

        // Deshabilitar de nuevo: 409 (ya está deshabilitada).
        Cookie tokenRe = xsrf();
        mockMvc.perform(post("/api/admin/estudio/posprueba/deshabilitar")
                .session(admin)
                .cookie(tokenRe)
                .header("X-XSRF-TOKEN", tokenRe.getValue()))
            .andExpect(status().isConflict());

        // Trazabilidad: la habilitación quedó registrada con fecha y responsable.
        var fila = jdbcTemplate.queryForMap(
            "SELECT posprueba_habilitada, posprueba_habilitada_en, posprueba_habilitada_por "
                + "FROM estudio_config WHERE id = 1");
        // H2 devuelve EXISTS/BOOLEAN como Boolean y MySQL como número: aceptar ambos.
        Object marcada = fila.get("posprueba_habilitada");
        boolean habilitada = marcada instanceof Boolean b ? b : ((Number) marcada).intValue() == 1;
        assertThat(habilitada).as("deshabilitada tras el toggle").isFalse();
        assertThat(fila.get("posprueba_habilitada_en")).as("queda la fecha de la habilitación").isNotNull();
        assertThat((String) fila.get("posprueba_habilitada_por")).isEqualTo(ADMIN_EMAIL);
    }

    @Test
    void adminVeEstadoPorViviendaYResidenteNoPuede() throws Exception {
        // Vivienda A: preprueba + final (posprueba habilitada).
        SesionAutenticada a = registrarYEntrar("adm-a");
        responderPreprueba(a);
        MockHttpSession admin = sesionAdmin();
        Cookie tokenHab = xsrf();
        mockMvc.perform(post("/api/admin/estudio/posprueba/habilitar")
                .session(admin)
                .cookie(tokenHab)
                .header("X-XSRF-TOKEN", tokenHab.getValue()))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/encuesta-final")
                .session(a.session())
                .cookie(a.xsrf())
                .header("X-XSRF-TOKEN", a.xsrf().getValue())
                .contentType("application/json")
                .content(cuerpoEnvioFinal()))
            .andExpect(status().isOk());
        // Vivienda B: solo preprueba (elegible, pendiente).
        SesionAutenticada b = registrarYEntrar("adm-b");
        responderPreprueba(b);

        // Residente: 403 (rol insuficiente).
        mockMvc.perform(get("/api/admin/encuesta-final/estado").session(a.session()))
            .andExpect(status().isForbidden());

        // Admin: filas por vivienda con pre vs. post.
        String cuerpo = mockMvc.perform(get("/api/admin/encuesta-final/estado").session(admin))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        String codigoA = jdbcTemplate.queryForObject(
            "SELECT codigo FROM vivienda WHERE id = ?", String.class, a.viviendaId());
        String codigoB = jdbcTemplate.queryForObject(
            "SELECT codigo FROM vivienda WHERE id = ?", String.class, b.viviendaId());

        // Vivienda A: respondió la FINAL con 10/10 — ya no está disponible.
        var filaA = (java.util.Map<String, Object>) ((java.util.List<?>) com.jayway.jsonpath.JsonPath.read(
            cuerpo, "$[?(@.codigo == '" + codigoA + "')]")).get(0);
        assertThat((Boolean) filaA.get("respondio_final")).isTrue();
        assertThat(((Number) filaA.get("puntaje_post")).intValue()).isEqualTo(10);
        assertThat(((Number) filaA.get("puntaje_pre")).intValue()).isEqualTo(10);
        assertThat((Boolean) filaA.get("disponible")).isFalse();

        // Vivienda B: preprueba sin final — disponible y pendiente.
        var filaB = (java.util.Map<String, Object>) ((java.util.List<?>) com.jayway.jsonpath.JsonPath.read(
            cuerpo, "$[?(@.codigo == '" + codigoB + "')]")).get(0);
        assertThat((Boolean) filaB.get("respondio_final")).isFalse();
        assertThat((Boolean) filaB.get("disponible")).isTrue();
        assertThat(((Number) filaB.get("puntaje_post")).intValue()).isZero();
    }
}
