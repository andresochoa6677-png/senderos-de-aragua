package com.senderos.auth;

import com.senderos.security.ActiveSessionStore;
import com.senderos.service.PasswordResetService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integracion del nucleo de autenticacion: login, logout, CSRF,
 * roles y reset de contrasena. MockMvc no emite cookies JSESSIONID, por lo
 * que la sesion HTTP real se transfiere entre peticiones via MockHttpSession.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ActiveSessionStore activeSessionStore;
    @Autowired com.senderos.support.TokensDePrueba tokensDePrueba;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private static final String DEMO = "demo@senderos.local";
    private static final String DEMO_PASS = "demo1234";

    /** Obtiene token CSRF desde la cookie XSRF-TOKEN emitida por /api/auth/csrf. */
    private String obtenerCsrfToken(MvcResult resultadoGetCsrf) {
        for (Cookie cookie : resultadoGetCsrf.getResponse().getCookies()) {
            if ("XSRF-TOKEN".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        throw new IllegalStateException("No se emitio la cookie XSRF-TOKEN");
    }

    /** Sesion con login ya hecho + cookie XSRF-TOKEN correspondiente. */
    record SesionAutenticada(MockHttpSession session, Cookie xsrf) {}

    private SesionAutenticada login() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        Cookie xsrf = csrf.getResponse().getCookie("XSRF-TOKEN");
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType("application/json")
                .content("{\"email\":\"" + DEMO + "\",\"password\":\"" + DEMO_PASS + "\"}"))
            .andExpect(status().isOk())
            .andReturn();
        MockHttpSession session = (MockHttpSession) resultado.getRequest().getSession(false);
        assertThat(session).as("el login debe crear sesion HTTP real").isNotNull();
        return new SesionAutenticada(session, xsrf);
    }

    private MvcResult loginConPassword(String password) throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        Cookie xsrf = csrf.getResponse().getCookie("XSRF-TOKEN");
        return mockMvc.perform(post("/api/auth/login")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType("application/json")
                .content("{\"email\":\"" + DEMO + "\",\"password\":\"" + password + "\"}"))
            .andReturn();
    }

    // ---------- LOGIN ----------

    @Test
    void loginConCredencialesValidasRetornaUsuarioYSesion() throws Exception {
        SesionAutenticada sesion = login();

        mockMvc.perform(get("/api/auth/me").session(sesion.session()))
            .andExpect(status().isOk());

        assertThat(activeSessionStore.cantidadSesiones(DEMO)).isEqualTo(1);
    }

    @Test
    void loginConCredencialesInvalidasRetorna401() throws Exception {
        MvcResult resultado = loginConPassword("incorrecta");
        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
    }

    // ---------- LOGOUT ----------

    @Test
    void logoutInvalidaLaSesionYMeRetorna401() throws Exception {
        SesionAutenticada sesion = login();
        mockMvc.perform(post("/api/auth/logout").session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue()))
            .andExpect(status().isNoContent());

        // La sesion ya no sirve: /me debe responder 401.
        mockMvc.perform(get("/api/auth/me").session(sesion.session()))
            .andExpect(status().isUnauthorized());
    }

    // ---------- CSRF ----------

    @Test
    void postSinHeaderCsrfEsRechazado() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"" + DEMO + "\",\"password\":\"" + DEMO_PASS + "\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void postConHeaderCsrfInvalidoEsRechazado() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andReturn();

        mockMvc.perform(post("/api/auth/login")
                .cookie(csrf.getResponse().getCookie("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", "token-falsificado")
                .contentType("application/json")
                .content("{\"email\":\"" + DEMO + "\",\"password\":\"" + DEMO_PASS + "\"}"))
            .andExpect(status().isForbidden());
    }

    // ---------- ROLES ----------

    @Test
    void residenteNoPuedeAccederAEndpointDeAdmin() throws Exception {
        SesionAutenticada sesion = login();
        mockMvc.perform(get("/api/admin/sesiones").session(sesion.session()))
            .andExpect(status().isForbidden());
    }

    @Test
    void endpointAutenticadoSinSesionRetorna401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    // ---------- RESET DE CONTRASENA ----------

    @Test
    void resetDeContrasenaInvalidaSesionesActivasInmediatamente() throws Exception {
        SesionAutenticada sesion = login();
        mockMvc.perform(get("/api/auth/me").session(sesion.session()))
            .andExpect(status().isOk());

        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        String token = obtenerCsrfToken(csrf);

        // El token viaja por correo; en tests lo generamos directamente desde el servicio.
        String tokenReal = generarTokenPara(DEMO);
        mockMvc.perform(post("/api/password/restablecer")
                .cookie(csrf.getResponse().getCookie("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", token)
                .contentType("application/json")
                .content("{\"token\":\"" + tokenReal + "\",\"nuevaPassword\":\"nuevaClave99\"}"))
            .andExpect(status().isOk());

        // La sesion anterior DEBE estar muerta inmediatamente.
        mockMvc.perform(get("/api/auth/me").session(sesion.session()))
            .andExpect(status().isUnauthorized());
        assertThat(activeSessionStore.cantidadSesiones(DEMO)).isZero();

        // Restaura la contrasena original para no afectar otros tests. La
        // restauracion va por BD: la semilla demo1234 (V2) es deliberadamente
        // debil y desde A-3 la API rechaza fijarla de nuevo.
        restaurarPasswordDemo();
    }

    @Test
    void cambioDeContrasenaAutenticadoInvalidaSesion() throws Exception {
        SesionAutenticada sesion = login();

        mockMvc.perform(post("/api/auth/password")
                .session(sesion.session())
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content("{\"passwordActual\":\"" + DEMO_PASS + "\",\"passwordNueva\":\"otraClave77\"}"))
            .andExpect(status().isOk());

        // La sesion anterior muere inmediatamente y no sobrevive peticiones.
        mockMvc.perform(get("/api/auth/me").session(sesion.session()))
            .andExpect(status().isUnauthorized());
        assertThat(activeSessionStore.cantidadSesiones(DEMO)).isZero();

        // La nueva contrasena funciona para volver a entrar.
        MvcResult relogin = mockMvc.perform(post("/api/auth/login")
                .cookie(sesion.xsrf())
                .header("X-XSRF-TOKEN", sesion.xsrf().getValue())
                .contentType("application/json")
                .content("{\"email\":\"" + DEMO + "\",\"password\":\"otraClave77\"}"))
            .andExpect(status().isOk()).andReturn();
        MockHttpSession nuevaSesion = (MockHttpSession) relogin.getRequest().getSession(false);
        assertThat(nuevaSesion).isNotNull();

        // Restaura la contrasena original por BD: la API ya no acepta fijar
        // la semilla demo1234 (politica A-3), y ese es exactamente el
        // comportamiento esperado.
        restaurarPasswordDemo();
    }

    // ---------- helpers ----------

    /** Devuelve la contrasena de la semilla demo al valor de V2 a nivel BD. */
    private void restaurarPasswordDemo() {
        jdbcTemplate.update("UPDATE usuario SET password_hash = ? WHERE email = ?",
            passwordEncoder.encode(DEMO_PASS), DEMO);
    }

    private Cookie obtenerCookie(MvcResult resultado, String nombre) {
        for (Cookie cookie : resultado.getResponse().getCookies()) {
            if (nombre.equals(cookie.getName())) {
                return cookie;
            }
        }
        return null;
    }

    private String generarTokenPara(String email) {
        return tokensDePrueba.crear(email);
    }
}
