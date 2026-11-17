-- ============================================================
-- V14: Verificación de correo electrónico y recuperación real.
--      1) usuario.email_verificado / fecha_verificacion: ninguna
--         cuenta de residente queda activa sin verificar su correo.
--      2) email_verification_token: token de un solo uso (hash
--         SHA-256, expiración 24 h), igual patrón de seguridad que
--         password_reset_token.
--      3) Las cuentas técnicas ADMIN y DEMO quedan pre-verificadas:
--         son cuentas de sistema y no pasan por el flujo de correo.
-- ============================================================

ALTER TABLE usuario ADD COLUMN email_verificado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuario ADD COLUMN fecha_verificacion TIMESTAMP NULL;

UPDATE usuario
SET email_verificado = 1,
    fecha_verificacion = CURRENT_TIMESTAMP
WHERE demo = 1
   OR id IN (
       SELECT ur.usuario_id
       FROM usuario_rol ur
       JOIN rol r ON r.id = ur.rol_id
       WHERE r.nombre = 'ADMIN'
   );

CREATE TABLE email_verification_token (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT       NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    usado      TINYINT(1)   NOT NULL DEFAULT 0,
    expira_en  TIMESTAMP    NOT NULL,
    creado_en  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_verif_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_verif_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    INDEX ix_verif_usuario (usuario_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
