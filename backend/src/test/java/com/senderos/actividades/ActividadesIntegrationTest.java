package com.senderos.actividades;

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

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la actividad obligatoria CLASIFICAR del rediseño: la solucion
 * vive solo en el backend, la actividad exige acertar TODOS los elementos,
 * fallar no completa la leccion y acertar la completa desbloqueando la
 * siguiente, con progreso aislado y persistente por vivienda.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActividadesIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired com.senderos.service.CapturaEmailService capturaEmailService;

    private static final ObjectMapper JSON = new ObjectMapper();

    record SesionAutenticada(MockHttpSession session, Cookie xsrf) {}

    private String sufijo() {
        return Long.toHexString(System.nanoTime());
    }

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

    private SesionAutenticada registrarYLogin(String nombre) throws Exception {
        String email = "act-" + sufijo() + "@test.senderos";
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andReturn();
        Cookie xsrf = csrf.getResponse().getCookie("XSRF-TOKEN");
        mockMvc.perform(post("/api/onboarding/registro")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType("application/json")
                .content("{\"nombre\":\"" + nombre + "\",\"email\":\"" + email
                    + "\",\"password\":\"claveSegura1\",\"aceptaTratamientoDatos\":true," + com.senderos.support.CatalogoPruebas.campos() + "}"))
            .andExpect(status().isCreated());
        SesionAutenticada sesion = login(email, "claveSegura1");
        emailsRegistrados.put(nombre, email);
        return sesion;
    }

    /** Emails de las viviendas de prueba de esta clase, por nombre. */
    private final Map<String, String> emailsRegistrados = new HashMap<>();

    private long leccionPorOrden(int orden) {
        return jdbcTemplate.queryForObject(
            "SELECT id FROM leccion WHERE activa = 1 AND orden = ?", Long.class, orden);
    }

    private long primerEjercicioDeLeccion(long leccionId) {
        return jdbcTemplate.queryForObject(
            "SELECT id FROM ejercicio WHERE leccion_id = ? AND activo = 1 ORDER BY orden LIMIT 1",
            Long.class, leccionId);
    }

    /** Solucion de la actividad, leida de la BD (el API nunca la expone). */
    @SuppressWarnings("unchecked")
    private Map<String, String> solucionDe(long ejercicioId) throws Exception {
        String cruda = jdbcTemplate.queryForObject(
            "SELECT respuesta FROM ejercicio WHERE id = ?", String.class, ejercicioId);
        return JSON.readValue(com.senderos.dto.EjercicioDTO.limpiarJson(cruda), Map.class);
    }

    private MvcResult responderClasificar(SesionAutenticada sesion, long ejercicioId,
                                          Map<String, String> asignaciones) throws Exception {
        String payload = JSON.writeValueAsString(Map.of("respuesta", JSON.writeValueAsString(asignaciones)));
        return mockMvc.perform(post("/api/ejercicios/" + ejercicioId + "/responder")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content(payload))
            .andReturn();
    }

    @Test
    void actividadCorrectaCompletaLaLeccionYDesbloqueaLaSiguiente() throws Exception {
        SesionAutenticada sesion = registrarYLogin("Familia Activa");
        long leccion1 = leccionPorOrden(1);
        long leccion2 = leccionPorOrden(2);
        long actividad = primerEjercicioDeLeccion(leccion1);

        MvcResult resultado = responderClasificar(sesion, actividad, solucionDe(actividad));
        String cuerpo = resultado.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(cuerpo)
            .contains("\"correcto\":true")
            .contains("\"leccionCompletada\":true")
            .contains("\"siguienteLeccionId\":" + leccion2);
        // El detalle por elemento existe y todos acertaron; no revela destinos.
        var detalle = JSON.readTree(cuerpo).get("detalleItems");
        assertThat(detalle).isNotNull();
        detalle.forEach(d -> assertThat(d.get("correcto").asBoolean()).isTrue());
        assertThat(cuerpo).doesNotContain("destinoCorrecto");

        // Estados tras completar: 1 COMPLETADA, 2 ACTUAL, resto BLOQUEADA.
        MvcResult lista = mockMvc.perform(get("/api/lecciones").session(sesion.session()))
            .andExpect(status().isOk())
            .andReturn();
        String listado = lista.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(listado).contains("\"estado\":\"COMPLETADA\"");
        assertThat(listado).contains("\"estado\":\"ACTUAL\"");

        // La siguiente leccion queda accesible (detalle y ejercicios).
        mockMvc.perform(get("/api/lecciones/" + leccion2).session(sesion.session()))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/ejercicios/leccion/" + leccion2).session(sesion.session()))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/lecciones/progreso").session(sesion.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completadas").value(1))
            .andExpect(jsonPath("$.leccionActualId").value(leccion2));
    }

    @Test
    void actividadParcialmenteIncorrectaNoCompletaYMarcaLosElementos() throws Exception {
        SesionAutenticada sesion = registrarYLogin("Familia Casi");
        long actividad = primerEjercicioDeLeccion(leccionPorOrden(1));

        Map<String, String> asignaciones = new HashMap<>(solucionDe(actividad));
        // Intercambia dos elementos: dos fallos, el resto bien.
        var entradas = new java.util.ArrayList<>(asignaciones.entrySet());
        String k1 = entradas.get(0).getKey(), v1 = entradas.get(0).getValue();
        String k2 = entradas.get(1).getKey(), v2 = entradas.get(1).getValue();
        if (v1.equals(v2)) {
            asignaciones.put(k1, "destino-inexistente");
        } else {
            asignaciones.put(k1, v2);
            asignaciones.put(k2, v1);
        }

        String cuerpo = responderClasificar(sesion, actividad, asignaciones)
            .getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(cuerpo)
            .contains("\"correcto\":false")
            .contains("\"leccionCompletada\":false");
        var detalle = JSON.readTree(cuerpo).get("detalleItems");
        long fallos = 0;
        for (var d : detalle) {
            if (!d.get("correcto").asBoolean()) fallos++;
            assertThat(d.has("destino")).as("el detalle no revela el destino correcto").isFalse();
        }
        assertThat(fallos).as("marca los elementos incorrectos").isEqualTo(2);

        mockMvc.perform(get("/api/lecciones/progreso").session(sesion.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completadas").value(0));
    }

    @Test
    void actividadConElementosFaltantesEsIncorrecta() throws Exception {
        SesionAutenticada sesion = registrarYLogin("Familia Incompleta");
        long actividad = primerEjercicioDeLeccion(leccionPorOrden(1));

        Map<String, String> parcial = new HashMap<>(solucionDe(actividad));
        // Responde solo la mitad de los elementos.
        var entradas = new java.util.ArrayList<>(parcial.entrySet());
        Map<String, String> incompleta = new HashMap<>();
        for (int i = 0; i < entradas.size() / 2; i++) {
            incompleta.put(entradas.get(i).getKey(), entradas.get(i).getValue());
        }
        assertThat(incompleta.size()).as("la respuesta incompleta cubre menos elementos").isLessThan(parcial.size());

        String cuerpo = responderClasificar(sesion, actividad, incompleta)
            .getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(cuerpo)
            .contains("\"correcto\":false")
            .contains("\"leccionCompletada\":false");
    }

    @Test
    void deteccionDeErroresEvaluaCualquierSeleccionYExigeLaExacta() throws Exception {
        SesionAutenticada sesion = registrarYLogin("Familia Detectiva");
        long leccion1 = leccionPorOrden(1);
        long leccion2 = leccionPorOrden(2);
        long actividad = primerEjercicioDeLeccion(leccion1);
        Map<String, String> solucion = solucionDe(actividad);

        java.util.List<String> problemas = solucion.entrySet().stream()
            .filter(e -> e.getValue().equals("PROBLEMA")).map(Map.Entry::getKey).toList();
        java.util.List<String> noProblemas = solucion.entrySet().stream()
            .filter(e -> e.getValue().equals("OK")).map(Map.Entry::getKey).toList();
        assertThat(problemas).as("la actividad tiene 4 problemas reales").hasSize(4);
        assertThat(noProblemas).as("la actividad tiene 2 situaciones que no son problema").hasSize(2);

        // Protocolo del frontend: marcados -> PROBLEMA, resto -> OK.
        java.util.function.Function<java.util.List<String>, Map<String, String>> seleccion =
            (marcados) -> {
                Map<String, String> r = new HashMap<>();
                solucion.forEach((itemId, destino) ->
                    r.put(itemId, marcados.contains(itemId) ? "PROBLEMA" : "OK"));
                return r;
            };

        java.util.List<String> todos = new java.util.ArrayList<>(problemas);
        todos.add(noProblemas.get(0));
        java.util.List<java.util.List<String>> incorrectas = java.util.List.of(
            problemas.subList(0, 3),                                        // falta un problema
            java.util.List.of(problemas.get(0), noProblemas.get(0)),        // parcial con falso positivo
            java.util.List.of(problemas.get(0), noProblemas.get(0), noProblemas.get(1)),
            noProblemas,                                                    // solo no-problemas
            java.util.List.<String>of(),                                    // ninguna seleccion
            todos                                                           // problemas + un no-problema
        );

        for (java.util.List<String> marcados : incorrectas) {
            String cuerpo = responderClasificar(sesion, actividad, seleccion.apply(marcados))
                .getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            assertThat(cuerpo)
                .as("seleccion %s debe ser incorrecta sin completar la leccion", marcados)
                .contains("\"correcto\":false")
                .contains("\"leccionCompletada\":false")
                .as("el detalle por elemento no revela destinos")
                .doesNotContain("\"destino\"");
            mockMvc.perform(get("/api/lecciones/progreso").session(sesion.session()))
                .andExpect(jsonPath("$.completadas").value(0));
            mockMvc.perform(get("/api/lecciones/" + leccion2).session(sesion.session()))
                .andExpect(status().isUnprocessableEntity());
        }

        // La seleccion exacta de los 4 problemas completa la leccion y desbloquea L2.
        String exacta = responderClasificar(sesion, actividad, seleccion.apply(problemas))
            .getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(exacta)
            .contains("\"correcto\":true")
            .contains("\"leccionCompletada\":true");
        mockMvc.perform(get("/api/lecciones/" + leccion2).session(sesion.session()))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/lecciones/progreso").session(sesion.session()))
            .andExpect(jsonPath("$.completadas").value(1));
    }

    @Test
    void laSolucionNuncaSeExponeEnElListadoDeActividades() throws Exception {
        SesionAutenticada sesion = registrarYLogin("Familia Curiosa");
        long leccion1 = leccionPorOrden(1);
        long leccion2 = leccionPorOrden(2);
        long actividad = primerEjercicioDeLeccion(leccion1);
        // Completa la leccion 1 para desbloquear la 2 (misma ruta que un residente).
        responderClasificar(sesion, actividad, solucionDe(actividad));

        MvcResult lista = mockMvc.perform(get("/api/ejercicios/leccion/" + leccion2).session(sesion.session()))
            .andExpect(status().isOk())
            .andReturn();
        String cuerpo = lista.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        String solucion = jdbcTemplate.queryForObject(
            "SELECT respuesta FROM ejercicio WHERE leccion_id = ? AND activo = 1", String.class, leccion2);
        assertThat(cuerpo)
            .as("el mapa de destinos correctos no debe viajar al cliente")
            .doesNotContain(solucion)
            .doesNotContain("\"respuesta\"");
        // Los items y destinos si deben estar.
        assertThat(cuerpo).contains("\"items\"");
        assertThat(cuerpo).contains("\"destinos\"");
    }

    @Test
    void leccionBloqueadaSigueProtegidaServerSide() throws Exception {
        SesionAutenticada sesion = registrarYLogin("Familia Impaciente");
        long leccion3 = leccionPorOrden(3);
        long ejercicioBloqueado = jdbcTemplate.queryForObject(
            "SELECT id FROM ejercicio WHERE leccion_id = ? AND activo = 1 ORDER BY orden LIMIT 1",
            Long.class, leccion3);

        mockMvc.perform(get("/api/lecciones/" + leccion3).session(sesion.session()))
            .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/api/ejercicios/leccion/" + leccion3).session(sesion.session()))
            .andExpect(status().isUnprocessableEntity());
        String rechazado = responderClasificar(sesion, ejercicioBloqueado, Map.of("x", "y"))
            .getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(rechazado).as("responder sobre bloqueada se rechaza").contains("bloqueada");
        // La respuesta a un ejercicio bloqueado tambien se rechaza: no quedan intentos.
        Integer intentos = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM intento_ejercicio i JOIN ejercicio e ON e.id = i.ejercicio_id "
                + "WHERE e.leccion_id = ?", Integer.class, leccion3);
        assertThat(intentos).as("sin intentos registrados sobre lecciones bloqueadas").isZero();
    }

    @Test
    void progresoAisladoYPersistentePorVivienda() throws Exception {
        SesionAutenticada a = registrarYLogin("Familia A");
        SesionAutenticada b = registrarYLogin("Familia B");
        long actividad = primerEjercicioDeLeccion(leccionPorOrden(1));

        responderClasificar(a, actividad, solucionDe(actividad));

        // B no ve el avance de A.
        mockMvc.perform(get("/api/lecciones/progreso").session(b.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completadas").value(0));

        // A cierra sesion y vuelve: conserva su progreso.
        String email = emailsRegistrados.get("Familia A");
        SesionAutenticada a2 = login(email, "claveSegura1");
        mockMvc.perform(get("/api/lecciones/progreso").session(a2.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completadas").value(1));
    }
}
