-- ============================================================
-- V5: Modulo educativo - lecciones y progreso de lectura
--      por vivienda (unidad de datos del sistema).
-- ============================================================

CREATE TABLE leccion (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    orden       INT          NOT NULL,
    titulo      VARCHAR(120) NOT NULL,
    resumen     VARCHAR(300) NOT NULL,
    icono       VARCHAR(30)  NOT NULL,
    color       VARCHAR(20)  NOT NULL,
    contenido   TEXT         NOT NULL,
    activa      TINYINT(1)   NOT NULL DEFAULT 1,
    CONSTRAINT uk_leccion_orden UNIQUE (orden),
    INDEX ix_leccion_activa_orden (activa, orden)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE progreso_leccion (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    vivienda_id   BIGINT      NOT NULL,
    leccion_id    BIGINT      NOT NULL,
    iniciado_en  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completado_en TIMESTAMP  NULL,
    CONSTRAINT uk_progreso_vivienda_leccion UNIQUE (vivienda_id, leccion_id),
    CONSTRAINT fk_progreso_vivienda FOREIGN KEY (vivienda_id) REFERENCES vivienda (id) ON DELETE CASCADE,
    CONSTRAINT fk_progreso_leccion FOREIGN KEY (leccion_id) REFERENCES leccion (id) ON DELETE CASCADE,
    INDEX ix_progreso_leccion (leccion_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
