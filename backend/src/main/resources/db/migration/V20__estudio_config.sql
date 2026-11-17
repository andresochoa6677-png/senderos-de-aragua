-- ============================================================
-- V20: Configuración del estudio (inicio manual de la ventana).
--      - Una sola fila (id = 1); el servicio siempre lee y actualiza esa fila.
--      - fecha_inicio: la fija el administrador al presionar
--        "iniciar estudio" (POST /api/admin/estudio/iniciar).
--        Mientras esté NULL, la encuesta FINAL no está disponible
--        para nadie. La ventana de posprueba cuenta desde esta
--        fecha: habilitación = +22 días, cierre = +35 días.
-- ============================================================

CREATE TABLE estudio_config (
    id             BIGINT       AUTO_INCREMENT PRIMARY KEY,
    fecha_inicio   TIMESTAMP    NULL,
    iniciado_por   VARCHAR(100) NULL,
    fecha_creacion TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO estudio_config (id) VALUES (1);
