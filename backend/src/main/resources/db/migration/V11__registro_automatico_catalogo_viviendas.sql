-- ============================================================
-- V11: Renovacion del registro de residentes.
--      1) Restriccion dura en BD: UNA VIVIENDA -> MAXIMO UNA CUENTA
--         (UNIQUE en usuario.vivienda_id; los NULL del admin no chocan
--         porque MySQL permite multiples NULL en un indice unico).
--      2) Catalogo de ~200 viviendas de prueba (bloques A-D), generado
--         de forma idempotente: no toca las viviendas existentes de la
--         semilla V3 ni la vivienda DEMO, y solo inserta codigos que
--         aun no existen (repara la ausencia de C-301 en esta BD).
--         PREPARADO PARA REEMPLAZO: cuando exista el censo oficial
--         actualizado, una migracion posterior (V12+) puede desactivar
--         las viviendas del catalogo sin cuentas (activa=0) e insertar
--         el listado oficial; las cuentas ya asignadas no se pierden.
-- ============================================================

-- El indice no-unico de V1 se sustituye por un indice UNIQUE explicito
-- (CREATE UNIQUE INDEX en lugar de ADD CONSTRAINT: H2 perfil test no
-- refuerza la unicidad cuando la constraint se apoya en un indice
-- no-unico preexistente). El indice unico se crea ANTES de retirar el
-- viejo: MySQL exige que la FK usuario.vivienda_id conserve un indice.
-- En ambos motores este indice impone UNA VIVIENDA -> MAXIMO UNA
-- CUENTA; los NULL del admin no chocan porque los indices unicos
-- admiten multiples NULL.
CREATE UNIQUE INDEX uk_usuario_vivienda ON usuario (vivienda_id);

ALTER TABLE usuario DROP INDEX ix_usuario_vivienda;

-- Bloques A/B/C/D con 48 casas cada uno (101-148, 201-248, 301-348,
-- 401-448). El anti-join descarta los codigos ya sembrados en V3
-- (A-101, A-102, A-103, B-201, B-202), de modo que el resultado final
-- del catalogo son 192 viviendas residenciales + DEMO-001.
INSERT INTO vivienda (codigo, direccion, sector, demo, activa)
SELECT CONCAT(b.bloque, '-', b.base + n.v) AS codigo,
       CONCAT('Bloque ', b.bloque, ', Casa ', b.base + n.v) AS direccion,
       b.sector,
       0,
       1
FROM (
    SELECT 'A' AS bloque, 101 AS base, 'Sector Norte' AS sector UNION ALL
    SELECT 'B', 201, 'Sector Sur' UNION ALL
    SELECT 'C', 301, 'Sector Este' UNION ALL
    SELECT 'D', 401, 'Sector Oeste'
) b
JOIN (
    SELECT d1.i + d2.i * 10 AS v
    FROM (SELECT 0 i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
          UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d1
    JOIN (SELECT 0 i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
          UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d2
) n ON n.v <= 47
LEFT JOIN vivienda v2 ON v2.codigo = CONCAT(b.bloque, '-', b.base + n.v)
WHERE v2.id IS NULL;
