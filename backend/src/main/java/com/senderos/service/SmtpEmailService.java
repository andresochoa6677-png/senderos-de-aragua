package com.senderos.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envío real por SMTP (app.mail.modo=smtp), configurable 100% por
 * variables de entorno: MAIL_HOST, MAIL_PORT, MAIL_USERNAME,
 * MAIL_PASSWORD, MAIL_FROM, MAIL_FROM_NAME. Cambiar de proveedor es
 * cambiar configuración, nunca código. Si el SMTP no está configurado
 * (sin spring.mail.host) el arranque no se rompe: falla con mensaje
 * claro en el momento de enviar.
 */
@Service
@ConditionalOnProperty(name = "app.mail.modo", havingValue = "smtp")
public class SmtpEmailService implements EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String desde;
    private final String nombreDesde;

    public SmtpEmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                            @Value("${app.mail.from:no-reply@senderos.local}") String desde,
                            @Value("${app.mail.from-name:Senderos de Aragua}") String nombreDesde) {
        this.mailSenderProvider = mailSenderProvider;
        this.desde = desde;
        this.nombreDesde = nombreDesde;
    }

    @Override
    public void enviarVerificacionCorreo(String destino, String enlace) {
        enviar(destino, "Verifica tu cuenta de Senderos de Aragua",
            "Gracias por registrarte. Abre el siguiente enlace para activar tu cuenta (expira en 24 horas):\n\n"
                + enlace + "\n\nSi no fuiste tu, ignora este mensaje.");
    }

    @Override
    public void enviarRecuperacionContrasena(String destino, String enlace) {
        enviar(destino, "Recupera tu contrasena - Senderos de Aragua",
            "Abre el siguiente enlace para crear una contrasena nueva (expira en 30 minutos):\n\n"
                + enlace + "\n\nSi no fuiste tu, ignora este mensaje. Tu contrasena actual sigue funcionando.");
    }

    @Override
    public void enviarCorreoPrueba(String destino) {
        enviar(destino, "Correo de prueba - Senderos de Aragua",
            "Este es un correo de PRUEBA del sistema de envío. Si lo recibiste, el canal de correo funciona.");
    }

    private void enviar(String destino, String asunto, String cuerpo) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException(
                "SMTP no configurado: define MAIL_HOST y MAIL_MODO=smtp en las variables de entorno");
        }
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(nombreDesde + " <" + desde + ">");
            mensaje.setTo(destino);
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);
            mailSender.send(mensaje);
        } catch (MailException e) {
            // El mensaje de MailException no contiene credenciales; se propaga
            // para que el llamador registre el fallo y responda de forma segura.
            throw e;
        }
    }
}
