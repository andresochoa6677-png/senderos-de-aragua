-- ============================================================
-- V1: Esquema core - usuarios, roles, viviendas, consentimientos,
--      encuestas, preguntas, opciones, respuestas y tokens
-- ============================================================

CREATE TABLE rol (
    id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(20) NOT NULL UNIQUE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE vivienda (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo    VARCHAR(20)  NOT NULL,
    direccion VARCHAR(120),
    sector    VARCHAR(60),
    demo      TINYINT(1)   NOT NULL DEFAULT 0,
    activa    TINYINT(1)   NOT NULL DEFAULT 1,
    creado_en TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_vivienda_codigo UNIQUE (codigo)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE usuario (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    email          VARCHAR(100) NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    nombre         VARCHAR(80),
    enabled        TINYINT(1)   NOT NULL DEFAULT 1,
    demo           TINYINT(1)   NOT NULL DEFAULT 0,
    vivienda_id    BIGINT       NULL,
    creado_en      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT fk_usuario_vivienda FOREIGN KEY (vivienda_id) REFERENCES vivienda (id),
    INDEX ix_usuario_vivienda (vivienda_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE usuario_rol (
    usuario_id BIGINT NOT NULL,
    rol_id     BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, rol_id),
    CONSTRAINT fk_usuariorol_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT fk_usuariorol_rol FOREIGN KEY (rol_id) REFERENCES rol (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE consentimiento (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    vivienda_id       BIGINT      NOT NULL,
    aceptado          TINYINT(1)  NOT NULL DEFAULT 0,
    version_documento VARCHAR(20) NOT NULL,
    fecha_aceptacion  TIMESTAMP   NULL,
    CONSTRAINT uk_consentimiento_vivienda UNIQUE (vivienda_id),
    CONSTRAINT fk_consentimiento_vivienda FOREIGN KEY (vivienda_id) REFERENCES vivienda (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE encuesta (
    id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    tipo   VARCHAR(10)  NOT NULL,
    titulo VARCHAR(120) NOT NULL,
    activa TINYINT(1)   NOT NULL DEFAULT 1,
    CONSTRAINT uk_encuesta_tipo UNIQUE (tipo)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE pregunta (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    encuesta_id BIGINT       NOT NULL,
    texto       VARCHAR(300) NOT NULL,
    tipo        VARCHAR(15)  NOT NULL,
    orden       INT          NOT NULL DEFAULT 0,
    requerida   TINYINT(1)   NOT NULL DEFAULT 1,
    CONSTRAINT fk_pregunta_encuesta FOREIGN KEY (encuesta_id) REFERENCES encuesta (id) ON DELETE CASCADE,
    INDEX ix_pregunta_encuesta_orden (encuesta_id, orden)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE opcion (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    pregunta_id BIGINT       NOT NULL,
    texto       VARCHAR(200) NOT NULL,
    valor       VARCHAR(50)  NOT NULL,
    orden       INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_opcion_pregunta FOREIGN KEY (pregunta_id) REFERENCES pregunta (id) ON DELETE CASCADE,
    INDEX ix_opcion_pregunta (pregunta_id, orden)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE respuesta (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    vivienda_id    BIGINT       NOT NULL,
    pregunta_id    BIGINT       NOT NULL,
    opcion_id      BIGINT       NULL,
    valor_texto    VARCHAR(500) NULL,
    respondido_en  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_respuesta_vivienda FOREIGN KEY (vivienda_id) REFERENCES vivienda (id) ON DELETE CASCADE,
    CONSTRAINT fk_respuesta_pregunta FOREIGN KEY (pregunta_id) REFERENCES pregunta (id) ON DELETE CASCADE,
    CONSTRAINT fk_respuesta_opcion FOREIGN KEY (opcion_id) REFERENCES opcion (id) ON DELETE CASCADE,
    INDEX ix_respuesta_vivienda_pregunta (vivienda_id, pregunta_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE password_reset_token (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT       NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    usado      TINYINT(1)   NOT NULL DEFAULT 0,
    expira_en  TIMESTAMP    NOT NULL,
    creado_en  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_reset_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_reset_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    INDEX ix_reset_usuario (usuario_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
