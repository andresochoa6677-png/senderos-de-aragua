package com.senderos.service;

/**
 * Envío de correos de la plataforma. La lógica de negocio nunca habla
 * directamente con SMTP: depende de este contrato, con dos
 * implementaciones intercambiables por configuración (app.mail.modo):
 *
 * - captura (dev/test): guarda el correo en correo-saliente/ y en
 *   memoria para inspección y pruebas.
 * - smtp (producción): envío real vía spring.mail.* configurable por
 *   variables de entorno (MAIL_HOST, MAIL_PORT, MAIL_USERNAME,
 *   MAIL_PASSWORD, MAIL_FROM, MAIL_FROM_NAME).
 */
public interface EmailService {

    /** Correo de verificación de cuenta: enlace de un solo uso, 24 h. */
    void enviarVerificacionCorreo(String destino, String enlace);

    /** Correo de recuperación de contraseña: enlace de un solo uso, 30 min. */
    void enviarRecuperacionContrasena(String destino, String enlace);

    /** Correo de prueba del canal de envío (solo para el panel de administración). */
    void enviarCorreoPrueba(String destino);
}
