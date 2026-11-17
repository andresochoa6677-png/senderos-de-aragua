-- ============================================================
-- V21: Registro de accesos por vivienda (trazabilidad del estudio).
--      Una fila por cada inicio de sesión (modulo = 'SESION') y por
--      cada consulta de módulo (LECCIONES, EJERCICIOS, PRACTICA,
--      COMENTARIOS, ENCUESTA_FINAL, ENCUESTA) de una vivienda.
--      Complementa lo que ya se guarda por acción (progreso_leccion,
--      intento_ejercicio, intento_practica, respuesta, comentario).
-- ============================================================

CREATE TABLE registro_acceso (
    id           BIGINT      AUTO_INCREMENT PRIMARY KEY,
    vivienda_id  BIGINT      NOT NULL,
    usuario_id   BIGINT      NOT NULL,
    fecha_hora   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_origen    VARCHAR(45) NULL,
    modulo       VARCHAR(30) NOT NULL,
    CONSTRAINT fk_regacceso_vivienda FOREIGN KEY (vivienda_id) REFERENCES vivienda (id) ON DELETE CASCADE,
    CONSTRAINT fk_regacceso_usuario  FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    INDEX ix_regacceso_vivienda_fecha (vivienda_id, fecha_hora),
    INDEX ix_regacceso_modulo (modulo)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
