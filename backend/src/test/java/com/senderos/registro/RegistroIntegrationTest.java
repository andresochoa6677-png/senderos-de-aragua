package com.senderos.registro;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests del flujo de registro de residentes con SELECCIÓN MANUAL de
 * vivienda: el residente escoge bloque y número (censo oficial
 * BLOQUE-NUMERO de V25) y el backend valida existencia y disponibilidad.
 * La garantía UNA VIVIENDA -> MAXIMO UNA CUENTA la impone la base de
 * datos (uk_usuario_vivienda, V11): dos registros simultáneos sobre la
 * misma vivienda producen un ganador (201) y un perdedor (409) con
 * mensaje claro.
 *
 * Los códigos "reservados" para colisiones (Ñ-4, Z-9) viven al FINAL del
 * censo de {@link com.senderos.support.CatalogoPruebas}: la suite nunca
 * los alcanza por rotación, así que su ocupación es determinista aquí.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistroIntegrationTest {

    private static final String MENSAJE_OCUPADA = "La vivienda Ñ-3 ya está registrada. "
        + "Escoge otra vivienda, o comunícate con el equipo investigador si crees que es un error";

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired com.senderos.service.CapturaEmailService capturaEmailService;

    private String sufijo() {
        return Long.toHexString(System.nanoTime());
    }

    private Cookie obtenerXsrf() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        return csrf.getResponse().getCookie("XSRF-TOKEN");
    }

    /** Cuerpo de registro con una vivienda concreta del censo. */
    private String cuerpoEn(String nombre, String email, String codigo) {
        return "{\"nombre\":\"" + nombre + "\",\"email\":\"" + email + "\","
            + "\"password\":\"claveSegura1\",\"aceptaTratamientoDatos\":true,"
            + com.senderos.support.CatalogoPruebas.camposDe(codigo) + "}";
    }

    /** Registra una cuenta con email único y vivienda rotatoria del censo. */
    private String registrarCuenta(String nombre) throws Exception {
        String email = "reg-" + sufijo() + "@senderos.local";
        Cookie xsrf = obtenerXsrf();
        MvcResult registro = mockMvc.perform(post("/api/onboarding/registro")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType("application/json")
                .content("{\"nombre\":\"" + nombre + "\",\"email\":\"" + email + "\","
                    + "\"password\":\"claveSegura1\",\"aceptaTratamientoDatos\":true,"
                    + com.senderos.support.CatalogoPruebas.campos() + "}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.usuario.viviendaId").isNumber())
            .andExpect(jsonPath("$.correoEnviado").value(true))
            .andReturn();
        String cuerpo = registro.getResponse().getContentAsString();
        String codigo = com.jayway.jsonpath.JsonPath.read(cuerpo, "$.usuario.viviendaCodigo");
        assertThat(codigo).as("la cuenta debe quedar asociada a una vivienda").isNotBlank();
        return codigo;
    }

    @Test
    void registroConViviendaElegidaQuedaConElCodigoOficial() throws Exception {
        String email = "eleccion-" + sufijo() + "@senderos.local";
        Cookie xsrf = obtenerXsrf();
        MvcResult registro = mockMvc.perform(post("/api/onboarding/registro")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType("application/json")
                .content(cuerpoEn("Familia Elección", email, "G-5")))
            .andExpect(status().isCreated())
            .andReturn();
        String cuerpo = registro.getResponse().getContentAsString();
        Number viviendaId = com.jayway.jsonpath.JsonPath.read(cuerpo, "$.usuario.viviendaId");
        String codigo = com.jayway.jsonpath.JsonPath.read(cuerpo, "$.usuario.viviendaCodigo");

        // El código de la vivienda es EXACTAMENTE el elegido (formato oficial).
        assertThat(codigo).as("el residente recibe la vivienda que escogió").isEqualTo("G-5");

        // La vivienda proviene del censo (bloque y número guardados) y no es DEMO.
        var fila = jdbcTemplate.queryForMap(
            "SELECT demo, activa, bloque, numero FROM vivienda WHERE id = ?", viviendaId);
        assertThat(((Number) fila.get("demo")).intValue()).isZero();
        assertThat((String) fila.get("bloque")).isEqualTo("G");
        assertThat(((Number) fila.get("numero")).intValue()).isEqualTo(5);

        // Desde REG-2.0 el registro NO auto-acepta el consentimiento: la
        // aceptación firmada (IP + hash) ocurre en la pantalla del onboarding.
        Integer consentimientos = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM consentimiento c JOIN usuario u ON u.vivienda_id = c.vivienda_id "
                + "WHERE u.email = ? AND c.aceptado = 1",
            Integer.class, email);
        assertThat(consentimientos).as("el registro no debe auto-aceptar el consentimiento").isZero();

        // La cuenta nace pendiente: se verifica por el enlace del correo (como el residente).

        // Con la cuenta verificada, el onboarding arranca en el CONSENTIMIENTO.
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"claveSegura1\"}"))
            .andExpect(status().isOk())
            .andReturn();
        var sesion = (org.springframework.mock.web.MockHttpSession) login.getRequest().getSession(false);
        mockMvc.perform(get("/api/onboarding/estado").session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.consentimientoAceptado").value(false))
            .andExpect(jsonPath("$.siguientePaso").value("CONSENTIMIENTO"));
    }

    @Test
    void dosRegistrosConViviendasDistintasAmbosExitosos() throws Exception {
        String codigoA = registrarCuenta("Familia Una");
        String codigoB = registrarCuenta("Familia Dos");
        assertThat(codigoA).isNotEqualTo(codigoB);
    }

    @Test
    void viviendaOcupadaRechazadaConMensajeClaro() throws Exception {
        // Ocupa Ñ-4 con la primera cuenta…
        String email = "ocupante-" + sufijo() + "@senderos.local";
        Cookie xsrf = obtenerXsrf();
        mockMvc.perform(post("/api/onboarding/registro")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType("application/json")
                .content(cuerpoEn("Familia Ocupante", email, "Ñ-3")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.usuario.viviendaCodigo").value("Ñ-3"));

        // …y un segundo residente que la escoge recibe 409 con el mensaje claro.
        mockMvc.perform(post("/api/onboarding/registro")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType("application/json")
                .content(cuerpoEn("Familia Tardía", "tardia-" + sufijo() + "@senderos.local", "Ñ-3")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value(MENSAJE_OCUPADA));
    }

    @Test
    void viviendaFueraDelCensoRechazada() throws Exception {
        Cookie xsrf = obtenerXsrf();
        // El bloque A solo tiene 4 casas: A-5 no existe en el censo.
        mockMvc.perform(post("/api/onboarding/registro")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType("application/json")
                .content(cuerpoEn("Familia Fuera", "fuera-a5-" + sufijo() + "@senderos.local", "A-5")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error").value(
                "La vivienda A-5 no existe en el catálogo del condominio. Revisa el bloque y el número"));

        // Un bloque que no pertenece al censo tampoco.
        mockMvc.perform(post("/api/onboarding/registro")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType("application/json")
                .content(cuerpoEn("Familia Fuera 2", "fuera-zz-" + sufijo() + "@senderos.local", "ZZ-1")))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void laViviendaDemoNoEsElegible() throws Exception {
        // DEMO-001 no tiene bloque ni número (no pertenece al censo
        // residencial): cualquier intento de elegirla cae fuera del catálogo.
        Integer demoConBloque = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM vivienda WHERE demo = 1 AND bloque IS NOT NULL", Integer.class);
        assertThat(demoConBloque).as("la vivienda demo no hace parte del censo elegible").isZero();

        Cookie xsrf = obtenerXsrf();
        mockMvc.perform(post("/api/onboarding/registro")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType("application/json")
                .content("{\"nombre\":\"Familia Demo\",\"email\":\"demo-elegida-" + sufijo()
                    + "@senderos.local\",\"password\":\"claveSegura1\",\"aceptaTratamientoDatos\":true,"
                    + "\"bloque\":\"DEMO\",\"numero\":1}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void correoDuplicadoRechazadoConMensajeClaro() throws Exception {
        String email = "duplicado-" + sufijo() + "@senderos.local";
        Cookie xsrf = obtenerXsrf();
        mockMvc.perform(post("/api/onboarding/registro")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType("application/json")
                .content(cuerpoEn("Familia Original", email, "Z-9")))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/onboarding/registro")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType("application/json")
                .content(cuerpoEn("Familia Repetida", email, "Z-9")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value(
                "Esta plataforma utiliza una cuenta por vivienda. Si tu vivienda ya tiene una cuenta registrada, "
                    + "inicia sesión con esa cuenta o utiliza la recuperación de contraseña."));
    }

    @Test
    void aceptacionDeTratamientoDeDatosEsObligatoria() throws Exception {
        Cookie xsrf = obtenerXsrf();
        String email = "sin-consent-" + sufijo() + "@senderos.local";
        mockMvc.perform(post("/api/onboarding/registro")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType("application/json")
                .content("{\"nombre\":\"Familia Sin Consentimiento\",\"email\":\"" + email + "\","
                    + "\"password\":\"claveSegura1\",\"aceptaTratamientoDatos\":false,"
                    + com.senderos.support.CatalogoPruebas.campos() + "}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void registrosSimultaneosConViviendasDistintasNoRepitenVivienda() throws Exception {
        int registros = 6;
        ExecutorService pool = Executors.newFixedThreadPool(registros);
        try {
            List<Future<String>> futuros = new ArrayList<>();
            for (int i = 0; i < registros; i++) {
                String nombre = "Familia Concurrente " + i;
                futuros.add(pool.submit((Callable<String>) () -> registrarCuenta(nombre)));
            }
            List<String> codigos = new ArrayList<>();
            for (Future<String> futuro : futuros) {
                codigos.add(futuro.get(60, TimeUnit.SECONDS));
            }
            assertThat(codigos).hasSize(registros)
                .as("cada registro simultáneo queda en la vivienda que escogió, sin duplicados")
                .doesNotHaveDuplicates();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void carreraPorLaMismaViviendaProduceUnGanadorY409ConMensaje() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> intentoConEmail = (Callable<Integer>) () -> {
                String email = "carrera-v-" + sufijo() + "-" + Thread.currentThread().getId() + "@senderos.local";
                Cookie xsrf = obtenerXsrf();
                return mockMvc.perform(post("/api/onboarding/registro")
                        .cookie(xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue())
                        .contentType("application/json")
                        .content(cuerpoEn("Familia Carrera Vivienda", email, "Ñ-4")))
                    .andReturn().getResponse().getStatus();
            };
            Future<Integer> primero = pool.submit(intentoConEmail);
            Future<Integer> segundo = pool.submit(intentoConEmail);
            int statusA = primero.get(60, TimeUnit.SECONDS);
            int statusB = segundo.get(60, TimeUnit.SECONDS);
            assertThat(new HashSet<>(List.of(statusA, statusB)))
                .as("dos residentes que escogen la misma vivienda a la vez: 201 y 409")
                .containsExactlyInAnyOrder(201, 409);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void carreraConMismoEmailProduceUnaSolaCuenta() throws Exception {
        String email = "carrera-" + sufijo() + "@senderos.local";
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> intento = () -> {
                Cookie xsrf = obtenerXsrf();
                return mockMvc.perform(post("/api/onboarding/registro")
                        .cookie(xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue())
                        .contentType("application/json")
                        .content(cuerpoEn("Familia Carrera", email, "Z-8")))
                    .andReturn().getResponse().getStatus();
            };
            Future<Integer> primero = pool.submit(intento);
            Future<Integer> segundo = pool.submit(intento);
            int statusA = primero.get(60, TimeUnit.SECONDS);
            int statusB = segundo.get(60, TimeUnit.SECONDS);
            assertThat(new HashSet<>(List.of(statusA, statusB)))
                .as("debe existir exactamente una cuenta creada (201) y el otro intento conflictuar (409)")
                .containsExactlyInAnyOrder(201, 409);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void laRestriccionUnicaExisteEnBaseDeDatos() {
        // Toma una vivienda que YA tiene una cuenta y prueba insertar una
        // segunda: el indice unico uk_usuario_vivienda (V11) debe rechazarlo.
        Long viviendaId = jdbcTemplate.queryForObject(
            "SELECT v.id FROM vivienda v JOIN usuario u ON u.vivienda_id = v.id LIMIT 1",
            Long.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO usuario (email, password_hash, nombre, enabled, demo, vivienda_id, creado_en, actualizado_en) "
                + "VALUES ('db-dupe-" + sufijo() + "@senderos.local', 'x', 'Dupe', 1, 0, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            viviendaId))
            .as("la BD debe rechazar una segunda cuenta sobre la misma vivienda")
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
