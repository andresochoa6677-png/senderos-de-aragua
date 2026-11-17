-- ============================================================
-- V7: Modulo de ejercicios - banco global de ejercicios por
--     leccion e intentos aislados por vivienda (unidad de datos).
--     La respuesta correcta nunca viaja al cliente: la califica
--     el backend en cada intento.
-- ============================================================

CREATE TABLE ejercicio (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    leccion_id   BIGINT       NOT NULL,
    orden        INT          NOT NULL,
    tipo         VARCHAR(20)  NOT NULL,
    enunciado    VARCHAR(400) NOT NULL,
    explicacion  VARCHAR(400) NOT NULL,
    opciones     JSON         NOT NULL,
    respuesta    VARCHAR(120) NOT NULL,
    puntos       INT          NOT NULL DEFAULT 10,
    activo       TINYINT(1)   NOT NULL DEFAULT 1,
    CONSTRAINT uk_ejercicio_leccion_orden UNIQUE (leccion_id, orden),
    CONSTRAINT fk_ejercicio_leccion FOREIGN KEY (leccion_id) REFERENCES leccion (id) ON DELETE CASCADE,
    CONSTRAINT ck_ejercicio_tipo CHECK (tipo IN ('OPCION_MULTIPLE', 'VERDADERO_FALSO', 'ORDENAR')),
    INDEX ix_ejercicio_leccion (leccion_id, activo, orden)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE intento_ejercicio (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    vivienda_id  BIGINT      NOT NULL,
    ejercicio_id BIGINT      NOT NULL,
    respuesta    VARCHAR(200) NOT NULL,
    correcto     TINYINT(1)  NOT NULL,
    creado_en    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_intento_vivienda FOREIGN KEY (vivienda_id) REFERENCES vivienda (id) ON DELETE CASCADE,
    CONSTRAINT fk_intento_ejercicio FOREIGN KEY (ejercicio_id) REFERENCES ejercicio (id) ON DELETE CASCADE,
    INDEX ix_intento_vivienda_creado (vivienda_id, creado_en),
    INDEX ix_intento_ejercicio (ejercicio_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
