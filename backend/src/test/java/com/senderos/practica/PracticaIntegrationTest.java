package com.senderos.practica;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests del módulo de Clasificación (práctica libre): banco por modos,
 * calificación por tipo de actividad, intentos registrados por vivienda
 * (aislamiento y persistencia), solución nunca expuesta, puntuación y
 * rechazo de respuestas manipuladas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PracticaIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired com.senderos.service.CapturaEmailService capturaEmailService;

    private static final ObjectMapper JSON = new ObjectMapper();

    record SesionAutenticada(MockHttpSession session, Cookie xsrf, String email) {}

    private String sufijo() {
        return Long.toHexString(System.nanoTime());
    }

    private SesionAutenticada registrarYLogin(String nombre) throws Exception {
        String email = "practica-" + sufijo() + "@test.senderos";
        Cookie xsrf = obtenerXsrf();
        mockMvc.perform(post("/api/onboarding/registro")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType("application/json")
                .content("{\"nombre\":\"" + nombre + "\",\"email\":\"" + email
                    + "\",\"password\":\"claveSegura1\",\"aceptaTratamientoDatos\":true," + com.senderos.support.CatalogoPruebas.campos() + "}"))
            .andExpect(status().isCreated());
        return login(email, "claveSegura1");
    }

    private Cookie obtenerXsrf() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        return csrf.getResponse().getCookie("XSRF-TOKEN");
    }

    private SesionAutenticada login(String email, String password) throws Exception {
        Cookie xsrf = obtenerXsrf();
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andReturn();
        MockHttpSession session = (MockHttpSession) resultado.getRequest().getSession(false);
        assertThat(session).as("el login debe crear sesion HTTP real").isNotNull();
        return new SesionAutenticada(session, xsrf, email);
    }

    /** Pide una actividad del modo dado (via API, como el frontend). */
    private MvcResult pedirItem(SesionAutenticada sesion, String modo) throws Exception {
        return mockMvc.perform(get("/api/practica/item")
                .session(sesion.session())
                .queryParam("modo", modo))
            .andExpect(status().isOk())
            .andReturn();
    }

    /** Auditoría A-8: la lista de excluidas del cliente no puede crecer sin tope. */
    @Test
    void listaExcluirExcesivaEsRechazada() throws Exception {
        SesionAutenticada sesion = registrarYLogin("Familia Tope Excluir");
        var peticion = get("/api/practica/item")
                .session(sesion.session())
                .queryParam("modo", "CLASIFICA");
        for (long id = 1; id <= 201; id++) {
            peticion = peticion.queryParam("excluir", String.valueOf(id));
        }
        mockMvc.perform(peticion)
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error").value("Lista de actividades excluidas demasiado grande"));
    }

    private MvcResult responder(SesionAutenticada sesion, long itemId, String respuesta) throws Exception {
        String payload = JSON.writeValueAsString(Map.of("respuesta", respuesta));
        return mockMvc.perform(post("/api/practica/item/" + itemId + "/responder")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content(payload))
            .andExpect(status().isOk())
            .andReturn();
    }

    private String cuerpo(MvcResult r) throws java.io.UnsupportedEncodingException {
        return r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }

    /** Solucion de DESTINO/ELECCION/ORDENAR: cadena simple. */
    private String solucionSimple(long itemId) {
        return jdbcTemplate.queryForObject(
            "SELECT respuesta FROM practica_item WHERE id = ?", String.class, itemId);
    }

    /** Solucion de GRUPO: mapa elemento -> destino. */
    @SuppressWarnings("unchecked")
    private Map<String, String> solucionDe(long itemId) throws Exception {
        String cruda = jdbcTemplate.queryForObject(
            "SELECT respuesta FROM practica_item WHERE id = ?", String.class, itemId);
        return JSON.readValue(com.senderos.dto.EjercicioDTO.limpiarJson(cruda), Map.class);
    }

    @Test
    void losTresModosSirvenActividadesSinExponerLaSolucion() throws Exception {
        SesionAutenticada sesion = registrarYLogin("Familia Practica");
        for (String modo : List.of("CLASIFICA", "PREPARA", "SITUACION")) {
            String cuerpo = cuerpo(pedirItem(sesion, modo));
            String tipo = JSON.readTree(cuerpo).get("tipo").asText();
            assertThat(tipo).isIn("DESTINO", "ELECCION", "ORDENAR", "GRUPO");
            assertThat(cuerpo)
                .as("la solucion y su explicacion no viajan al cliente (modo %s)", modo)
                .doesNotContain("\"respuesta\"")
                .doesNotContain("\"explicacion\"");
        }
        // Los contadores de la BD confirman el banco amplio.
        Integer banco = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM practica_item WHERE activo = 1", Integer.class);
        assertThat(banco).as("banco de al menos 25 actividades").isGreaterThanOrEqualTo(25);
    }

    @Test
    void clasificacionDestinoCorrectaEIncorrectaConFeedback() throws Exception {
        SesionAutenticada sesion = registrarYLogin("Familia Clasifica");
        String cuerpo = cuerpo(pedirItem(sesion, "CLASIFICA"));
        long itemId = JSON.readTree(cuerpo).get("id").asLong();
        String solucion = solucionSimple(itemId);

        String bien = cuerpo(responder(sesion, itemId, solucion));
        assertThat(bien)
            .contains("\"correcto\":true")
            .contains("\"explicacion\"")
            .doesNotContain("respuestaCorrecta");

        String mal = cuerpo(responder(sesion, itemId, "destino-que-no-existe"));
        assertThat(mal)
            .contains("\"correcto\":false")
            .as("el feedback incorrecto tambien educa")
            .contains("\"explicacion\"");
    }

    @Test
    void eleccionGradaPorOpcion() throws Exception {
        SesionAutenticada sesion = registrarYLogin("Familia Elige");
        String cuerpo = cuerpo(pedirItem(sesion, "PREPARA"));
        var nodo = JSON.readTree(cuerpo);
        assertThat(nodo.get("tipo").asText()).isEqualTo("ELECCION");
        long itemId = nodo.get("id").asLong();
        String solucion = solucionSimple(itemId);

        // Busca una opcion incorrecta real del contenido.
        String incorrecta = null;
        for (var opcion : nodo.get("contenido").get("opciones")) {
            if (!opcion.get("id").asText().equals(solucion)) {
                incorrecta = opcion.get("id").asText();
                break;
            }
        }
        assertThat(incorrecta).isNotNull();

        assertThat(cuerpo(responder(sesion, itemId, incorrecta))).contains("\"correcto\":false");
        assertThat(cuerpo(responder(sesion, itemId, solucion))).contains("\"correcto\":true");
    }

    @Test
    void grupoRespuestaParcialOFaltanteEsIncorrectaConDetalle() throws Exception {
        SesionAutenticada sesion = registrarYLogin("Familia Grupo");
        // Busca via API hasta conseguir una actividad GRUPO.
        long itemId = 0;
        for (int intento = 0; intento < 30 && itemId == 0; intento++) {
            var nodo = JSON.readTree(cuerpo(pedirItem(sesion, "SITUACION")));
            if ("GRUPO".equals(nodo.get("tipo").asText())) {
                itemId = nodo.get("id").asLong();
            }
        }
        assertThat(itemId).as("deben existir actividades GRUPO en SITUACION").isNotZero();
        Map<String, String> solucion = solucionDe(itemId);

        // Parcial: solo la mitad de los elementos.
        Map<String, String> parcial = new HashMap<>();
        int mitad = solucion.size() / 2;
        int i = 0;
        for (var e : solucion.entrySet()) {
            if (i++ < mitad) parcial.put(e.getKey(), e.getValue());
        }
        assertThat(mitad).isGreaterThan(0);
        String respuestaParcial = responder(sesion, itemId, JSON.writeValueAsString(parcial))
            .getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(respuestaParcial)
            .contains("\"correcto\":false")
            .as("feedback de grupo marca elemento por elemento")
            .contains("\"detalleItems\"");

        // Faltante absoluto: mapa vacio.
        String vacia = responder(sesion, itemId, "{}")
            .getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(vacia).contains("\"correcto\":false");

        // Completa: correcta.
        String completa = responder(sesion, itemId, JSON.writeValueAsString(solucion))
            .getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(completa).contains("\"correcto\":true");
    }

    @Test
    void ordenarGradaLaSecuenciaCompleta() throws Exception {
        SesionAutenticada sesion = registrarYLogin("Familia Ordena");
        // Busca via API hasta conseguir un ORDENAR (hay en PREPARA/SITUACION).
        long itemId = 0;
        String solucion = null;
        for (int intento = 0; intento < 30 && itemId == 0; intento++) {
            var nodo = JSON.readTree(cuerpo(pedirItem(sesion, "SITUACION")));
            if ("ORDENAR".equals(nodo.get("tipo").asText())) {
                itemId = nodo.get("id").asLong();
                solucion = solucionSimple(itemId);
            }
        }
        assertThat(itemId).as("debe existir actividad ORDENAR en SITUACION").isNotZero();

        // Desordena: intercambia dos tramos de la secuencia.
        String[] partes = solucion.split("\\|");
        String alterada = partes[1] + "|" + partes[0] + "|" + partes[2];
        if (partes.length > 3) alterada += "|" + partes[3];
        assertThat(cuerpo(responder(sesion, itemId, alterada))).contains("\"correcto\":false");
        assertThat(cuerpo(responder(sesion, itemId, solucion))).contains("\"correcto\":true");
    }

    @Test
    void intentosSeRegistranPorViviendaConAislamiento() throws Exception {
        SesionAutenticada a = registrarYLogin("Familia A");
        SesionAutenticada b = registrarYLogin("Familia B");
        long itemIdA = JSON.readTree(cuerpo(pedirItem(a, "CLASIFICA"))).get("id").asLong();
        String solucionA = solucionSimple(itemIdA);

        responder(a, itemIdA, solucionA);
        responder(a, itemIdA, "otra-cosa");

        // El intento queda en BD asociado a la vivienda de la sesion.
        Long viviendaA = jdbcTemplate.queryForObject(
            "SELECT vivienda_id FROM usuario WHERE email = ?", Long.class, a.email());
        Integer intentos = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM intento_practica WHERE vivienda_id = ?", Integer.class, viviendaA);
        assertThat(intentos).as("intento registrado para la vivienda de la sesion").isEqualTo(2);

        // B empieza desde cero: su resumen no ve lo de A.
        mockMvc.perform(get("/api/practica/resumen").session(b.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(0))
            .andExpect(jsonPath("$.correctas").value(0));
    }

    @Test
    void puntuacionYRachaAcumulanPorVivienda() throws Exception {
        SesionAutenticada sesion = registrarYLogin("Familia Racha");
        // Tres aciertos seguidos.
        for (int i = 0; i < 3; i++) {
            var nodo = JSON.readTree(cuerpo(pedirItem(sesion, "CLASIFICA")));
            long itemId = nodo.get("id").asLong();
            String solucion = solucionSimple(itemId);
            String cuerpo = cuerpo(responder(sesion, itemId, solucion));
            assertThat(cuerpo).contains("\"correcto\":true");
        }
        mockMvc.perform(get("/api/practica/resumen").session(sesion.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(3))
            .andExpect(jsonPath("$.correctas").value(3))
            .andExpect(jsonPath("$.racha").value(3))
            .andExpect(jsonPath("$.rachaMaxima").value(3));

        // Un fallo rompe la racha pero conserva el maximo.
        var nodo = JSON.readTree(cuerpo(pedirItem(sesion, "CLASIFICA")));
        responder(sesion, nodo.get("id").asLong(), "respuesta-invalida");
        mockMvc.perform(get("/api/practica/resumen").session(sesion.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(4))
            .andExpect(jsonPath("$.correctas").value(3))
            .andExpect(jsonPath("$.racha").value(0))
            .andExpect(jsonPath("$.rachaMaxima").value(3));
    }

    @Test
    void exclusionEvitaRepetirActividadesEnLaRonda() throws Exception {
        SesionAutenticada sesion = registrarYLogin("Familia SinRepetir");
        List<Long> ids = jdbcTemplate.queryForList(
            "SELECT id FROM practica_item WHERE modo = 'CLASIFICA' AND activo = 1", Long.class);
        List<Long> excluir = new ArrayList<>(ids);
        excluir.remove(0);
        String excluidas = excluir.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");

        for (int i = 0; i < 3; i++) {
            MvcResult r = mockMvc.perform(get("/api/practica/item")
                    .session(sesion.session())
                    .queryParam("modo", "CLASIFICA")
                    .queryParam("excluir", excluidas))
                .andExpect(status().isOk())
                .andReturn();
            assertThat(JSON.readTree(cuerpo(r)).get("id").asLong()).isEqualTo(ids.get(0));
        }
    }

    @Test
    void respuestasManipuladasSeRechazan() throws Exception {
        SesionAutenticada sesion = registrarYLogin("Familia Hacker");

        // Item inexistente.
        mockMvc.perform(post("/api/practica/item/999999/responder")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content("{\"respuesta\":\"lo que sea\"}"))
            .andExpect(status().isNotFound());

        // Modo inexistente.
        mockMvc.perform(get("/api/practica/item").session(sesion.session()).queryParam("modo", "HACKER"))
            .andExpect(status().isUnprocessableEntity());

        // JSON malformado como respuesta de GRUPO: califica incorrecta, no rompe.
        String cuerpo = cuerpo(pedirItem(sesion, "SITUACION"));
        var nodo = JSON.readTree(cuerpo);
        if ("GRUPO".equals(nodo.get("tipo").asText())) {
            long itemId = nodo.get("id").asLong();
            String basura = responder(sesion, itemId, "{esto no es json")
                .getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            assertThat(basura).contains("\"correcto\":false");
        }

        // Sin respuesta (blank) la validacion rechaza.
        long itemId = JSON.readTree(cuerpo).get("id").asLong();
        mockMvc.perform(post("/api/practica/item/" + itemId + "/responder")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content("{\"respuesta\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void sinSesionDa401() throws Exception {
        mockMvc.perform(get("/api/practica/item").queryParam("modo", "CLASIFICA"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/practica/resumen"))
            .andExpect(status().isUnauthorized());
    }
}
