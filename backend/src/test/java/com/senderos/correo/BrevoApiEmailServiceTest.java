package com.senderos.correo;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitarios de BrevoApiEmailService con un servidor HTTP local
 * (sin red real): verifica que la petición a la API v3 lleve la API key,
 * el templateId correcto y los params de los enlaces dinámicos, y que los
 * errores de Brevo se propagan con mensaje claro.
 */
class BrevoApiEmailServiceTest {

    private HttpServer servidor;
    private final List<String> peticiones = new CopyOnWriteArrayList<>();

    private com.senderos.service.BrevoApiEmailService servicio(int puerto, int respuestaStatus, String cuerpoRespuesta) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/v3/smtp/email", exchange -> {
            peticiones.add("api-key=" + exchange.getRequestHeaders().getFirst("api-key"));
            peticiones.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] cuerpo = cuerpoRespuesta.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(respuestaStatus, cuerpo.length);
            try (var out = exchange.getResponseBody()) {
                out.write(cuerpo);
            }
            exchange.close();
        });
        servidor.start();
        String url = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/v3/smtp/email";
        return new com.senderos.service.BrevoApiEmailService(
            "xkeysib-prueba", 1, 2, "andresochoa224@unisangil.edu.co", "Senderos de Aragua", url);
    }

    @AfterEach
    void parar() {
        if (servidor != null) servidor.stop(0);
        peticiones.clear();
    }

    @Test
    void verificacionUsaPlantilla1ConElEnlaceDinamico() throws Exception {
        var servicio = servicio(0, 201, "{\"messageId\":\"<abc@brevo>\"}");
        servicio.enviarVerificacionCorreo("residente@gmail.com", "http://localhost:5173/verificar?token=TOKEN123");

        assertThat(peticiones).hasSize(2);
        assertThat(peticiones.get(0)).isEqualTo("api-key=xkeysib-prueba");
        String cuerpo = peticiones.get(1);
        assertThat(cuerpo)
            .as("usa la plantilla de verificación (id 1)")
            .contains("\"templateId\":1")
            .as("el enlace dinámico llega como param")
            .contains("\"LINK_VERIFICACION\":\"http://localhost:5173/verificar?token=TOKEN123\"")
            .as("destinatario correcto")
            .contains("\"residente@gmail.com\"")
            .as("remitente institucional")
            .contains("andresochoa224@unisangil.edu.co");
    }

    @Test
    void recuperacionUsaPlantilla2ConSuParametro() throws Exception {
        var servicio = servicio(0, 201, "{\"messageId\":\"<abc@brevo>\"}");
        servicio.enviarRecuperacionContrasena("residente@hotmail.com", "http://localhost:5173/restablecer?token=XYZ");

        String cuerpo = peticiones.get(1);
        assertThat(cuerpo)
            .as("usa la plantilla de recuperación (id 2)")
            .contains("\"templateId\":2")
            .contains("\"LINK_RECUPERACION\":\"http://localhost:5173/restablecer?token=XYZ\"");
    }

    @Test
    void erroresDeBrevoSePropaganConMensajeClaro() throws Exception {
        var servicio = servicio(0, 401, "{\"code\":\"unauthorized\",\"message\":\"Key inválida\"}");
        assertThatThrownBy(() -> servicio.enviarVerificacionCorreo("x@y.com", "http://l/verificar?token=Z"))
            .as("el fallo de la API no debe pasar desapercibido")
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Brevo respondió 401");
    }

    @Test
    void sinApiKeyFallaConMensajeClaro() {
        var servicio = new com.senderos.service.BrevoApiEmailService(
            "", 1, 2, "from@x", "Senderos", "http://127.0.0.1:1/v3/smtp/email");
        assertThatThrownBy(() -> servicio.enviarVerificacionCorreo("x@y.com", "http://l/verificar?token=Z"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("BREVO_API_KEY");
    }
}
