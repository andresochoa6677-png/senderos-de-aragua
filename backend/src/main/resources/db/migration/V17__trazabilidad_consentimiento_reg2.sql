-- ============================================================
-- V17: Trazabilidad digital del consentimiento informado (REG-2.0).
--      1) La tabla `consentimiento` (estado vigente, una fila por
--         vivienda) gana los campos de trazabilidad:
--           usuario_id: quien aceptó (legacy REG-1.0 queda NULL).
--           ip_origen:  IP desde la cual se aceptó (45 cubre IPv6).
--           hash_texto: SHA-256 del texto exacto aceptado
--                       (consentimiento/REG-2.0.txt).
--         fecha_hora_aceptacion reutiliza fecha_aceptacion de V1.
--      2) Nueva tabla `consentimiento_evento`: bitácora inmutable de
--         cada decisión (aceptaciones y rechazos, con su versión, IP
--         y hash). Al actualizar el documento a una nueva versión la
--         fila de estado se actualiza y el historial queda intacto.
--      Sintaxis portable MySQL/H2 (sin AFTER; CREATE INDEX suelto).
-- ============================================================

ALTER TABLE consentimiento ADD COLUMN usuario_id BIGINT NULL;
ALTER TABLE consentimiento ADD COLUMN ip_origen VARCHAR(45) NULL;
ALTER TABLE consentimiento ADD COLUMN hash_texto VARCHAR(64) NULL;

ALTER TABLE consentimiento
    ADD CONSTRAINT fk_consentimiento_usuario
    FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE;

CREATE TABLE consentimiento_evento (
    id                BIGINT       AUTO_INCREMENT PRIMARY KEY,
    vivienda_id       BIGINT       NOT NULL,
    usuario_id        BIGINT       NULL,
    version_documento VARCHAR(20)  NOT NULL,
    aceptado          TINYINT(1)   NOT NULL,
    ip_origen         VARCHAR(45)  NULL,
    hash_texto        VARCHAR(64)     NULL,
    fecha_evento      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cevento_vivienda FOREIGN KEY (vivienda_id) REFERENCES vivienda (id) ON DELETE CASCADE,
    CONSTRAINT fk_cevento_usuario  FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    INDEX ix_cevento_vivienda (vivienda_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
