package com.senderos.support;

import com.senderos.service.CapturaEmailService;
import jakarta.servlet.http.Cookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Utilidad de pruebas: obtiene el enlace de verificación capturado por
 * el buzón de desarrollo (CapturaEmailService) y activa la cuenta vía
 * el endpoint público, exactamente como lo haría el residente abriendo
 * el correo.
 */
public final class PruebasCorreo {

    private PruebasCorreo() {}

    public static void verificarCorreoDesdeEnlace(MockMvc mockMvc,
                                                  CapturaEmailService captura,
                                                  String email) throws Exception {
        String enlace = captura.ultimoEnlace("verificacion", email);
        String token = enlace.substring(enlace.indexOf("token=") + "token=".length());
        Cookie xsrf = obtenerXsrf(mockMvc);
        mockMvc.perform(post("/api/auth/verificar-correo")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType("application/json")
                .content("{\"token\":\"" + token + "\"}"))
            .andExpect(status().isOk());
    }

    public static Cookie obtenerXsrf(MockMvc mockMvc) throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        return csrf.getResponse().getCookie("XSRF-TOKEN");
    }
}
