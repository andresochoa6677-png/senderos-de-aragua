package com.senderos.limpieza;

import com.senderos.service.CapturaEmailService;
import com.senderos.service.LimpiezaCuentasService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Auditoría 2026-09-12 (A-1): el registro reserva la vivienda aunque la
 * cuenta nazca PENDIENTE de verificar. Sin limpieza, cuentas fantasma
 * agotan el catálogo. Este test demuestra que LimpiezaCuentasService
 * elimina la cuenta antigua sin verificar con TODO su rastro (consenti-
 * miento, tokens) y devuelve la vivienda al catálogo: el siguiente
 * registro recibe EXACTAMENTE esa misma vivienda.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LimpiezaCuentasIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired CapturaEmailService capturaEmailService;
    @Autowired LimpiezaCuentasService limpiezaCuentasService;

    private String sufijo() {
        return Long.toHexString(System.nanoTime());
    }

    private Cookie xsrf() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        return csrf.getResponse().getCookie("XSRF-TOKEN");
    }

    /** Registra y devuelve el cuerpo 201 (usuario con viviendaCodigo). */
    private String registrar(String email) throws Exception {
        Cookie token = xsrf();
        MvcResult registro = mockMvc.perform(post("/api/onboarding/registro")
                .cookie(token)
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType("application/json")
                .content("{\"nombre\":\"Familia Limpieza\",\"email\":\"" + email
                    + "\",\"password\":\"claveSegura1\",\"aceptaTratamientoDatos\":true," + com.senderos.support.CatalogoPruebas.campos() + "}"))
            .andExpect(status().isCreated())
            .andReturn();
        return registro.getResponse().getContentAsString();
    }

    @Test
    void cuentaPendienteAntiguaLiberaSuVivienda() throws Exception {
        // 1. Registro normal: consume una vivienda y nace PENDIENTE.
        String email = "limpieza-vieja-" + sufijo() + "@test.senderos";
        String cuerpo = registrar(email);
        String codigoOriginal = com.jayway.jsonpath.JsonPath.read(cuerpo, "$.usuario.viviendaCodigo");

        Integer cuentas = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM usuario WHERE email = ?", Integer.class, email);
        assertThat(cuentas).isEqualTo(1);

        // 2. La convertimos en cuenta PENDIENTE antigua (el registro ya no
        //    las crea, pero la limpieza sigue protegiendo el catálogo de
        //    cuentas legacy sin verificar).
        jdbcTemplate.update(
            "UPDATE usuario SET creado_en = ?, email_verificado = 0 WHERE email = ?",
            Instant.now().minus(Duration.ofDays(9)), email);

        // 3. La limpieza elimina la cuenta y su rastro completo.
        int eliminadas = limpiezaCuentasService.limpiarPendientesAntiguos();
        assertThat(eliminadas)
            .as("la cuenta envejecida debe estar entre las eliminadas")
            .isGreaterThanOrEqualTo(1);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM usuario WHERE email = ?", Integer.class, email))
            .as("la cuenta fantasma desaparece").isZero();

        Integer consentimientos = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM consentimiento c JOIN vivienda v ON v.id = c.vivienda_id "
                + "WHERE v.codigo = ?", Integer.class, codigoOriginal);
        assertThat(consentimientos)
            .as("el consentimiento colgante de la vivienda liberada desaparece").isZero();

        // 4. La vivienda vuelve al catálogo: un nuevo residente puede
        //    ELEGIRLA de nuevo en el registro (selección manual).
        String emailReasignada = "limpieza-reasignada-" + sufijo() + "@test.senderos";
        Cookie token2 = xsrf();
        MvcResult registro2 = mockMvc.perform(post("/api/onboarding/registro")
                .cookie(token2)
                .header("X-XSRF-TOKEN", token2.getValue())
                .contentType("application/json")
                .content("{\"nombre\":\"Familia Reasignada\",\"email\":\"" + emailReasignada
                    + "\",\"password\":\"claveSegura1\",\"aceptaTratamientoDatos\":true,"
                    + com.senderos.support.CatalogoPruebas.camposDe(codigoOriginal) + "}"))
            .andExpect(status().isCreated())
            .andReturn();
        String codigoReasignada = com.jayway.jsonpath.JsonPath.read(
            registro2.getResponse().getContentAsString(), "$.usuario.viviendaCodigo");
        assertThat(codigoReasignada)
            .as("la vivienda liberada debe ser elegible de nuevo")
            .isEqualTo(codigoOriginal);
    }

    @Test
    void cuentasRecientesPendientesNoSeTocan() throws Exception {
        String email = "limpieza-fresca-" + sufijo() + "@test.senderos";
        registrar(email); // creada hace segundos: lejos del umbral
        jdbcTemplate.update("UPDATE usuario SET email_verificado = 0 WHERE email = ?", email);

        limpiezaCuentasService.limpiarPendientesAntiguos();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM usuario WHERE email = ?", Integer.class, email))
            .as("una cuenta pendiente reciente NO debe eliminarse").isEqualTo(1);
    }

    @Test
    void cuentasVerificadasYDemoNuncaSeEliminan() throws Exception {
        // El demo (flag demo=1, pre-verificado) debe sobrevivir a la limpieza
        // aunque estuviera "viejo". La semilla admin fue sustituida por V19.
        jdbcTemplate.update(
            "UPDATE usuario SET creado_en = ? WHERE email = 'demo@senderos.local'",
            Instant.now().minus(Duration.ofDays(365)));

        limpiezaCuentasService.limpiarPendientesAntiguos();

        Integer demos = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM usuario WHERE email = 'demo@senderos.local'", Integer.class);
        assertThat(demos)
            .as("demo es cuenta de sistema: la limpieza jamás la toca")
            .isEqualTo(1);
        Integer semillaVieja = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM usuario WHERE email = 'admin@senderos.local'", Integer.class);
        assertThat(semillaVieja).as("la semilla admin ya no existe (V19)").isZero();
    }
}
