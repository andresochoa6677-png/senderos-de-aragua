package com.senderos.seguridad;

import com.senderos.service.CapturaEmailService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Auditoría 2026-09-12 (A-1, A-2, A-9): tasa límite de los endpoints
 * públicos de escritura.
 *
 * Contexto PROPIO con umbral 6 (las suites generales comparten otro
 * contexto con umbral alto). Cada caso usa IPs remotas distintas porque
 * el RateLimiter es un bean del contexto y sus contadores persisten
 * entre métodos de la misma clase.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "app.rate-limiter.max-intentos=6",
    "app.rate-limiter.ventana-minutos=10"
})
class RateLimitIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired CapturaEmailService capturaEmailService;

    private String sufijo() {
        return Long.toHexString(System.nanoTime());
    }

    private Cookie xsrf() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        return csrf.getResponse().getCookie("XSRF-TOKEN");
    }

    /** Simula peticiones desde IPs distintas (MockMvc siempre usa 127.0.0.1). */
    private MockHttpServletRequestBuilder desde(MockHttpServletRequestBuilder peticion, String ip) {
        return peticion.with(req -> {
            req.setRemoteAddr(ip);
            return req;
        });
    }

    private String cuerpoRegistro(String email) {
        return "{\"nombre\":\"Familia Rate\",\"email\":\"" + email
            + "\",\"password\":\"claveSegura1\",\"aceptaTratamientoDatos\":true,"
            + com.senderos.support.CatalogoPruebas.campos() + "}";
    }

    private String registrar(String email, String ip) throws Exception {
        Cookie token = xsrf();
        MvcResult registro = mockMvc.perform(desde(post("/api/onboarding/registro")
                .cookie(token)
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType("application/json")
                .content(cuerpoRegistro(email)), ip))
            .andReturn();
        return registro.getResponse().getContentAsString();
    }

    /** A-1: sin límite, un script agota el catálogo de viviendas; con él, 429. */
    @Test
    void registroSeBloqueaAlSuperarLaTasaPorIp() throws Exception {
        String ip = "10.1.0.1";
        for (int i = 1; i <= 6; i++) {
            String cuerpo = registrar("rate-reg-" + sufijo() + "-" + i + "@test.senderos", ip);
            Object viviendaId = com.jayway.jsonpath.JsonPath.read(cuerpo, "$.usuario.viviendaId");
            assertThat(viviendaId).isNotNull();
        }
        Cookie token = xsrf();
        mockMvc.perform(desde(post("/api/onboarding/registro")
                .cookie(token)
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType("application/json")
                .content(cuerpoRegistro("rate-reg-" + sufijo() + "-7@test.senderos")), ip))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error").exists());
    }

    /** A-2: agotada la tasa de la CUENTA, la IP fresca no ayuda ni la contraseña correcta. */
    @Test
    void cuentaAgotadaSeBloqueaAunRotandoIp() throws Exception {
        String email = "rate-login-" + sufijo() + "@test.senderos";
        registrar(email, "10.1.1.1");

        for (int i = 1; i <= 6; i++) {
            Cookie token = xsrf();
            mockMvc.perform(desde(post("/api/auth/login")
                    .cookie(token)
                    .header("X-XSRF-TOKEN", token.getValue())
                    .contentType("application/json")
                    .content("{\"email\":\"" + email + "\",\"password\":\"incorrecta-" + i + "\"}"), "10.1.1.2"))
                .andExpect(status().isUnauthorized());
        }
        Cookie token = xsrf();
        mockMvc.perform(desde(post("/api/auth/login")
                .cookie(token)
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"claveSegura1\"}"), "10.1.1.3"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error").exists());
    }

    /** A-2 (contracara): otra cuenta desde IP fresca sigue pudiendo intentarlo. */
    @Test
    void otraCuentaConIpFrescaNoHeredaElBloqueo() throws Exception {
        Cookie token = xsrf();
        mockMvc.perform(desde(post("/api/auth/login")
                .cookie(token)
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType("application/json")
                .content("{\"email\":\"nadie-" + sufijo() + "@test.senderos\",\"password\":\"loQueSea-123\"}"),
                "10.1.1.4"))
            .andExpect(status().isUnauthorized());
    }

    /** A-9: restablecer consume trabajo (BCrypt) y tiene su propia tasa. */
    @Test
    void restablecerSeBloqueaAlSuperarLaTasa() throws Exception {
        String ip = "10.1.2.1";
        for (int i = 1; i <= 6; i++) {
            Cookie token = xsrf();
            mockMvc.perform(desde(post("/api/password/restablecer")
                    .cookie(token)
                    .header("X-XSRF-TOKEN", token.getValue())
                    .contentType("application/json")
                    .content("{\"token\":\"vacio-" + i + "\",\"nuevaPassword\":\"Clave-Fuerte-2026\"}"), ip))
                .andExpect(status().isUnprocessableEntity());
        }
        Cookie token = xsrf();
        mockMvc.perform(desde(post("/api/password/restablecer")
                .cookie(token)
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType("application/json")
                .content("{\"token\":\"vacio-7\",\"nuevaPassword\":\"Clave-Fuerte-2026\"}"), ip))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.mensaje").exists());
    }

    /** A-9: la activación de cuenta también queda limitada por IP. */
    @Test
    void verificarCorreoSeBloqueaAlSuperarLaTasa() throws Exception {
        String ip = "10.1.2.2";
        for (int i = 1; i <= 6; i++) {
            Cookie token = xsrf();
            mockMvc.perform(desde(post("/api/auth/verificar-correo")
                    .cookie(token)
                    .header("X-XSRF-TOKEN", token.getValue())
                    .contentType("application/json")
                    .content("{\"token\":\"vacio-" + i + "\"}"), ip))
                .andExpect(status().isUnprocessableEntity());
        }
        Cookie token = xsrf();
        mockMvc.perform(desde(post("/api/auth/verificar-correo")
                .cookie(token)
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType("application/json")
                .content("{\"token\":\"vacio-7\"}"), ip))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.mensaje").exists());
    }

    /** A-9 (movido de VerificacionEmailIntegrationTest): el reenvío del enlace tiene tasa propia. */
    @Test
    void reenvioSeBloqueaAlSuperarLaTasa() throws Exception {
        String ip = "10.1.2.3";
        for (int i = 1; i <= 6; i++) {
            Cookie token = xsrf();
            mockMvc.perform(desde(post("/api/auth/reenviar-verificacion")
                    .cookie(token)
                    .header("X-XSRF-TOKEN", token.getValue())
                    .contentType("application/json")
                    .content("{\"email\":\"nadie-" + i + "-test.senderos\"}"), ip))
                .andExpect(status().isOk()); // genérico: no revela existencia
        }
        Cookie token = xsrf();
        mockMvc.perform(desde(post("/api/auth/reenviar-verificacion")
                .cookie(token)
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType("application/json")
                .content("{\"email\":\"nadie-7-test.senderos\"}"), ip))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.mensaje").exists());
    }
}
