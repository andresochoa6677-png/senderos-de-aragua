package com.senderos.onboarding;

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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración de la encuesta INICIAL (preprueba, V24):
 * 18 ítems en 3 partes — 10 de conocimiento (OPCION con 1 correcta),
 * 4 de prácticas (ESCALA frecuencia 1-5) y 4 de percepción (ESCALA de
 * acuerdo 1-5; el ítem 18 ya NO es la barrera abierta "Otra").
 *
 * Verifica que la clave de corrección nunca se expone al cliente, que
 * el backend devuelve el puntaje de conocimiento 0-10, que las escalas
 * conservan su valor 1-5, que se guardan las 18 respuestas individuales
 * por vivienda y que la encuesta no puede sobrescribirse.
 *
 * Se usa una vivienda propia creada por test (no DEMO) para no
 * contaminar el conteo de sesiones del usuario demo que hace
 * AuthIntegrationTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EncuestaInicialIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired com.senderos.service.CapturaEmailService capturaEmailService;

    record SesionAutenticada(MockHttpSession session, Cookie xsrf) {}

    private Cookie obtenerXsrf() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        return csrf.getResponse().getCookie("XSRF-TOKEN");
    }

    /** Registra una cuenta nueva (vivienda asignada automaticamente) y hace login. */
    private SesionAutenticada registrarYEntrar(String etiqueta) throws Exception {
        String sufijo = Long.toHexString(System.nanoTime());
        String email = "encuesta-" + etiqueta + "-" + sufijo + "@senderos.local";
        Cookie xsrf = obtenerXsrf();
        mockMvc.perform(post("/api/onboarding/registro")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType("application/json")
                .content("{\"nombre\":\"Encuesta Test\","
                    + "\"email\":\"" + email + "\","
                    + "\"password\":\"clave-segura-123\",\"aceptaTratamientoDatos\":true," + com.senderos.support.CatalogoPruebas.campos() + "}"))
            .andExpect(status().isCreated());
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"clave-segura-123\"}"))
            .andExpect(status().isOk())
            .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).as("el login debe crear sesion HTTP real").isNotNull();
        SesionAutenticada sesion = new SesionAutenticada(session, xsrf);
        // Desde REG-2.0 la encuesta exige consentimiento aceptado: se firma aquí.
        mockMvc.perform(post("/api/onboarding/consentimiento")
                .session(session)
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue()))
            .andExpect(status().isOk());
        return sesion;
    }

    private String cuerpoEncuesta(SesionAutenticada sesion) throws Exception {
        MvcResult r = mockMvc.perform(get("/api/onboarding/encuesta/INICIAL").session(sesion.session()))
            .andExpect(status().isOk())
            .andReturn();
        return r.getResponse().getContentAsString();
    }

    private List<Long> idsPreguntasEnOrden() {
        return jdbcTemplate.queryForList(
            "SELECT p.id FROM pregunta p JOIN encuesta e ON e.id = p.encuesta_id "
                + "WHERE e.tipo = 'INICIAL' ORDER BY p.orden", Long.class);
    }

    /**
     * Construye un envío completo de 18 ítems. En conocimiento elige la
     * clave (es_correcta=1) o una errada según el parámetro; en escalas
     * la primera opción (valor 1).
     */
    private String construirEnvio(boolean aciertos) {
        StringBuilder items = new StringBuilder();
        List<Long> ids = idsPreguntasEnOrden();
        for (int i = 0; i < ids.size(); i++) {
            Long preguntaId = ids.get(i);
            String seccion = jdbcTemplate.queryForObject(
                "SELECT tipo_pregunta FROM pregunta WHERE id = ?", String.class, preguntaId);
            String opcionSql = seccion.equals("CONOCIMIENTO")
                ? "SELECT id FROM opcion WHERE pregunta_id = " + preguntaId
                    + " AND es_correcta = " + (aciertos ? 1 : 0) + " ORDER BY orden LIMIT 1"
                : "SELECT id FROM opcion WHERE pregunta_id = " + preguntaId + " ORDER BY orden LIMIT 1";
            Long opcionId = jdbcTemplate.queryForObject(opcionSql, Long.class);
            if (i > 0) items.append(',');
            items.append("{\"preguntaId\":").append(preguntaId)
                 .append(",\"opcionId\":").append(opcionId).append('}');
        }
        return "{\"encuestaId\":1,\"respuestas\":[" + items + "]}";
    }

    @Test
    void laEncuestaInicialTiene18ItemsEn3PartesYClaveOculta() throws Exception {
        SesionAutenticada sesion = registrarYEntrar("estructura");

        mockMvc.perform(get("/api/onboarding/encuesta/INICIAL").session(sesion.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tipo").value("INICIAL"))
            .andExpect(jsonPath("$.preguntas", hasSize(18)))
            .andExpect(jsonPath("$.preguntas[?(@.tipoPregunta=='CONOCIMIENTO')]", hasSize(10)))
            .andExpect(jsonPath("$.preguntas[?(@.tipoPregunta=='PRACTICA')]", hasSize(4)))
            .andExpect(jsonPath("$.preguntas[?(@.tipoPregunta=='PERCEPCION')]", hasSize(4)));

        String cuerpo = cuerpoEncuesta(sesion);
        assertThat(cuerpo)
            .as("la clave de correccion no debe exponerse al cliente")
            .doesNotContain("esCorrecta")
            .doesNotContain("es_correcta");
        assertThat(cuerpo)
            .as("el valor interno de las opciones (clave numerica) no debe exponerse")
            .doesNotContain("\"valor\"");
    }

    @Test
    void responderGuardaLas18RespuestasYCalculaPuntaje0a10() throws Exception {
        SesionAutenticada sesion = registrarYEntrar("puntaje");
        Long viviendaId = jdbcTemplate.queryForObject(
            "SELECT v.id FROM vivienda v JOIN usuario u ON u.vivienda_id = v.id "
                + "WHERE u.email LIKE 'encuesta-puntaje-%' ORDER BY u.id DESC LIMIT 1", Long.class);
        assertThat(viviendaId).isNotNull();

        // El consentimiento de tratamiento de datos se registro en el propio
        // registro (version REG-1.0): el onboarding arranca en la encuesta.
        mockMvc.perform(get("/api/onboarding/estado").session(sesion.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.siguientePaso").value("ENCUESTA_INICIAL"));

        // La encuesta rechaza el envio si falta alguna de las 18.
        List<Long> ids = idsPreguntasEnOrden();
        assertThat(ids).as("la encuesta INICIAL debe tener 18 items").hasSize(18);
        Integer opcionValida = jdbcTemplate.queryForObject(
            "SELECT id FROM opcion WHERE pregunta_id = ? ORDER BY orden ASC LIMIT 1",
            Integer.class, ids.get(0));
        mockMvc.perform(post("/api/onboarding/encuesta")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content("{\"encuestaId\":1,\"respuestas\":["
                    + "{\"preguntaId\":" + ids.get(0) + ",\"opcionId\":" + opcionValida + "}]}"))
            .andExpect(status().isUnprocessableEntity());

        // Envio completo con TODAS las claves de conocimiento correctas:
        // el backend califica server-side y devuelve el puntaje 10/10.
        mockMvc.perform(post("/api/onboarding/encuesta")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content(construirEnvio(true)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.puntajeConocimiento").value("10"));

        // 18 filas individuales asociadas a la vivienda, una por pregunta.
        Integer filas = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM respuesta WHERE vivienda_id = ?", Integer.class, viviendaId);
        assertThat(filas).as("deben guardarse las 18 respuestas individuales").isEqualTo(18);

        // Aciertos reales en BD: 10/10.
        Integer aciertos = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM respuesta r JOIN opcion o ON o.id = r.opcion_id "
                + "JOIN pregunta p ON p.id = r.pregunta_id "
                + "WHERE r.vivienda_id = ? AND p.tipo_pregunta = 'CONOCIMIENTO' AND o.es_correcta = 1",
            Integer.class, viviendaId);
        assertThat(aciertos).as("con todas las claves elegidas el conocimiento debe ser 10/10").isEqualTo(10);

        // Practicas (4) y percepcion (4) son ESCALA y conservan el valor 1-5
        // (el item 18 ya no es la barrera abierta "Otra": es Likert).
        Integer escala = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM respuesta r JOIN pregunta p ON p.id = r.pregunta_id "
                + "WHERE r.vivienda_id = ? AND p.tipo_pregunta IN ('PRACTICA','PERCEPCION') "
                + "AND r.valor_texto IN ('1','2','3','4','5')",
            Integer.class, viviendaId);
        assertThat(escala).as("4 practicas + 4 percepcion deben conservar valores 1-5").isEqualTo(8);

        // El estado de onboarding avanza a DASHBOARD y un segundo envio
        // de la encuesta INICIAL se rechaza (no sobrescribir la preprueba).
        mockMvc.perform(get("/api/onboarding/estado").session(sesion.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.siguientePaso").value("DASHBOARD"));

        mockMvc.perform(post("/api/onboarding/encuesta")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .content("{\"encuestaId\":1,\"respuestas\":["
                    + "{\"preguntaId\":" + ids.get(0) + ",\"opcionId\":" + opcionValida + "}]}")
                .contentType("application/json"))
            .andExpect(status().isConflict());
    }

    @Test
    void elInstrumentoNuevoYaNoTraeOtraAbierta() throws Exception {
        // El ítem 18 pasó de barrera OPCION con "Otra" a escala Likert:
        // ninguna pregunta de las dos encuestas trae ya la opción abierta.
        Integer otras = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM opcion o JOIN pregunta p ON p.id = o.pregunta_id "
                + "JOIN encuesta e ON e.id = p.encuesta_id "
                + "WHERE e.tipo IN ('INICIAL','FINAL') AND o.valor = 'OTRA'", Integer.class);
        assertThat(otras).as("el instrumento nuevo no usa la opcion abierta OTRA").isZero();
    }
}
