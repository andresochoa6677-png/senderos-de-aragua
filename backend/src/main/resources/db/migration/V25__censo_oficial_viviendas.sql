-- ============================================================
-- V25: Censo oficial de viviendas + selección manual en el registro.
--
--      El condominio envió el listado oficial: todas las viviendas son
--      CASAS identificadas como BLOQUE-NUMERO (A-1, G-5, N-12, RR-3,
--      Ñ-2). Bloques: A-F (4 casas), G (12), H-M (4), N (18), O-S (4),
--      T-Y (4), Z (9), RR (4), Ñ (4) → 139 viviendas residenciales.
--
--      1) Nuevas columnas bloque/numero (NULL para DEMO-001, que no se
--         toca).
--      2) Se REEMPLAZA el catálogo anterior (A-101, B-201, ...): la BD
--         está limpia (solo admin y demo), así que borrar las viviendas
--         no-demo no deja cuentas ni datos colgantes. REQUIERE ese
--         estado: si existieran usuarios residenciales, esta migración
--         fallaría por las llaves foráneas.
--      3) Las viviendas se insertan con activa = 1 (disponibles para
--         elegirse); "ocupada" sigue siendo "con cuenta vinculada"
--         (uk_usuario_vivienda impone una cuenta por vivienda).
--      4) El orden de inserción sigue el alfabeto español con Ñ después
--         de N y RR después de R: A…N, Ñ, O…R, RR, S…Z.
-- ============================================================

ALTER TABLE vivienda ADD COLUMN bloque VARCHAR(2) NULL;
ALTER TABLE vivienda ADD COLUMN numero INT NULL;

-- El collation de la tabla (utf8mb4_unicode_ci) considera 'Ñ' igual a 'N'
-- (insensible a acentos): el código oficial DISTINGUE el bloque Ñ del
-- bloque N, así que la comparación del código pasa a binaria. H2 (tests)
-- ya compara binario: esta sentencia no cambia nada allí.
ALTER TABLE vivienda
    MODIFY codigo VARCHAR(20) NOT NULL COLLATE utf8mb4_bin;

-- ------------------------------------------------------------
-- Retirar el catálogo viejo (solo no-demo; DEMO-001 no se toca)
-- ------------------------------------------------------------
DELETE FROM vivienda WHERE demo = 0;

-- ------------------------------------------------------------
-- Censo oficial (139 viviendas), orden alfabético español
-- ------------------------------------------------------------
INSERT INTO vivienda (codigo, direccion, sector, bloque, numero, demo, activa)
SELECT CONCAT(b.bloque, '-', n.v),
       CONCAT('Bloque ', b.bloque, ', casa ', n.v),
       NULL,
       b.bloque,
       n.v,
       0,
       1
FROM (
    SELECT 'A' AS bloque, 4 AS casas
    UNION ALL SELECT 'B', 4
    UNION ALL SELECT 'C', 4
    UNION ALL SELECT 'D', 4
    UNION ALL SELECT 'E', 4
    UNION ALL SELECT 'F', 4
    UNION ALL SELECT 'G', 12
    UNION ALL SELECT 'H', 4
    UNION ALL SELECT 'I', 4
    UNION ALL SELECT 'J', 4
    UNION ALL SELECT 'K', 4
    UNION ALL SELECT 'L', 4
    UNION ALL SELECT 'M', 4
    UNION ALL SELECT 'N', 18
    UNION ALL SELECT 'Ñ', 4
    UNION ALL SELECT 'O', 4
    UNION ALL SELECT 'P', 4
    UNION ALL SELECT 'Q', 4
    UNION ALL SELECT 'R', 4
    UNION ALL SELECT 'RR', 4
    UNION ALL SELECT 'S', 4
    UNION ALL SELECT 'T', 4
    UNION ALL SELECT 'U', 4
    UNION ALL SELECT 'V', 4
    UNION ALL SELECT 'W', 4
    UNION ALL SELECT 'X', 4
    UNION ALL SELECT 'Y', 4
    UNION ALL SELECT 'Z', 9
) b
JOIN (
    SELECT 1 AS v
    UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
    UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
    UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13
    UNION ALL SELECT 14 UNION ALL SELECT 15 UNION ALL SELECT 16
    UNION ALL SELECT 17 UNION ALL SELECT 18
) n ON n.v <= b.casas;

-- ------------------------------------------------------------
-- Verificación esperada: 140 viviendas (139 residenciales + DEMO-001),
-- 139 con bloque/numero, todas no-demo activas y sin cuenta.
-- ------------------------------------------------------------
SELECT 'total_viviendas' AS dato, COUNT(*) AS cantidad FROM vivienda
UNION ALL SELECT 'residenciales', COUNT(*) FROM vivienda WHERE demo = 0
UNION ALL SELECT 'con_bloque', COUNT(*) FROM vivienda WHERE bloque IS NOT NULL
UNION ALL SELECT 'casa_g', COUNT(*) FROM vivienda WHERE bloque = 'G'
UNION ALL SELECT 'casa_n', COUNT(*) FROM vivienda WHERE bloque = 'N'
UNION ALL SELECT 'casa_rr', COUNT(*) FROM vivienda WHERE bloque = 'RR'
UNION ALL SELECT 'casa_enie', COUNT(*) FROM vivienda WHERE bloque = 'Ñ';
