package com.senderos.ejercicios;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integracion del modulo de ejercicios: listado por leccion,
 * calificacion de intentos (primera correcta otorga puntos, reintento
 * no duplica), actividades CLASIFICAR sin exponer la solucion y acceso
 * no autenticado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EjercicioIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired com.senderos.service.CapturaEmailService capturaEmailService;

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String DEMO = "demo@senderos.local";
    private static final String DEMO_PASS = "demo1234";

    record SesionAutenticada(MockHttpSession session, Cookie xsrf) {}

    private SesionAutenticada login(String email, String password) throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andReturn();
        Cookie xsrf = csrf.getResponse().getCookie("XSRF-TOKEN");
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andReturn();
        MockHttpSession session = (MockHttpSession) resultado.getRequest().getSession(false);
        assertThat(session).as("el login debe crear sesion HTTP real").isNotNull();
        return new SesionAutenticada(session, xsrf);
    }

    private String primerLeccionId(SesionAutenticada sesion) throws Exception {
        MvcResult lista = mockMvc.perform(get("/api/lecciones").session(sesion.session()))
            .andExpect(status().isOk())
            .andReturn();
        var m = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*(\\d+)")
            .matcher(lista.getResponse().getContentAsString());
        assertThat(m.find()).as("el listado debe incluir ids de lecciones").isTrue();
        return m.group(1);
    }

    private long primerEjercicioIdDeLeccion(long leccionId) {
        return jdbcTemplate.queryForObject(
            "SELECT id FROM ejercicio WHERE leccion_id = ? AND activo = 1 ORDER BY orden LIMIT 1",
            Long.class, leccionId);
    }

    /** Responde un ejercicio con su respuesta correcta (tomada de la BD, nunca del API). */
    private MvcResult responderCorrectamente(SesionAutenticada sesion, long ejercicioId) throws Exception {
        String correcta = jdbcTemplate.queryForObject(
            "SELECT respuesta FROM ejercicio WHERE id = ?", String.class, ejercicioId);
        String payload = JSON.writeValueAsString(Map.of("respuesta", correcta));
        return mockMvc.perform(post("/api/ejercicios/" + ejercicioId + "/responder")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content(payload))
            .andExpect(status().isOk())
            .andReturn();
    }

    @Test
    void listarEjerciciosPorLeccionRetornaActividadClasificar() throws Exception {
        SesionAutenticada sesion = login(DEMO, DEMO_PASS);
        String leccionId = primerLeccionId(sesion);
        MvcResult lista = mockMvc.perform(get("/api/ejercicios/leccion/" + leccionId).session(sesion.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].tipo").value("CLASIFICAR"))
            .andExpect(jsonPath("$[0].config.destinos", notNullValue()))
            .andExpect(jsonPath("$[0].config.items", notNullValue()))
            .andReturn();
        String cuerpo = lista.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(cuerpo)
            .as("la solucion nunca debe exponerse al cliente")
            .doesNotContain("\"respuesta\"")
            .as("los destinos no exponen el mapa de soluciones")
            .doesNotContain("destinoCorrecto");
    }

    @Test
    void bancoDeEjerciciosCubreLas6Lecciones() {
        Integer lecciones = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM leccion WHERE activa = 1", Integer.class);
        Integer totalEjercicios = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM ejercicio WHERE activo = 1", Integer.class);
        Integer tiposClasificar = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM ejercicio WHERE activo = 1 AND tipo = 'CLASIFICAR'", Integer.class);
        assertThat(lecciones).as("6 lecciones activas").isEqualTo(6);
        assertThat(totalEjercicios).as("8 actividades en el recorrido").isEqualTo(8);
        assertThat(tiposClasificar).as("6 actividades CLASIFICAR").isEqualTo(6);
        // Toda actividad CLASIFICAR debe tener solucion valida en BD.
        Integer sinSolucion = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM ejercicio WHERE tipo = 'CLASIFICAR' "
                + "AND (respuesta NOT LIKE '{%' OR respuesta NOT LIKE '%:%')", Integer.class);
        assertThat(sinSolucion).as("soluciones de CLASIFICAR bien formadas").isZero();
    }

    @Test
    void respuestaIncorrectaDaFeedbackSinRevelar() throws Exception {
        SesionAutenticada sesion = login(DEMO, DEMO_PASS);
        String leccionId = primerLeccionId(sesion);
        long ejercicioId = primerEjercicioIdDeLeccion(Long.parseLong(leccionId));

        MvcResult resultado = mockMvc.perform(post("/api/ejercicios/" + ejercicioId + "/responder")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content("{\"respuesta\":\"cualquier cosa invalida\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.correcto").value(false))
            .andExpect(jsonPath("$.explicacion", notNullValue()))
            .andReturn();
        assertThat(resultado.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
            .as("el feedback explica el error sin revelar la respuesta correcta")
            .doesNotContain("respuestaCorrecta");

        mockMvc.perform(get("/api/ejercicios/leccion/" + leccionId).session(sesion.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].resuelto").value(false));
    }

    @Test
    void respuestaCorrectaDaFeedbackYPuntosUnaSolaVez() throws Exception {
        SesionAutenticada sesion = login(DEMO, DEMO_PASS);
        String leccionId = primerLeccionId(sesion);
        long ejercicioId = primerEjercicioIdDeLeccion(Long.parseLong(leccionId));

        MvcResult primera = responderCorrectamente(sesion, ejercicioId);
        String primeraJson = primera.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(primeraJson)
            .contains("\"correcto\":true")
            .contains("\"primeraCorrecta\":true")
            .doesNotContain("respuestaCorrecta");
        assertThat(java.util.regex.Pattern.compile("\"puntosObtenidos\"\\s*:\\s*(\\d+)")
                .matcher(primeraJson).results()
                .map(r -> Integer.parseInt(r.group(1))).findFirst().orElse(0))
            .as("la primera respuesta correcta otorga puntos")
            .isGreaterThan(0);

        // Reintentar no duplica puntos ni revela la respuesta.
        mockMvc.perform(post("/api/ejercicios/" + ejercicioId + "/responder")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content(JSON.writeValueAsString(Map.of("respuesta",
                    jdbcTemplate.queryForObject("SELECT respuesta FROM ejercicio WHERE id = ?",
                        String.class, ejercicioId)))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.correcto").value(true))
            .andExpect(jsonPath("$.primeraCorrecta").value(false))
            .andExpect(jsonPath("$.puntosObtenidos").value(0));

        mockMvc.perform(get("/api/ejercicios/leccion/" + leccionId).session(sesion.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].resuelto").value(true));
    }

    @Test
    void intentosSonPrivadosDeCadaVivienda() throws Exception {
        SesionAutenticada demo = login(DEMO, DEMO_PASS);
        String leccionId = primerLeccionId(demo);
        long ejercicioId = primerEjercicioIdDeLeccion(Long.parseLong(leccionId));

        mockMvc.perform(post("/api/ejercicios/" + ejercicioId + "/responder")
                .session(demo.session())
                .cookie(demo.xsrf())
                .header("X-XSRF-TOKEN", demo.xsrf().getValue())
                .contentType("application/json")
                .content("{\"respuesta\":\"texto generico\"}"))
            .andExpect(status().isOk());

        SesionAutenticada otra = registrarYEntrar("vivienda2@senderos.local");
        MvcResult lista = mockMvc.perform(get("/api/ejercicios/leccion/" + leccionId)
                .session(otra.session()))
            .andExpect(status().isOk())
            .andReturn();
        assertThat(lista.getResponse().getContentAsString())
            .as("la otra vivienda no debe ver intentos ajenos")
            .doesNotContain("\"intentos\":1");
    }

    private SesionAutenticada registrarYEntrar(String email) throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andReturn();
        Cookie xsrf = csrf.getResponse().getCookie("XSRF-TOKEN");
        mockMvc.perform(post("/api/onboarding/registro")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType("application/json")
                .content("{\"nombre\":\"Segunda Vivienda\",\"email\":\"" + email
                    + "\",\"password\":\"casaSegura123\",\"aceptaTratamientoDatos\":true," + com.senderos.support.CatalogoPruebas.campos() + "}"))
            .andExpect(status().isCreated());
        return login(email, "casaSegura123");
    }

    @Test
    void responderSinSesionDa401() throws Exception {
        mockMvc.perform(get("/api/ejercicios/leccion/1"))
            .andExpect(status().isUnauthorized());
    }
}
