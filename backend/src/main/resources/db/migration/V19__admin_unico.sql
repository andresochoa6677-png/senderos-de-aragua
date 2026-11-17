-- ============================================================
-- V19: Administrador ÚNICO del estudio.
--      - Elimina la cuenta semilla admin@senderos.local (sin vivienda
--        ni datos de investigación; sus roles/tokens cascada).
--      - Crea la única cuenta con rol ADMIN: andresochoa224@unisangil.edu.co
--        (pre-verificada, sin vivienda: no pasa por onboarding; su acceso
--        es directo al panel de administración).
--      - La contraseña viaja como hash BCrypt (la clave en claro se
--        comunica por un canal aparte, no en este archivo).
--      - Defensa final: ninguna otra cuenta conserva rol ADMIN.
-- ============================================================

DELETE FROM usuario WHERE email = 'admin@senderos.local';

INSERT INTO usuario (email, password_hash, nombre, enabled, demo, email_verificado, fecha_verificacion, creado_en, actualizado_en)
VALUES ('andresochoa224@unisangil.edu.co',
        '$2a$10$jYoGDScB2eMKLwbe3vKCK.0qCQlGutpebCfzDXxNTwOfghhL0jeEW',
        'Administración del estudio', 1, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO usuario_rol (usuario_id, rol_id)
SELECT u.id, r.id
  FROM usuario u
  JOIN rol r ON r.nombre = 'ADMIN'
 WHERE u.email = 'andresochoa224@unisangil.edu.co';

DELETE FROM usuario_rol
 WHERE rol_id = (SELECT id FROM rol WHERE nombre = 'ADMIN')
   AND usuario_id <> (SELECT id FROM usuario WHERE email = 'andresochoa224@unisangil.edu.co');
