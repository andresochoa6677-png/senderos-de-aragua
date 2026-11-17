-- ============================================================
-- V26: Registro sin verificación por correo (decisión de
--      implementación 2026-11).
--
--      El registro crea cuentas YA VERIFICADAS: el residente queda
--      dentro al registrarse, sin el paso de abrir el correo. La
--      recuperación de contraseña sigue funcionando por correo.
--
--      Esta migración activa las cuentas existentes: los residentes
--      PENDIENTE de verificación quedan operativos con todo su rastro
--      (consentimiento, encuesta, respuestas) intacto. Los enlaces de
--      verificación pendientes quedan sin efecto y se limpian.
-- ============================================================

UPDATE usuario
   SET email_verificado = 1,
       fecha_verificacion = COALESCE(fecha_verificacion, CURRENT_TIMESTAMP)
 WHERE email_verificado = 0;

DELETE FROM email_verification_token;

SELECT 'usuarios' AS dato, COUNT(*) AS cantidad FROM usuario
UNION ALL SELECT 'pendientes_restantes', COUNT(*) FROM usuario WHERE email_verificado = 0;
