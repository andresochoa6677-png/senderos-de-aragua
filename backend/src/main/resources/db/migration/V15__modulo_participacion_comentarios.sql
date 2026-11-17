-- ============================================================
-- V15: Módulo de PARTICIPACIÓN — comentarios de residentes.
--      Aporte privado del residente al proyecto: inquietudes,
--      sugerencias, percepciones y situaciones relacionadas con
--      residuos. Genera datos para el análisis cualitativo del
--      Proyecto Integrador IV (fecha + vivienda + tipo + contenido),
--      con categoría temática RESERVADA para el análisis posterior
--      (no la determina la aplicación).
--      Privado: nunca se muestra a otras viviendas.
-- ============================================================

CREATE TABLE comentario (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id         BIGINT       NOT NULL,
    vivienda_id        BIGINT       NOT NULL,
    tipo               VARCHAR(20)  NOT NULL,
    tema_declarado     VARCHAR(40)  NULL,
    contenido          VARCHAR(500) NOT NULL,
    categoria_tematica VARCHAR(60)  NULL,
    creado_en          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comentario_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT fk_comentario_vivienda FOREIGN KEY (vivienda_id) REFERENCES vivienda (id) ON DELETE CASCADE,
    CONSTRAINT ck_comentario_tipo CHECK (tipo IN ('INQUIETUD', 'SUGERENCIA', 'PERCEPCION', 'SITUACION')),
    INDEX ix_comentario_vivienda (vivienda_id, creado_en),
    INDEX ix_comentario_usuario (usuario_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
