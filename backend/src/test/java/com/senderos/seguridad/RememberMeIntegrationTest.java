package com.senderos.seguridad;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * "Mantener sesión activa" (V27): al iniciar sesión con recordar=true el
 * backend emite la cookie persistente REMEMBERME (token rotativo guardado
 * como hash en persistent_logins). Con esa cookie —aunque la sesión HTTP
 * haya expirado o el servidor se haya reiniciado— el residente queda
 * re-autenticado automáticamente. Cambiar la contraseña o cerrar sesión
 * invalida el token.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RememberMeIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;

    private Cookie xsrf() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        return csrf.getResponse().getCookie("XSRF-TOKEN");
    }

    record SesionRemember(String email, String rememberCookie, MockHttpSession session) {}

    private SesionRemember registrarYLogin(boolean recordar) throws Exception {
        String email = "remember-" + Long.toHexString(System.nanoTime()) + "@senderos.local";
        Cookie t = xsrf();
        mockMvc.perform(post("/api/onboarding/registro")
                .cookie(t)
                .header("X-XSRF-TOKEN", t.getValue())
                .contentType("application/json")
                .content("{\"nombre\":\"Familia Remember\",\"email\":\"" + email
                    + "\",\"password\":\"claveSegura1\",\"aceptaTratamientoDatos\":true,"
                    + com.senderos.support.CatalogoPruebas.campos() + "}"))
            .andExpect(status().isCreated());
        Cookie t2 = xsrf();
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                .cookie(t2)
                .header("X-XSRF-TOKEN", t2.getValue())
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"claveSegura1\",\"recordar\":" + recordar + "}"))
            .andExpect(status().isOk())
            .andReturn();
        Cookie rememberCookie = login.getResponse().getCookie("REMEMBERME");
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        return new SesionRemember(email, rememberCookie == null ? "" : rememberCookie.getValue(), session);
    }

    @Test
    void loginConRecordarReautenticaSinSesionHttp() throws Exception {
        SesionRemember sesion = registrarYLogin(true);
        String email = sesion.email(), rememberCookie = sesion.rememberCookie();
        assertThat(rememberCookie).as("la cookie persistente se emite").isNotBlank();
        assertThat(sesion.session()).isNotNull();

        // Nueva petición SIN la sesión HTTP (expiró / servidor reiniciado):
        // la cookie REMEMBERME re-autentica sola.
        mockMvc.perform(get("/api/auth/me").cookie(new Cookie("REMEMBERME", rememberCookie)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email));

        // El token vive como registro de dispositivo (rotativo).
        Integer tokens = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM persistent_logins WHERE username = ?", Integer.class, email);
        assertThat(tokens).as("el dispositivo queda registrado").isEqualTo(1);
    }

    @Test
    void loginSinRecordarNoEmiteCookiePersistente() throws Exception {
        SesionRemember sesion = registrarYLogin(false);
        assertThat(sesion.rememberCookie()).as("sin recordar no hay cookie REMEMBERME").isBlank();
        assertThat(sesion.session()).as("la sesión HTTP normal sí existe").isNotNull();

        Integer tokens = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM persistent_logins WHERE username = ?", Integer.class, sesion.email());
        assertThat(tokens).isZero();
    }

    @Test
    void cambiarPasswordInvalidaElRecuerdame() throws Exception {
        SesionRemember sesion = registrarYLogin(true);
        String email = sesion.email(), rememberCookie = sesion.rememberCookie();
        MockHttpSession session = sesion.session();

        Cookie t = xsrf();
        mockMvc.perform(post("/api/auth/password")
                .session(session)
                .cookie(t)
                .header("X-XSRF-TOKEN", t.getValue())
                .contentType("application/json")
                .content("{\"passwordActual\":\"claveSegura1\",\"passwordNueva\":\"otra-clave-larga-456\"}"))
            .andExpect(status().isOk());

        // El token del dispositivo se borró: la cookie vieja ya no autentica.
        mockMvc.perform(get("/api/auth/me").cookie(new Cookie("REMEMBERME", rememberCookie)))
            .andExpect(status().isUnauthorized());
        Integer tokens = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM persistent_logins WHERE username = ?", Integer.class, email);
        assertThat(tokens).isZero();
    }

    @Test
    void logoutLimpiaLaCookieYElToken() throws Exception {
        SesionRemember sesion = registrarYLogin(true);
        String email = sesion.email(), rememberCookie = sesion.rememberCookie();
        MockHttpSession session = sesion.session();

        Cookie t = xsrf();
        MvcResult logout = mockMvc.perform(post("/api/auth/logout")
                .session(session)
                .cookie(t, new Cookie("REMEMBERME", rememberCookie))
                .header("X-XSRF-TOKEN", t.getValue()))
            .andExpect(status().isNoContent())
            .andReturn();

        assertThat(String.valueOf(logout.getResponse().getHeader("Set-Cookie")))
            .as("la cookie REMEMBERME se elimina en el logout").contains("REMEMBERME");
        mockMvc.perform(get("/api/auth/me").cookie(new Cookie("REMEMBERME", rememberCookie)))
            .andExpect(status().isUnauthorized());
        Integer tokens = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM persistent_logins WHERE username = ?", Integer.class, email);
        assertThat(tokens).isZero();
    }
}
