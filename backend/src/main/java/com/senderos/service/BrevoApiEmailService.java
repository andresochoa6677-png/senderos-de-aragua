package com.senderos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Envío real con las PLANTILLAS de Brevo (app.mail.modo=brevo) vía la API
 * transaccional v3 (POST /v3/smtp/email).
 *
 * Por qué API y no MimeMessage: las plantillas viven en el dashboard de
 * Brevo (editarlas ahí aplica al instante, sin recompilar), la API acepta
 * "params" nativos para los enlaces dinámicos ({{LINK_VERIFICACION}} /
 * {{LINK_RECUPERACION}}) y Brevo entrega métricas de apertura por correo.
 *
 * Requiere: BREVO_API_KEY (clave de API, prefijo xkeysib- — NO la key SMTP),
 * BREVO_TEMPLATE_VERIFICACION y BREVO_TEMPLATE_RECUPERACION (ids de las
 * plantillas en el dashboard). Errores de la API se propagan con mensaje
 * claro (sin credenciales en el texto) para que el llamador decida.
 */
@Service
@ConditionalOnProperty(name = "app.mail.modo", havingValue = "brevo")
public class BrevoApiEmailService implements EmailService {

    private static final org.slf4j.Logger log =
        org.slf4j.LoggerFactory.getLogger(BrevoApiEmailService.class);

    private final HttpClient httpClient;
    private final String urlApi;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey;
    private final int templateVerificacion;
    private final int templateRecuperacion;
    private final String desde;
    private final String nombreDesde;

    public BrevoApiEmailService(@Value("${BREVO_API_KEY:}") String apiKey,
                                @Value("${BREVO_TEMPLATE_VERIFICACION:1}") int templateVerificacion,
                                @Value("${BREVO_TEMPLATE_RECUPERACION:2}") int templateRecuperacion,
                                @Value("${app.mail.from:no-reply@senderos.local}") String desde,
                                @Value("${app.mail.from-name:Senderos de Aragua}") String nombreDesde,
                                @Value("${BREVO_API_URL:https://api.brevo.com/v3/smtp/email}") String urlApi) {
        this.apiKey = apiKey;
        this.templateVerificacion = templateVerificacion;
        this.templateRecuperacion = templateRecuperacion;
        this.desde = desde;
        this.nombreDesde = nombreDesde;
        this.urlApi = urlApi;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    }

    @Override
    public void enviarVerificacionCorreo(String destino, String enlace) {
        enviarConPlantilla(destino, templateVerificacion, Map.of("LINK_VERIFICACION", enlace));
    }

    @Override
    public void enviarRecuperacionContrasena(String destino, String enlace) {
        enviarConPlantilla(destino, templateRecuperacion, Map.of("LINK_RECUPERACION", enlace));
    }

    /** Correo de prueba del canal: sin plantilla, textContent directo. */
    @Override
    public void enviarCorreoPrueba(String destino) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("to", List.of(Map.of("email", destino)));
        payload.put("sender", Map.of("name", nombreDesde, "email", desde));
        payload.put("subject", "Correo de prueba - Senderos de Aragua");
        payload.put("textContent", "Este es un correo de PRUEBA del sistema de envío. "
            + "Si lo recibiste, el canal de correo funciona.");
        enviar(destino, payload);
    }

    private void enviarConPlantilla(String destino, int templateId, Map<String, String> params) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("templateId", templateId);
        payload.put("to", List.of(Map.of("email", destino)));
        payload.put("params", params);
        payload.put("sender", Map.of("name", nombreDesde, "email", desde));
        enviar(destino, payload);
    }

    private void enviar(String destino, Map<String, Object> payload) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("BREVO_API_KEY no configurada: define la clave de API "
                + "de Brevo (prefijo xkeysib-) para enviar con plantillas");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlApi))
                .timeout(Duration.ofSeconds(20))
                .header("api-key", apiKey)
                .header("accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                .build();
            HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Brevo respondió " + response.statusCode()
                    + " al enviar a " + destino + ": " + resumir(response.body()));
            }
            log.info("Correo Brevo enviado a {} con plantilla {} (mensaje {})",
                destino, payload.get("templateId"), resumir(response.body()));
        } catch (java.io.IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("No se pudo contactar la API de Brevo: " + e.getMessage(), e);
        }
    }

    private static String resumir(String cuerpo) {
        String texto = cuerpo == null ? "" : cuerpo.replace('\n', ' ');
        return texto.length() > 160 ? texto.substring(0, 160) + "…" : texto;
    }
}
