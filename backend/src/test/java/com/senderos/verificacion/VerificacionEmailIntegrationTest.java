package com.senderos.verificacion;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo completo de verificación de correo: registro pendiente, token
 * seguro de un solo uso, activación, login antes/después, reenvío con
 * invalidación y rate limit, y recuperación de contraseña por correo
 * con respuestas genéricas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VerificacionEmailIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired CapturaEmailService capturaEmailService;
    @Autowired com.senderos.service.VerificacionEmailService verificacionEmailService;

    /**
     * El registro ya nace verificado: para probar el flujo de tokens (que
     * sigue vigente como código y endpoints) la cuenta se vuelve PENDIENTE
     * y se genera su token directamente con el servicio.
     */
    private void volverPendienteConToken(String email) {
        jdbcTemplate.update("UPDATE usuario SET email_verificado = 0 WHERE email = ?", email);
        verificacionEmailService.crearYEnviar(email);
    }

    private String sufijo() {
        return Long.toHexString(System.nanoTime());
    }

    private Cookie xsrf() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        return csrf.getResponse().getCookie("XSRF-TOKEN");
    }

    /** Registra una vivienda y devuelve el email normalizado. */
    private String registrar(String nombre) throws Exception {
        String email = "verif-" + sufijo() + "@gmail.com";
        Cookie cookie = xsrf();
        mockMvc.perform(post("/api/onboarding/registro")
                .cookie(cookie)
                .header("X-XSRF-TOKEN", cookie.getValue())
                .contentType("application/json")
                .content("{\"nombre\":\"" + nombre + "\",\"email\":\"" + email
                    + "\",\"password\":\"claveSegura1\",\"aceptaTratamientoDatos\":true," + com.senderos.support.CatalogoPruebas.campos() + "}"))
            .andExpect(status().isCreated());
        return email;
    }

    private void login(String email, String resultado) throws Exception {
        Cookie cookie = xsrf();
        mockMvc.perform(post("/api/auth/login")
                .cookie(cookie)
                .header("X-XSRF-TOKEN", cookie.getValue())
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"claveSegura1\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$." + resultado).exists());
    }

    @Test
    void registroNaceVerificadoSinCorreoDeVerificacion() throws Exception {
        String email = registrar("Familia Directa");

        Long viviendaId = jdbcTemplate.queryForObject(
            "SELECT vivienda_id FROM usuario WHERE email = ?", Long.class, email);
        assertThat(viviendaId).isNotNull();

        // La cuenta nace ACTIVA: sin paso de correo para entrar.
        Integer verificado = jdbcTemplate.queryForObject(
            "SELECT email_verificado FROM usuario WHERE email = ?", Integer.class, email);
        assertThat(verificado).as("la cuenta nace verificada").isEqualTo(1);
        Integer conFecha = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM usuario WHERE email = ? AND fecha_verificacion IS NOT NULL",
            Integer.class, email);
        assertThat(conFecha).isEqualTo(1);
        assertThat(capturaEmailService.capturado("verificacion", email))
            .as("el registro ya no envía correo de verificación").isFalse();

        // Login inmediato sin paso previo: 200 con sesión real.
        Cookie cookie = xsrf();
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                .cookie(cookie)
                .header("X-XSRF-TOKEN", cookie.getValue())
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"claveSegura1\"}"))
            .andExpect(status().isOk())
            .andReturn();
        assertThat((org.springframework.mock.web.MockHttpSession) login.getRequest().getSession(false)).isNotNull();
    }

    @Test
    void loginAntesDeVerificarSeRechazaYDespuesSePermite() throws Exception {
        String email = registrar("Familia Verificacion");
        volverPendienteConToken(email);

        // Antes: 403 con codigo PENDIENTE_VERIFICACION, sin sesion creada.
        Cookie cookie = xsrf();
        mockMvc.perform(post("/api/auth/login")
                .cookie(cookie)
                .header("X-XSRF-TOKEN", cookie.getValue())
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"claveSegura1\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("PENDIENTE_VERIFICACION"));

        // Token expirado: insertado directo en BD (simula el paso del tiempo).
        String enlace = capturaEmailService.ultimoEnlace("verificacion", email);
        String token = enlace.substring(enlace.indexOf("token=") + 6);
        jdbcTemplate.update(
            "UPDATE email_verification_token SET expira_en = DATEADD('MINUTE', -5, CURRENT_TIMESTAMP) "
                + "WHERE token_hash = (SELECT token_hash FROM email_verification_token evt "
                + "JOIN usuario u ON u.id = evt.usuario_id WHERE u.email = ?)", email);
        Cookie cookie2 = xsrf();
        mockMvc.perform(post("/api/auth/verificar-correo")
                .cookie(cookie2)
                .header("X-XSRF-TOKEN", cookie2.getValue())
                .contentType("application/json")
                .content("{\"token\":\"" + token + "\"}"))
            .andExpect(status().isUnprocessableEntity());

        // Reenvio: nuevo token, el anterior (expirado) se invalida.
        Cookie cookie3 = xsrf();
        mockMvc.perform(post("/api/auth/reenviar-verificacion")
                .cookie(cookie3)
                .header("X-XSRF-TOKEN", cookie3.getValue())
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mensaje").value(
                "Si el correo esta registrado y pendiente de verificacion, te enviamos un nuevo enlace"));
        String enlace2 = capturaEmailService.ultimoEnlace("verificacion", email);
        String token2 = enlace2.substring(enlace2.indexOf("token=") + 6);
        org.assertj.core.api.Assertions.assertThat(token2)
            .as("el reenvio genera un token NUEVO para %s (bean=%s, enlace1=%s enlace2=%s)",
                email, System.identityHashCode(capturaEmailService), enlace, enlace2)
            .isNotEqualTo(token);

        // El token viejo ya no verifica; el nuevo si.
        Cookie cookie4 = xsrf();
        mockMvc.perform(post("/api/auth/verificar-correo")
                .cookie(cookie4)
                .header("X-XSRF-TOKEN", cookie4.getValue())
                .contentType("application/json")
                .content("{\"token\":\"" + token + "\"}"))
            .andExpect(status().isUnprocessableEntity());
        Cookie cookie5 = xsrf();
        mockMvc.perform(post("/api/auth/verificar-correo")
                .cookie(cookie5)
                .header("X-XSRF-TOKEN", cookie5.getValue())
                .contentType("application/json")
                .content("{\"token\":\"" + token2 + "\"}"))
            .andExpect(status().isOk());

        // Despues de verificar: login normal con sesion real.
        Cookie cookie6 = xsrf();
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                .cookie(cookie6)
                .header("X-XSRF-TOKEN", cookie6.getValue())
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"claveSegura1\"}"))
            .andExpect(status().isOk())
            .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mockMvc.perform(get("/api/auth/me").session(session))
            .andExpect(status().isOk());
    }

    @Test
    void tokenInexistenteYReenvioDeCuentaInexistenteSonGenericos() throws Exception {
        Cookie cookie = xsrf();
        mockMvc.perform(post("/api/auth/verificar-correo")
                .cookie(cookie)
                .header("X-XSRF-TOKEN", cookie.getValue())
                .contentType("application/json")
                .content("{\"token\":\"no-existe-ni-existira\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error").value(
                "El enlace no es valido o ya expiro. Solicita un nuevo correo de verificacion."));

        // Reenvio de correo inexistente: respuesta generica identica.
        mockMvc.perform(post("/api/auth/reenviar-verificacion")
                .cookie(cookie)
                .header("X-XSRF-TOKEN", cookie.getValue())
                .contentType("application/json")
                .content("{\"email\":\"nadie@" + sufijo() + ".test\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mensaje").value(
                "Si el correo esta registrado y pendiente de verificacion, te enviamos un nuevo enlace"));
    }

    // El test de tasa límite del reenvío vive ahora en
    // com.senderos.seguridad.RateLimitIntegrationTest (contexto propio con
    // umbral bajo); aquí el umbral de pruebas es alto porque toda la suite
    // comparte RateLimiter.

    @Test
    void recuperacionEnviaCorreoYRestableceSinRevelarExistencia() throws Exception {
        // Cuenta verificada para probar recuperación completa (nace verificada).
        String email = registrar("Familia Reset");

        // Solicitud con correo existente: generica + correo capturado.
        Cookie cookie = xsrf();
        mockMvc.perform(post("/api/password/solicitud")
                .cookie(cookie)
                .header("X-XSRF-TOKEN", cookie.getValue())
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mensaje").value(
                "Si el correo esta registrado, recibiras instrucciones para restablecer tu contrasena"));
        String enlace = capturaEmailService.ultimoEnlace("recuperacion", email);
        String token = enlace.substring(enlace.indexOf("token=") + 6);

        // Solicitud con correo INEXISTENTE: misma respuesta generica.
        mockMvc.perform(post("/api/password/solicitud")
                .cookie(cookie)
                .header("X-XSRF-TOKEN", cookie.getValue())
                .contentType("application/json")
                .content("{\"email\":\"fantasma-" + sufijo() + "@gmail.com\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mensaje").value(
                "Si el correo esta registrado, recibiras instrucciones para restablecer tu contrasena"));

        // Restablecer con el token del correo.
        mockMvc.perform(post("/api/password/restablecer")
                .cookie(cookie)
                .header("X-XSRF-TOKEN", cookie.getValue())
                .contentType("application/json")
                .content("{\"token\":\"" + token + "\",\"nuevaPassword\":\"nuevaClave123\"}"))
            .andExpect(status().isOk());

        // Token de un solo uso: reutilizarlo falla y la clave no vuelve a cambiar.
        mockMvc.perform(post("/api/password/restablecer")
                .cookie(cookie)
                .header("X-XSRF-TOKEN", cookie.getValue())
                .contentType("application/json")
                .content("{\"token\":\"" + token + "\",\"nuevaPassword\":\"otraClave456\"}"))
            .andExpect(status().isUnprocessableEntity());

        // La nueva clave funciona; la anterior no.
        mockMvc.perform(post("/api/auth/login")
                .cookie(cookie)
                .header("X-XSRF-TOKEN", cookie.getValue())
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"claveSegura1\"}"))
            .andExpect(status().isUnauthorized());
        Cookie cookie2 = xsrf();
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                .cookie(cookie2)
                .header("X-XSRF-TOKEN", cookie2.getValue())
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"nuevaClave123\"}"))
            .andExpect(status().isOk())
            .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();
    }
}
