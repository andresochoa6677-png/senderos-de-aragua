package com.senderos.comentarios;

import com.senderos.service.CapturaEmailService;
import com.senderos.support.PruebasCorreo;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Módulo de participación: aportes privados del residente asociados a su
 * vivienda (derivada de la sesión), con validaciones de tipo y contenido,
 * privacidad entre viviendas y acceso de investigación solo para ADMIN.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ComentarioIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired CapturaEmailService capturaEmailService;

    private String sufijo() {
        return Long.toHexString(System.nanoTime());
    }

    record SesionAutenticada(MockHttpSession session, Cookie xsrf, String email) {}

    private SesionAutenticada registrarYLogin(String nombre) throws Exception {
        String email = "coment-" + sufijo() + "@gmail.com";
        Cookie xsrf = obtenerXsrf();
        mockMvc.perform(post("/api/onboarding/registro")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType("application/json")
                .content("{\"nombre\":\"" + nombre + "\",\"email\":\"" + email
                    + "\",\"password\":\"claveSegura1\",\"aceptaTratamientoDatos\":true," + com.senderos.support.CatalogoPruebas.campos() + "}"))
            .andExpect(status().isCreated());
        SesionAutenticada sesion = login(email, "claveSegura1");
        return sesion;
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
        return new SesionAutenticada(session, xsrf, email);
    }

    private Long viviendaDe(String email) {
        return jdbcTemplate.queryForObject(
            "SELECT vivienda_id FROM usuario WHERE email = ?", Long.class, email);
    }

    private String cuerpo(String tipo, String tema, String contenido) {
        return "{\"tipo\":\"" + tipo + "\",\"temaDeclarado\":" + (tema == null ? "null" : "\"" + tema + "\"")
            + ",\"contenido\":\"" + contenido + "\"}";
    }

    private Long crearComentario(SesionAutenticada sesion, String tipo, String contenido) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/comentarios")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content(cuerpo(tipo, null, contenido)))
            .andExpect(status().isCreated())
            .andReturn();
        return Long.parseLong(r.getResponse().getContentAsString()
            .replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    @Test
    void residenteAutenticadoCreaComentarioAsociadoASuVivienda() throws Exception {
        SesionAutenticada sesion = registrarYLogin("Familia Participa");
        Long viviendaId = viviendaDe(sesion.email());

        mockMvc.perform(post("/api/comentarios")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content(cuerpo("SUGERENCIA", "PUNTO_RESIDUOS", "Sugiero una señal más grande en el punto")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tipo").value("SUGERENCIA"))
            .andExpect(jsonPath("$.temaDeclarado").value("PUNTO_RESIDUOS"));

        // El registro queda asociado a la vivienda de la sesión, con fecha.
        Map<String, Object> fila = jdbcTemplate.queryForMap(
            "SELECT c.vivienda_id AS vid, c.contenido AS contenido, c.creado_en AS fecha "
                + "FROM comentario c WHERE c.vivienda_id = ?", viviendaId);
        assertThat(fila.get("vid")).isEqualTo(viviendaId);
        assertThat((String) fila.get("contenido")).contains("señal más grande");
        assertThat(fila.get("fecha")).isNotNull();
    }

    @Test
    void sinSesionNoSePuedeEnviar() throws Exception {
        // Con CSRF válido pero SIN sesión: la autenticación rechaza (401).
        Cookie xsrf = obtenerXsrf();
        mockMvc.perform(post("/api/comentarios")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType("application/json")
                .content(cuerpo("SUGERENCIA", null, "comentario sin sesion")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void contenidoVacioOConSoloEspaciosEsRechazado() throws Exception {
        SesionAutenticada sesion = registrarYLogin("Familia Vacia");
        mockMvc.perform(post("/api/comentarios")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content(cuerpo("INQUIETUD", null, "")))
            .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/comentarios")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content(cuerpo("INQUIETUD", null, "      ")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void contenidoMayorA500CaracteresEsRechazado() throws Exception {
        SesionAutenticada sesion = registrarYLogin("Familia Larga");
        String largo = "x".repeat(501);
        mockMvc.perform(post("/api/comentarios")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content(cuerpo("INQUIETUD", null, largo)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void tipoInvalidoYTemasInvalidosSonRechazados() throws Exception {
        SesionAutenticada sesion = registrarYLogin("Familia Tipos");
        mockMvc.perform(post("/api/comentarios")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content(cuerpo("CHISME", null, "comentario de prueba")))
            .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(post("/api/comentarios")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content(cuerpo("INQUIETUD", "TEMA_INVENTADO", "comentario de prueba")))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void residenteNoPuedeManipularLaViviendaDesdeElCliente() throws Exception {
        SesionAutenticada sesion = registrarYLogin("Familia Honesta");
        Long viviendaReal = viviendaDe(sesion.email());

        // El cliente intenta colar viviendaId de OTRA vivienda: el campo no
        // existe en el DTO y se ignora; el aporte queda con la vivienda de sesión.
        mockMvc.perform(post("/api/comentarios")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content("{\"tipo\":\"SUGERENCIA\",\"contenido\":\"prueba manipulacion\",\"viviendaId\":1}"))
            .andExpect(status().isCreated());

        Long viviendaGuardada = jdbcTemplate.queryForObject(
            "SELECT vivienda_id FROM comentario ORDER BY id DESC LIMIT 1", Long.class);
        assertThat(viviendaGuardada).isEqualTo(viviendaReal);
    }

    @Test
    void dobleEnvioRegistraDosAportesIndependientesPorDiseno() throws Exception {
        SesionAutenticada sesion = registrarYLogin("Familia Doble");
        Long viviendaId = viviendaDe(sesion.email());
        crearComentario(sesion, "PERCEPCION", "Primera percepción del proyecto");
        crearComentario(sesion, "PERCEPCION", "Segunda percepción del proyecto");

        Integer filas = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM comentario WHERE vivienda_id = ?", Integer.class, viviendaId);
        // Sin deduplicación por diseño: cada envío intencional es un aporte
        // independiente para el análisis (el frontend previene doble clic).
        assertThat(filas).isEqualTo(2);
    }

    @Test
    void misAportesSoloMuestraLosDeLaPropiaVivienda() throws Exception {
        SesionAutenticada a = registrarYLogin("Familia Mis A");
        SesionAutenticada b = registrarYLogin("Familia Mis B");
        Long viviendaA = viviendaDe(a.email());
        crearComentario(a, "SITUACION", "Situación de la vivienda A");
        crearComentario(a, "SITUACION", "Segunda situación de la vivienda A");

        MvcResult mios = mockMvc.perform(get("/api/comentarios/mios").session(a.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andReturn();
        assertThat(mios.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
            .contains("Situación de la vivienda A");

        // B no ve ninguno de A.
        mockMvc.perform(get("/api/comentarios/mios").session(b.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        // El residente no puede usar la ruta de investigación.
        mockMvc.perform(get("/api/admin/comentarios").session(a.session()))
            .andExpect(status().isForbidden());
    }

    @Test
    void historialDeInvestigacionSoloParaAdmin() throws Exception {
        SesionAutenticada sesion = registrarYLogin("Familia Sin Admin");
        mockMvc.perform(get("/api/admin/comentarios").session(sesion.session()))
            .andExpect(status().isForbidden());
    }

    @Test
    void contenidoSeAlmacenaTalCualFueRegistrado() throws Exception {
        SesionAutenticada sesion = registrarYLogin("Familia Texto");
        String contenido = "  Percepción con MAYÚSCULAS, acentos y <etiquetas>  ";
        MvcResult r = mockMvc.perform(post("/api/comentarios")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content(cuerpo("PERCEPCION", null, contenido)))
            .andExpect(status().isCreated())
            .andReturn();

        // La respuesta devuelve el contenido YA recortado (trim del servicio).
        assertThat(r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
            .contains("Percepción con MAYÚSCULAS, acentos y <etiquetas>");

        // Se recorta el espacio inicial/final y se conserva el texto literal
        // (sin interpretar HTML); el frontend lo renderiza como texto.
        String almacenado = jdbcTemplate.queryForObject(
            "SELECT contenido FROM comentario ORDER BY id DESC LIMIT 1", String.class);
        assertThat(almacenado).isEqualTo("Percepción con MAYÚSCULAS, acentos y <etiquetas>");
    }
}
