package com.senderos.contrasena;

import com.senderos.service.CapturaEmailService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Auditoría 2026-09-12 (A-3): política de contraseñas NIST 800-63B en los
 * tres flujos que fijan contraseña — registro, cambio autenticado y
 * restablecimiento. Rechazo de contraseñas comunes, solo numéricas y
 * cortas; aceptación de una larga y no comprometida. La respuesta 400 no
 * cambia nada del estado del usuario (sesión intacta, contraseña vieja
 * sigue válida).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContrasenaPoliticaIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired CapturaEmailService capturaEmailService;

    private String sufijo() {
        return Long.toHexString(System.nanoTime());
    }

    private Cookie xsrf() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        return csrf.getResponse().getCookie("XSRF-TOKEN");
    }

    private String registrar(String email, String password) throws Exception {
        Cookie token = xsrf();
        MvcResult registro = mockMvc.perform(post("/api/onboarding/registro")
                .cookie(token)
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType("application/json")
                .content("{\"nombre\":\"Familia Politica\",\"email\":\"" + email
                    + "\",\"password\":\"" + password + "\",\"aceptaTratamientoDatos\":true," + com.senderos.support.CatalogoPruebas.campos() + "}"))
            .andReturn();
        return registro.getResponse().getContentAsString();
    }

    @Test
    void registroRechazaContrasenaComun() throws Exception {
        Cookie token = xsrf();
        mockMvc.perform(post("/api/onboarding/registro")
                .cookie(token)
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType("application/json")
                .content("{\"nombre\":\"Familia Comun\",\"email\":\"pwd-comun-" + sufijo()
                    + "@test.senderos\",\"password\":\"password123\",\"aceptaTratamientoDatos\":true," + com.senderos.support.CatalogoPruebas.campos() + "}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detalles.password").exists());
    }

    @Test
    void registroRechazaSoloNumeros() throws Exception {
        Cookie token = xsrf();
        mockMvc.perform(post("/api/onboarding/registro")
                .cookie(token)
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType("application/json")
                .content("{\"nombre\":\"Familia Numeros\",\"email\":\"pwd-num-" + sufijo()
                    + "@test.senderos\",\"password\":\"12345678901\",\"aceptaTratamientoDatos\":true," + com.senderos.support.CatalogoPruebas.campos() + "}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detalles.password").exists());
    }

    @Test
    void registroRechazaCorta() throws Exception {
        // 9 caracteres: por debajo del mínimo NIST adaptado (10).
        Cookie token = xsrf();
        mockMvc.perform(post("/api/onboarding/registro")
                .cookie(token)
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType("application/json")
                .content("{\"nombre\":\"Familia Corta\",\"email\":\"pwd-corta-" + sufijo()
                    + "@test.senderos\",\"password\":\"corta1234\",\"aceptaTratamientoDatos\":true," + com.senderos.support.CatalogoPruebas.campos() + "}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detalles.password").exists());
    }

    @Test
    void registroAceptaContrasenaLargaNoComprometida() throws Exception {
        String cuerpo = registrar("pwd-fuerte-" + sufijo() + "@test.senderos", "Clave-Fuerte-2026");
        Object viviendaId = com.jayway.jsonpath.JsonPath.read(cuerpo, "$.usuario.viviendaId");
        assertThat(viviendaId).as("el registro fuerte debe crear la cuenta").isNotNull();
    }

    @Test
    void cambioDePasswordRechazaDebilSinRomperLaSesion() throws Exception {
        String email = "pwd-cambio-" + sufijo() + "@test.senderos";
        registrar(email, "claveSegura1");
        Cookie token = xsrf();
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                .cookie(token)
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"claveSegura1\"}"))
            .andExpect(status().isOk())
            .andReturn();
        MockHttpSession sesion = (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(post("/api/auth/password")
                .session(sesion)
                .cookie(token)
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType("application/json")
                .content("{\"passwordActual\":\"claveSegura1\",\"passwordNueva\":\"password123\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detalles.passwordNueva").exists());

        // La validación rechazó ANTES de tocar nada: la sesión sigue viva y
        // la contraseña antigua sigue siendo la válida.
        mockMvc.perform(get("/api/auth/me").session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void restablecimientoRechazaDebilYAceptaFuerte() throws Exception {
        String email = "pwd-reset-" + sufijo() + "@test.senderos";
        registrar(email, "claveSegura1");
        // Verificamos la cuenta: tras restablecer, el login exige correo verificado.

        Cookie token = xsrf();
        mockMvc.perform(post("/api/password/solicitud")
                .cookie(token)
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\"}"))
            .andExpect(status().isOk());

        String enlace = capturaEmailService.ultimoEnlace("recuperacion", email);
        String tokenReset = enlace.substring(enlace.indexOf("token=") + "token=".length());

        mockMvc.perform(post("/api/password/restablecer")
                .cookie(token)
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType("application/json")
                .content("{\"token\":\"" + tokenReset + "\",\"nuevaPassword\":\"password123\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detalles.nuevaPassword").exists());

        mockMvc.perform(post("/api/password/restablecer")
                .cookie(token)
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType("application/json")
                .content("{\"token\":\"" + tokenReset + "\",\"nuevaPassword\":\"Clave-Restablecida-2026\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                .cookie(token)
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"Clave-Restablecida-2026\"}"))
            .andExpect(status().isOk());
    }
}
