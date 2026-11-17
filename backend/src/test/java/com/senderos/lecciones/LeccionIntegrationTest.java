package com.senderos.lecciones;

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
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests del recorrido educativo OBLIGATORIO: una leccion solo se completa
 * resolviendo correctamente todos sus ejercicios; resolver una leccion
 * desbloquea la siguiente; las bloqueadas rechazan abrirse y responderse;
 * el progreso persiste por vivienda (aislamiento) entre sesiones.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LeccionIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired com.senderos.service.CapturaEmailService capturaEmailService;

    private static final String DEMO = "demo@senderos.local";
    private static final String DEMO_PASS = "demo1234";

    record SesionAutenticada(MockHttpSession session, Cookie xsrf) {}

    private SesionAutenticada login(String email, String password) throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
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

    /** Registra una vivienda nueva (asignacion automatica) y devuelve su sesion. */
    private SesionAutenticada registrarYLogin(String email) throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andReturn();
        Cookie xsrf = csrf.getResponse().getCookie("XSRF-TOKEN");
        mockMvc.perform(post("/api/onboarding/registro")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType("application/json")
                .content("{\"nombre\":\"Recorrido\",\"email\":\"" + email
                    + "\",\"password\":\"claveSegura1\",\"aceptaTratamientoDatos\":true," + com.senderos.support.CatalogoPruebas.campos() + "}"))
            .andExpect(status().isCreated());
        return login(email, "claveSegura1");
    }

    private String primerIdLeccion(SesionAutenticada sesion) throws Exception {
        MvcResult lista = mockMvc.perform(get("/api/lecciones").session(sesion.session()))
            .andExpect(status().isOk())
            .andReturn();
        var m = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*(\\d+)")
            .matcher(lista.getResponse().getContentAsString());
        assertThat(m.find()).as("el listado debe incluir ids de lecciones").isTrue();
        return m.group(1);
    }

    private String escapar(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** Responde correctamente todos los ejercicios activos de la leccion (respuestas tomadas de la BD). */
    private void completarEjerciciosDeLeccion(SesionAutenticada sesion, long leccionId) throws Exception {
        List<Long> ejercicios = jdbcTemplate.queryForList(
            "SELECT id FROM ejercicio WHERE leccion_id = ? AND activo = 1 ORDER BY orden", Long.class, leccionId);
        assertThat(ejercicios).as("la leccion debe tener ejercicios activos").isNotEmpty();
        for (Long ejercicioId : ejercicios) {
            String correcta = jdbcTemplate.queryForObject(
                "SELECT respuesta FROM ejercicio WHERE id = ?", String.class, ejercicioId);
            mockMvc.perform(post("/api/ejercicios/" + ejercicioId + "/responder")
                    .session(sesion.session())
                    .cookie(sesion.xsrf())
                    .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                    .contentType("application/json")
                    .content("{\"respuesta\":\"" + escapar(correcta) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correcto").value(true));
        }
    }

    private List<Long> idsLeccionesActivas() {
        return jdbcTemplate.queryForList(
            "SELECT id FROM leccion WHERE activa = 1 ORDER BY orden", Long.class);
    }

    @Test
    void listarLeccionesRetornaSeedConEstados() throws Exception {
        SesionAutenticada sesion = login(DEMO, DEMO_PASS);
        mockMvc.perform(get("/api/lecciones").session(sesion.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(6)))
            .andExpect(jsonPath("$[0].titulo", notNullValue()))
            .andExpect(jsonPath("$[0].estado", notNullValue()))
            .andExpect(jsonPath("$[0].ejerciciosTotales", notNullValue()));
    }

    @Test
    void detalleRetornaContenidoCompleto() throws Exception {
        SesionAutenticada sesion = login(DEMO, DEMO_PASS);
        String id = primerIdLeccion(sesion);
        mockMvc.perform(get("/api/lecciones/" + id).session(sesion.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(Integer.parseInt(id)))
            .andExpect(jsonPath("$.contenido", notNullValue()));
    }

    @Test
    void detalleIncluyeImagenEjemploEIdeaClave() throws Exception {
        SesionAutenticada sesion = login(DEMO, DEMO_PASS);
        String id = primerIdLeccion(sesion);
        MvcResult detalle = mockMvc.perform(get("/api/lecciones/" + id).session(sesion.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.imagen").value("/images/aprendizaje/leccion-01-problema.webp"))
            .andExpect(jsonPath("$.ejemplo", notNullValue()))
            .andExpect(jsonPath("$.ideaClave", notNullValue()))
            .andReturn();
        assertThat(detalle.getResponse().getContentAsString())
            .as("el detalle nunca debe exponer la respuesta de un ejercicio")
            .doesNotContain("\"respuesta\"");
    }

    @Test
    void recorridoNuevoDejaPrimeraLeccionActualYRestoBloqueadas() throws Exception {
        SesionAutenticada sesion = registrarYLogin("flujo.nuevo." + System.nanoTime() + "@test.senderos");
        List<Long> ids = idsLeccionesActivas();

        MvcResult lista = mockMvc.perform(get("/api/lecciones").session(sesion.session()))
            .andExpect(status().isOk())
            .andReturn();
        String cuerpo = lista.getResponse().getContentAsString();
        for (int i = 0; i < ids.size(); i++) {
            String esperado = i == 0 ? "ACTUAL" : "BLOQUEADA";
            assertThat(cuerpo).contains("\"estado\":\"" + esperado + "\"");
        }
        // La primera debe aparecer como ACTUAL exactamente una vez.
        assertThat(cuerpo.split("\"estado\":\"ACTUAL\"")).hasSize(2); // split deja n+1 partes

        // Las bloqueadas rechazan abrirse, listar ejercicios y responderse.
        Long segunda = ids.get(1);
        mockMvc.perform(get("/api/lecciones/" + segunda).session(sesion.session()))
            .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/api/ejercicios/leccion/" + segunda).session(sesion.session()))
            .andExpect(status().isUnprocessableEntity());
        Long ejercicioBloqueado = jdbcTemplate.queryForObject(
            "SELECT id FROM ejercicio WHERE leccion_id = ? AND activo = 1 ORDER BY orden LIMIT 1",
            Long.class, segunda);
        mockMvc.perform(post("/api/ejercicios/" + ejercicioBloqueado + "/responder")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content("{\"respuesta\":\"cualquiera\"}"))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void abrirLaLeccionNoLaCompleta() throws Exception {
        SesionAutenticada sesion = registrarYLogin("flujo.leer." + System.nanoTime() + "@test.senderos");
        String id = primerIdLeccion(sesion);
        mockMvc.perform(get("/api/lecciones/" + id).session(sesion.session()))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/lecciones/progreso").session(sesion.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completadas").value(0));
    }

    @Test
    void responderIncorrectoNoCompletaLaLeccion() throws Exception {
        SesionAutenticada sesion = registrarYLogin("flujo.mal." + System.nanoTime() + "@test.senderos");
        List<Long> ids = idsLeccionesActivas();
        Long ejercicio = jdbcTemplate.queryForObject(
            "SELECT id FROM ejercicio WHERE leccion_id = ? AND activo = 1 ORDER BY orden LIMIT 1",
            Long.class, ids.get(0));

        mockMvc.perform(post("/api/ejercicios/" + ejercicio + "/responder")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content("{\"respuesta\":\"respuesta-incorrecta-a-proposito\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.correcto").value(false))
            .andExpect(jsonPath("$.leccionCompletada").value(false));

        mockMvc.perform(get("/api/lecciones/progreso").session(sesion.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completadas").value(0));

        // La leccion sigue ACTUAL (no completada) y la siguiente sigue bloqueada.
        Long segunda = ids.get(1);
        mockMvc.perform(get("/api/lecciones/" + segunda).session(sesion.session()))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void completarEjerciciosDesbloqueaLaSiguienteLeccion() throws Exception {
        SesionAutenticada sesion = registrarYLogin("flujo.desbloqueo." + System.nanoTime() + "@test.senderos");
        List<Long> ids = idsLeccionesActivas();
        Long tercera = ids.get(2);
        Long cuarta = ids.get(3);
        // El recorrido exige completar antes las lecciones 1 y 2.
        completarEjerciciosDeLeccion(sesion, ids.get(0));
        completarEjerciciosDeLeccion(sesion, ids.get(1));
        List<Long> ejercicios = jdbcTemplate.queryForList(
            "SELECT id FROM ejercicio WHERE leccion_id = ? AND activo = 1 ORDER BY orden", Long.class, tercera);
        assertThat(ejercicios).as("la leccion 3 tiene dos ejercicios").hasSize(2);

        // Primer ejercicio correcto: la leccion aun no se completa.
        String correcta1 = jdbcTemplate.queryForObject(
            "SELECT respuesta FROM ejercicio WHERE id = ?", String.class, ejercicios.get(0));
        mockMvc.perform(post("/api/ejercicios/" + ejercicios.get(0) + "/responder")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content("{\"respuesta\":\"" + escapar(correcta1) + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.correcto").value(true))
            .andExpect(jsonPath("$.leccionCompletada").value(false));

        // Ultimo ejercicio correcto: completa la leccion y desbloquea la siguiente.
        String correcta2 = jdbcTemplate.queryForObject(
            "SELECT respuesta FROM ejercicio WHERE id = ?", String.class, ejercicios.get(1));
        mockMvc.perform(post("/api/ejercicios/" + ejercicios.get(1) + "/responder")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content("{\"respuesta\":\"" + escapar(correcta2) + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.correcto").value(true))
            .andExpect(jsonPath("$.leccionCompletada").value(true))
            .andExpect(jsonPath("$.siguienteLeccionId").value(cuarta.intValue()));

        // Estados: 3 COMPLETADA, 4 ACTUAL; detalle de la 4 accesible.
        MvcResult lista = mockMvc.perform(get("/api/lecciones").session(sesion.session()))
            .andExpect(status().isOk())
            .andReturn();
        String cuerpo = lista.getResponse().getContentAsString();
        assertThat(cuerpo).contains("\"estado\":\"COMPLETADA\"");
        assertThat(cuerpo).contains("\"estado\":\"ACTUAL\"");
        mockMvc.perform(get("/api/lecciones/" + cuarta).session(sesion.session()))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/lecciones/progreso").session(sesion.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completadas").value(3))
            .andExpect(jsonPath("$.porcentaje").value(50))
            .andExpect(jsonPath("$.leccionActualId").value(cuarta.intValue()));
    }

    @Test
    void progresoPersisteTrasCerrarSesionYVolver() throws Exception {
        String email = "flujo.persiste." + System.nanoTime() + "@test.senderos";
        SesionAutenticada sesion = registrarYLogin(email);
        completarEjerciciosDeLeccion(sesion, idsLeccionesActivas().get(0));

        // Nueva sesion (equivale a cerrar sesion y volver).
        SesionAutenticada sesion2 = login(email, "claveSegura1");
        mockMvc.perform(get("/api/lecciones/progreso").session(sesion2.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completadas").value(1));
        mockMvc.perform(get("/api/lecciones").session(sesion2.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].estado").value("COMPLETADA"))
            .andExpect(jsonPath("$[1].estado").value("ACTUAL"));
    }

    @Test
    void aislamientoPorViviendaEnElRecorrido() throws Exception {
        SesionAutenticada a = registrarYLogin("flujo.iso.a." + System.nanoTime() + "@test.senderos");
        SesionAutenticada b = registrarYLogin("flujo.iso.b." + System.nanoTime() + "@test.senderos");
        Long primera = idsLeccionesActivas().get(0);

        completarEjerciciosDeLeccion(a, primera);

        // B no ve el avance de A: su recorrido sigue en cero y su leccion 1 sigue ACTUAL.
        mockMvc.perform(get("/api/lecciones/progreso").session(b.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completadas").value(0));
        mockMvc.perform(get("/api/lecciones").session(b.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].estado").value("ACTUAL"))
            .andExpect(jsonPath("$[1].estado").value("BLOQUEADA"));
    }

    @Test
    void completarTodasLasLeccionesProduce100Porciento() throws Exception {
        SesionAutenticada sesion = registrarYLogin("flujo.cien." + System.nanoTime() + "@test.senderos");
        List<Long> ids = idsLeccionesActivas();
        for (Long leccionId : ids) {
            completarEjerciciosDeLeccion(sesion, leccionId);
        }
        mockMvc.perform(get("/api/lecciones/progreso").session(sesion.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalLecciones").value(6))
            .andExpect(jsonPath("$.completadas").value(6))
            .andExpect(jsonPath("$.porcentaje").value(100))
            .andExpect(jsonPath("$.leccionActualId").value(nullValue()));
    }

    @Test
    void completadoManualYaNoExiste() throws Exception {
        SesionAutenticada sesion = registrarYLogin("flujo.manual." + System.nanoTime() + "@test.senderos");
        String id = primerIdLeccion(sesion);
        // El endpoint manual POST /lecciones/{id}/completar fue retirado:
        // completar solo es posible resolviendo los ejercicios.
        mockMvc.perform(post("/api/lecciones/" + id + "/completar")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue()))
            .andExpect(status().isNotFound());
    }

    @Test
    void postCompletarSinCsrfEsRechazado() throws Exception {
        SesionAutenticada sesion = login(DEMO, DEMO_PASS);
        String id = primerIdLeccion(sesion);
        mockMvc.perform(post("/api/ejercicios/1/responder").session(sesion.session()))
            .andExpect(status().isForbidden());
    }

    @Test
    void leccionesRequierenAutenticacion() throws Exception {
        mockMvc.perform(get("/api/lecciones"))
            .andExpect(status().isUnauthorized());
    }
}
