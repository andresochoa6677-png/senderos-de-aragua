-- ============================================================================
-- Limpieza de datos de desarrollo — Senderos de Aragua
-- (mantenimiento 2026-09: dejar la BD lista para demostraciones y E2E)
--
-- Qué hace:
--   * Deja SOLO las cuentas de sistema: admin@senderos.local (ADMIN) y
--     demo@senderos.local (RESIDENTE, vivienda DEMO-001).
--   * Libera el catálogo completo de viviendas (193, todas activas y sin
--     cuenta): borra las cuentas reales y los datos colgantes por vivienda.
--   * Vacía las tablas de actividad: respuesta, intento_ejercicio,
--     progreso_leccion, intento_practica, comentario, tokens.
--   * Conserva las respuestas de consentimiento y la vivienda de demo.
--
-- Qué NO toca (semillas):
--   leccion, ejercicio, practica_item, encuesta/pregunta/opcion, rol,
--   vivienda (ninguna fila se elimina, solo se liberan).
--
-- Repetible (idempotente). Ejecutar así (la contraseña viaja en el entorno
-- del contenedor, jamás en el archivo):
--   docker exec -i senderos-mysql sh -c \
--     'mysql -usenderos -p"$MYSQL_PASSWORD" --default-character-set=utf8mb4 senderos' \
--     < scripts/limpieza_datos.sql
-- ============================================================================

START TRANSACTION;

-- 1. Tokens de recuperación y verificación (también cascaden con usuario,
--    pero el borrado explícito deja la intención clara).
DELETE FROM password_reset_token;
DELETE FROM email_verification_token;

-- 2. Actividad educativa y participación (incluida la de demo: la encuesta
--    INICIAL fue reemplazada por V16, sus respuestas previas ya no aplican;
--    demo las vuelve a responder en la demostración E2E).
DELETE FROM respuesta;
DELETE FROM intento_ejercicio;
DELETE FROM progreso_leccion;
DELETE FROM intento_practica;
DELETE FROM comentario;
DELETE FROM registro_acceso;
DELETE FROM consentimiento_evento;

-- 3. Roles de cuentas que no sean admin/demo (borrado explícito pedido;
--    el resto cascada con el borrado de usuario).
DELETE FROM usuario_rol
 WHERE usuario_id NOT IN (
     SELECT id FROM usuario
      WHERE email IN ('andresochoa224@unisangil.edu.co', 'demo@senderos.local'));

-- 4. Consentimientos: se eliminan TODOS (incluido el de demo, cuyo
--    documento REG-1.0 quedó obsoleto) para que en la próxima sesión el
--    residente —y la cuenta demo— pasen de nuevo por el consentimiento
--    digital REG-2.0 con trazabilidad completa.
DELETE FROM consentimiento;

-- 5. Cuentas residentes reales y de prueba. El cascade elimina sus
--    usuario_rol, tokens y comentarios restantes; su vivienda queda libre
--    para el catálogo.
DELETE FROM usuario
 WHERE email NOT IN ('andresochoa224@unisangil.edu.co', 'demo@senderos.local');

-- 6. Catálogo: todas las viviendas no-demo activas (V3 sembró A-103
--    inactiva; para demostraciones se reactiva según lo pedido).
UPDATE vivienda SET activa = 1 WHERE demo = 0;

-- 6b. Posprueba: vuelve a quedar DESHABILITADA (estado de instalación
--     fresca; el administrador la habilita desde el panel cuando toque).
UPDATE estudio_config
   SET posprueba_habilitada = FALSE, posprueba_habilitada_en = NULL,
       posprueba_habilitada_por = NULL
 WHERE id = 1;

COMMIT;

-- ============================================================================
-- Verificación esperada tras la limpieza:
--   usuarios................ 2   (admin + demo)
--   viviendas............... 193 (0 con cuenta, todas no-demo activas)
--   respuesta/intentos/etc.. 0
--   consentimiento.......... 0  (demo vuelve a pasar por REG-2.0)
--   lecciones................ 6  · ejercicios/practica_item intactos
-- ============================================================================
SELECT 'usuarios' AS dato, COUNT(*) AS cantidad FROM usuario
UNION ALL SELECT 'viviendas', COUNT(*) FROM vivienda
UNION ALL SELECT 'viviendas_con_cuenta', COUNT(*) FROM vivienda v
    WHERE EXISTS (SELECT 1 FROM usuario u WHERE u.vivienda_id = v.id)
UNION ALL SELECT 'viviendas_no_demo_activas', COUNT(*) FROM vivienda WHERE demo = 0 AND activa = 1
UNION ALL SELECT 'consentimientos', COUNT(*) FROM consentimiento
UNION ALL SELECT 'respuestas', COUNT(*) FROM respuesta
UNION ALL SELECT 'intentos_ejercicio', COUNT(*) FROM intento_ejercicio
UNION ALL SELECT 'progreso_leccion', COUNT(*) FROM progreso_leccion
UNION ALL SELECT 'intentos_practica', COUNT(*) FROM intento_practica
UNION ALL SELECT 'comentarios', COUNT(*) FROM comentario
UNION ALL SELECT 'registro_accesos', COUNT(*) FROM registro_acceso
UNION ALL SELECT 'consentimiento_eventos', COUNT(*) FROM consentimiento_evento
UNION ALL SELECT 'password_reset_token', COUNT(*) FROM password_reset_token
UNION ALL SELECT 'email_verification_token', COUNT(*) FROM email_verification_token
UNION ALL SELECT 'lecciones', COUNT(*) FROM leccion
UNION ALL SELECT 'practica_items', COUNT(*) FROM practica_item
UNION ALL SELECT 'preguntas_inicial', (SELECT COUNT(*) FROM pregunta p
    JOIN encuesta e ON e.id = p.encuesta_id WHERE e.tipo = 'INICIAL');
