-- ============================================================
-- V18: Encuesta FINAL (posprueba) — 23 ítems.
--      - Se ACTIVA la encuesta tipo FINAL (su disponibilidad por
--        ventana de fechas la controla el servicio, no la BD).
--      - Ítems 1-18: DUPLICADOS de la preprueba (INICIAL) con sus
--        mismas opciones y claves (es_correcta), copiados por `orden`
--        para no depender de ids autogenerados (portable MySQL/H2).
--        Se duplican y no se comparten filas porque pregunta.encuesta_id
--        es un FK simple: cada encuesta posee su propio banco.
--      - Ítems 19-23: nuevos de PERCEPCION sobre la plataforma (TAM):
--        19 frecuencia de uso (OPCION) y 20-23 Likert de acuerdo.
-- ============================================================

UPDATE encuesta SET activa = 1 WHERE tipo = 'FINAL';

-- Copia de los 18 ítems de la preprueba (mismo texto/tipo/orden).
INSERT INTO pregunta (encuesta_id, texto, tipo, tipo_pregunta, orden, requerida)
SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       texto, tipo, tipo_pregunta, orden, requerida
  FROM pregunta
 WHERE encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL');

-- Copia de las opciones (incluidas las claves es_correcta) mapeando
-- pregunta origen → pregunta destino por `orden`.
INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p2.id, o.texto, o.valor, o.es_correcta, o.orden
  FROM opcion o
  JOIN pregunta p  ON p.id  = o.pregunta_id
  JOIN pregunta p2 ON p2.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL')
                  AND p2.orden = p.orden
 WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL');

-- ------------------------------------------------------------
-- Ítems TAM 19-23 (PERCEPCION sobre la plataforma)
-- ------------------------------------------------------------
INSERT INTO pregunta (encuesta_id, texto, tipo, tipo_pregunta, orden, requerida)
SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'Durante las tres semanas de uso de la plataforma, ¿con qué frecuencia accedió a la plataforma digital?',
       'OPCION', 'PERCEPCION', 19, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'La plataforma me resultó fácil de usar (registro, navegación, ingreso a los contenidos).',
       'ESCALA', 'PERCEPCION', 20, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'La información de la plataforma me resultó útil para mejorar mi manejo de residuos en casa.',
       'ESCALA', 'PERCEPCION', 21, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'Los contenidos educativos (lecciones, práctica libre, ejemplos) me ayudaron a comprender mejor cómo separar los residuos.',
       'ESCALA', 'PERCEPCION', 22, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'Después de usar la plataforma, mantengo o mejoré mis prácticas de separación en la fuente, y pienso seguir usándola.',
       'ESCALA', 'PERCEPCION', 23, 1;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Nunca', 'NUNCA', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 19
UNION ALL
SELECT p.id, 'Una vez por semana o menos', 'SEMANA_O_MENOS', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 19
UNION ALL
SELECT p.id, 'Dos o tres veces por semana', 'DOS_TRES_SEMANA', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 19
UNION ALL
SELECT p.id, 'Casi todos los días', 'CASI_DIARIO', 0, 4
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 19
UNION ALL
SELECT p.id, 'Todos los días', 'DIARIO', 0, 5
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 19;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, o.texto, o.valor, 0, o.orden
    FROM pregunta p
    JOIN (SELECT 'Muy en desacuerdo' AS texto, '1' AS valor, 1 AS orden
          UNION ALL SELECT 'En desacuerdo', '2', 2
          UNION ALL SELECT 'Neutral', '3', 3
          UNION ALL SELECT 'De acuerdo', '4', 4
          UNION ALL SELECT 'Muy de acuerdo', '5', 5) o
    WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden BETWEEN 20 AND 23;
