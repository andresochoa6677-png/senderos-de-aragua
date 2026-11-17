-- ============================================================
-- V24: Nuevas encuestas PREPRUEBA/POSPRUEBA + habilitación manual
--      de la posprueba.
--
--      CAMBIO 1 — reemplaza COMPLETAS las encuestas INICIAL (preprueba,
--      18 ítems) y FINAL (posprueba, 22 ítems; antes 23 con TAM):
--      nueva redacción y opciones reorganizadas. El orden de los
--      incisos cambia A PROPÓSITO entre pre y pos (anti-memoria), así
--      que la clave (es_correcta) se define por encuesta por separado.
--        PARTE 1 (1-10)      OPCION/CONOCIMIENTO — clave es_correcta=1
--                            (1 punto cada una; puntaje 0-10).
--        PARTE 2 (11-14)     ESCALA/PRACTICA — Likert frecuencia 1-5.
--        PARTE 3 (15-18)     ESCALA/PERCEPCION — Likert acuerdo 1-5.
--        PARTE 4 pos (19-22) 19: OPCION frecuencia de uso con valores
--                            1-5 (A=1 … E=5); 20-22: ESCALA acuerdo 1-5.
--      El ítem 18 deja de ser la barrera abierta "Otra" (ahora es
--      Likert); el mecanismo OTRA queda en el código sin uso.
--      ADVERTENCIA: borra las respuestas previas de ambas encuestas —
--      las claves nuevas no son comparables con las viejas.
--
--      CAMBIO 2 — se retira la ventana automática (+22/+35 días desde
--      fecha_inicio). La posprueba queda DESHABILITADA por defecto y
--      el administrador la habilita/deshabilita manualmente desde el
--      panel, con trazabilidad (fecha/hora y responsable). Las columnas
--      fecha_inicio/iniciado_por de V20 se conservan como histórico
--      sin uso en runtime.
-- ============================================================

ALTER TABLE estudio_config ADD COLUMN posprueba_habilitada BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE estudio_config ADD COLUMN posprueba_habilitada_en TIMESTAMP NULL;
ALTER TABLE estudio_config ADD COLUMN posprueba_habilitada_por VARCHAR(100) NULL;

-- ------------------------------------------------------------
-- Limpieza de las encuestas anteriores (respuestas, opciones, preguntas)
-- ------------------------------------------------------------
DELETE FROM respuesta
 WHERE pregunta_id IN (SELECT p.id FROM pregunta p
     JOIN encuesta e ON e.id = p.encuesta_id WHERE e.tipo IN ('INICIAL', 'FINAL'));
DELETE FROM opcion
 WHERE pregunta_id IN (SELECT p.id FROM pregunta p
     JOIN encuesta e ON e.id = p.encuesta_id WHERE e.tipo IN ('INICIAL', 'FINAL'));
DELETE FROM pregunta
 WHERE encuesta_id IN (SELECT id FROM encuesta WHERE tipo IN ('INICIAL', 'FINAL'));

UPDATE encuesta SET titulo = 'Encuesta inicial de residuos sólidos' WHERE tipo = 'INICIAL';
UPDATE encuesta SET titulo = 'Encuesta final: manejo de residuos sólidos' WHERE tipo = 'FINAL';

-- ------------------------------------------------------------
-- PREPRUEBA (INICIAL): 18 preguntas
-- ------------------------------------------------------------
INSERT INTO pregunta (encuesta_id, texto, tipo, tipo_pregunta, orden, requerida)
SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'En el sitio de basuras del condominio hay tres canecas: blanca, negra y verde. ¿Qué se echa en la caneca VERDE?',
       'OPCION', 'CONOCIMIENTO', 1, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'En su casa solo tiene bolsas negras. ¿Cómo puede separar la basura antes de llevarla al sitio de basuras del condominio?',
       'OPCION', 'CONOCIMIENTO', 2, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'Después de comer pizza, la caja de cartón quedó manchada de grasa. ¿Dónde la echa?',
       'OPCION', 'CONOCIMIENTO', 3, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       '¿Qué se echa en la caneca BLANCA del condominio?',
       'OPCION', 'CONOCIMIENTO', 4, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'Se gastaron las pilas del control o se fundió un bombillo. ¿Qué hace con ellos?',
       'OPCION', 'CONOCIMIENTO', 5, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'Terminó de comer una lata de atún o un envase de yogur. ¿Qué hace antes de llevarlo al sitio de basuras del condominio?',
       'OPCION', 'CONOCIMIENTO', 6, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'Al preparar el almuerzo le sobran cáscaras de plátano, de papa y de huevo. ¿En qué caneca las echa?',
       'OPCION', 'CONOCIMIENTO', 7, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'Compró un electrodoméstico y quedó una caja de cartón grande. ¿Cuál es la forma correcta de sacarla?',
       'OPCION', 'CONOCIMIENTO', 8, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'Si mete papel higiénico usado o pañales en la bolsa de los reciclables, ¿qué pasa?',
       'OPCION', 'CONOCIMIENTO', 9, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'Se rompió un vaso de vidrio en la cocina. ¿Cómo lo saca de forma segura?',
       'OPCION', 'CONOCIMIENTO', 10, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       '¿Con qué frecuencia separó lo reciclable (cartón, papel, plástico y latas limpias) de las sobras de comida?',
       'ESCALA', 'PRACTICA', 11, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       '¿Con qué frecuencia sacó la basura al sitio de basuras del condominio en el horario indicado, sin dejarla acumulada en la casa o en las zonas comunes?',
       'ESCALA', 'PRACTICA', 12, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       '¿Con qué frecuencia usó bolsas distintas para cada tipo de basura en su casa (así todas sean negras)?',
       'ESCALA', 'PRACTICA', 13, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'Cuando vio basura tirada o desordenada en las zonas comunes, ¿con qué frecuencia avisó a la administración?',
       'ESCALA', 'PRACTICA', 14, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'La información que da el condominio sobre cómo manejar la basura es clara.',
       'ESCALA', 'PERCEPCION', 15, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'Creo que lo que yo hago en mi casa para reciclar ayuda a mantener limpio el condominio.',
       'ESCALA', 'PERCEPCION', 16, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'Me resulta fácil usar el celular (WhatsApp, redes, etc.) para enterarme de lo que pasa en mi condominio.',
       'ESCALA', 'PERCEPCION', 17, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'No separo la basura porque no sé bien en qué bolsa o caneca va cada cosa.',
       'ESCALA', 'PERCEPCION', 18, 1;

-- Opciones de conocimiento — PREPRUEBA (claves según cada inciso)
INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Plásticos y botellas limpias', 'VERDE_PLASTICOS', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 1
UNION ALL
SELECT p.id, 'Papel higiénico y servilletas sucias', 'VERDE_PAPEL_HIGIENICO', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 1
UNION ALL
SELECT p.id, 'Sobras de comida, cáscaras de frutas y verduras', 'VERDE_ORGANICOS', 1, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 1;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Uso bolsas negras distintas: una para reciclables, otra para sobras de comida y otra para lo que no se recicla', 'SEP_TRES_BOLSAS', 1, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 2
UNION ALL
SELECT p.id, 'Mezclo todo en una sola bolsa, porque lo importante es el color de la bolsa', 'SEP_MEZCLADO', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 2
UNION ALL
SELECT p.id, 'No se puede separar si no tengo bolsas o canecas de colores', 'SEP_IMPOSIBLE', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 2;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'En la caneca negra', 'PIZZA_NEGRA', 1, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 3
UNION ALL
SELECT p.id, 'En la caneca blanca', 'PIZZA_BLANCA', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 3
UNION ALL
SELECT p.id, 'En la caneca verde', 'PIZZA_VERDE', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 3;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Sobras de comida y cáscaras de frutas', 'BLANCO_ORGANICOS', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 4
UNION ALL
SELECT p.id, 'Papel higiénico, servilletas usadas y colillas de cigarrillo', 'BLANCO_NO_APROVECHABLES', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 4
UNION ALL
SELECT p.id, 'Papel, cartón, plástico, vidrio y latas limpias y secas', 'BLANCO_APROVECHABLES', 1, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 4;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Los guardo aparte y los llevo a un punto de recolección especial, porque no van con la basura normal', 'PILAS_ESPECIALES', 1, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 5
UNION ALL
SELECT p.id, 'Los echo en la caneca negra, bien envueltos', 'PILAS_NEGRA', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 5
UNION ALL
SELECT p.id, 'Los echo en la caneca verde', 'PILAS_VERDE', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 5;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Lo vacío y lo enjuago, lo aplasto si puedo y lo meto con los reciclables', 'ENVASE_PREPARADO', 1, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 6
UNION ALL
SELECT p.id, 'Lo echo con los reciclables tal cual, con los restos adentro', 'ENVASE_CON_LIQUIDO', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 6
UNION ALL
SELECT p.id, 'Lo echo con la basura que no se recicla, porque ya no sirve', 'ENVASE_NO_APROVECHABLE', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 6;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'En la caneca blanca', 'CASCARAS_BLANCA', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 7
UNION ALL
SELECT p.id, 'En la caneca verde', 'CASCARAS_VERDE', 1, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 7
UNION ALL
SELECT p.id, 'En la caneca negra', 'CASCARAS_NEGRA', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 7;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Dejarla armada junto a las canecas', 'CAJA_ARMADA', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 8
UNION ALL
SELECT p.id, 'Romperla en pedacitos y echarla en la caneca negra', 'CAJA_PICADA_NEGRA', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 8
UNION ALL
SELECT p.id, 'Desarmarla, aplastarla y echarla en la caneca blanca', 'CAJA_DESARMADA_BLANCA', 1, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 8;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Nada, el camión de la basura los separa después', 'SIN_CONSECUENCIA', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 9
UNION ALL
SELECT p.id, 'Se ensucian los demás reciclables y ya no se pueden aprovechar', 'CONTAMINA_RECICLABLES', 1, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 9
UNION ALL
SELECT p.id, 'Solo hace que la bolsa pese más', 'BOLSA_MAS_PESADA', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 9;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Lo echo suelto en la caneca negra', 'VIDRIO_SUELTO', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 10
UNION ALL
SELECT p.id, 'Lo envuelvo en papel periódico, escribo "vidrio roto" en el paquete y lo echo en la caneca negra', 'VIDRIO_ENVUELTO_MARCADO', 1, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 10
UNION ALL
SELECT p.id, 'Lo echo en la caneca blanca con los reciclables', 'VIDRIO_BLANCA', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 10;

-- Escalas PREPRUEBA: 11-14 frecuencia, 15-18 acuerdo (valores 1-5)
INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, o.texto, o.valor, 0, o.orden
    FROM pregunta p
    JOIN (SELECT 'Nunca' AS texto, '1' AS valor, 1 AS orden
          UNION ALL SELECT 'Casi nunca', '2', 2
          UNION ALL SELECT 'A veces', '3', 3
          UNION ALL SELECT 'Casi siempre', '4', 4
          UNION ALL SELECT 'Siempre', '5', 5) o
    WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden BETWEEN 11 AND 14;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, o.texto, o.valor, 0, o.orden
    FROM pregunta p
    JOIN (SELECT 'Muy en desacuerdo' AS texto, '1' AS valor, 1 AS orden
          UNION ALL SELECT 'En desacuerdo', '2', 2
          UNION ALL SELECT 'Neutral', '3', 3
          UNION ALL SELECT 'De acuerdo', '4', 4
          UNION ALL SELECT 'Muy de acuerdo', '5', 5) o
    WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden BETWEEN 15 AND 18;

-- ------------------------------------------------------------
-- POSPRUEBA (FINAL): 22 preguntas — mismas preguntas de la preprueba
-- con incisos REORGANIZADOS (anti-memoria) + Parte 4 de la plataforma.
-- ------------------------------------------------------------
INSERT INTO pregunta (encuesta_id, texto, tipo, tipo_pregunta, orden, requerida)
SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'En el sitio de basuras del condominio hay tres canecas: blanca, negra y verde. ¿Qué se echa en la caneca VERDE?',
       'OPCION', 'CONOCIMIENTO', 1, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'En su casa solo tiene bolsas negras. ¿Cómo puede separar la basura antes de llevarla al sitio de basuras del condominio?',
       'OPCION', 'CONOCIMIENTO', 2, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'Después de comer pizza, la caja de cartón quedó manchada de grasa. ¿Dónde la echa?',
       'OPCION', 'CONOCIMIENTO', 3, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       '¿Qué se echa en la caneca BLANCA del condominio?',
       'OPCION', 'CONOCIMIENTO', 4, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'Se gastaron las pilas del control o se fundió un bombillo. ¿Qué hace con ellos?',
       'OPCION', 'CONOCIMIENTO', 5, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'Terminó de comer una lata de atún o un envase de yogur. ¿Qué hace antes de llevarlo al sitio de basuras del condominio?',
       'OPCION', 'CONOCIMIENTO', 6, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'Al preparar el almuerzo le sobran cáscaras de plátano, de papa y de huevo. ¿En qué caneca las echa?',
       'OPCION', 'CONOCIMIENTO', 7, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'Compró un electrodoméstico y quedó una caja de cartón grande. ¿Cuál es la forma correcta de sacarla?',
       'OPCION', 'CONOCIMIENTO', 8, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'Si mete papel higiénico usado o pañales en la bolsa de los reciclables, ¿qué pasa?',
       'OPCION', 'CONOCIMIENTO', 9, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'Se rompió un vaso de vidrio en la cocina. ¿Cómo lo saca de forma segura?',
       'OPCION', 'CONOCIMIENTO', 10, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       '¿Con qué frecuencia separó lo reciclable (cartón, papel, plástico y latas limpias) de las sobras de comida?',
       'ESCALA', 'PRACTICA', 11, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       '¿Con qué frecuencia sacó la basura al sitio de basuras del condominio en el horario indicado, sin dejarla acumulada en la casa o en las zonas comunes?',
       'ESCALA', 'PRACTICA', 12, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       '¿Con qué frecuencia usó bolsas distintas para cada tipo de basura en su casa (así todas sean negras)?',
       'ESCALA', 'PRACTICA', 13, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'Cuando vio basura tirada o desordenada en las zonas comunes, ¿con qué frecuencia avisó a la administración?',
       'ESCALA', 'PRACTICA', 14, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'La información que da el condominio sobre cómo manejar la basura es clara.',
       'ESCALA', 'PERCEPCION', 15, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'Creo que lo que yo hago en mi casa para reciclar ayuda a mantener limpio el condominio.',
       'ESCALA', 'PERCEPCION', 16, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'Me resulta fácil usar el celular (WhatsApp, redes, etc.) para enterarme de lo que pasa en mi condominio.',
       'ESCALA', 'PERCEPCION', 17, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'No separo la basura porque no sé bien en qué bolsa o caneca va cada cosa.',
       'ESCALA', 'PERCEPCION', 18, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'En estas tres semanas, ¿con qué frecuencia entró a la plataforma?',
       'OPCION', 'PERCEPCION', 19, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'La plataforma fue fácil de usar (registrarme, moverme por ella y entrar a los contenidos).',
       'ESCALA', 'PERCEPCION', 20, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'Lo que aprendí en la plataforma (lecciones, ejemplos y práctica) me sirvió para separar mejor la basura en mi casa.',
       'ESCALA', 'PERCEPCION', 21, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'Después de usar la plataforma, separo la basura en mi casa mejor que antes.',
       'ESCALA', 'PERCEPCION', 22, 1;

-- Opciones de conocimiento — POSPRUEBA (incisos reorganizados; la clave
-- correcta cambia de posición respecto a la preprueba)
INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Papel higiénico y servilletas sucias', 'VERDE_PAPEL_HIGIENICO', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 1
UNION ALL
SELECT p.id, 'Sobras de comida, cáscaras de frutas y verduras', 'VERDE_ORGANICOS', 1, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 1
UNION ALL
SELECT p.id, 'Plásticos y botellas limpias', 'VERDE_PLASTICOS', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 1;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Mezclo todo en una sola bolsa, porque lo importante es el color de la bolsa', 'SEP_MEZCLADO', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 2
UNION ALL
SELECT p.id, 'No se puede separar si no tengo bolsas o canecas de colores', 'SEP_IMPOSIBLE', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 2
UNION ALL
SELECT p.id, 'Uso bolsas negras distintas: una para reciclables, otra para sobras de comida y otra para lo que no se recicla', 'SEP_TRES_BOLSAS', 1, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 2;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'En la caneca verde', 'PIZZA_VERDE', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 3
UNION ALL
SELECT p.id, 'En la caneca negra', 'PIZZA_NEGRA', 1, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 3
UNION ALL
SELECT p.id, 'En la caneca blanca', 'PIZZA_BLANCA', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 3;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Papel, cartón, plástico, vidrio y latas limpias y secas', 'BLANCO_APROVECHABLES', 1, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 4
UNION ALL
SELECT p.id, 'Sobras de comida y cáscaras de frutas', 'BLANCO_ORGANICOS', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 4
UNION ALL
SELECT p.id, 'Papel higiénico, servilletas usadas y colillas de cigarrillo', 'BLANCO_NO_APROVECHABLES', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 4;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Los echo en la caneca verde', 'PILAS_VERDE', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 5
UNION ALL
SELECT p.id, 'Los guardo aparte y los llevo a un punto de recolección especial, porque no van con la basura normal', 'PILAS_ESPECIALES', 1, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 5
UNION ALL
SELECT p.id, 'Los echo en la caneca negra, bien envueltos', 'PILAS_NEGRA', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 5;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Lo echo con la basura que no se recicla, porque ya no sirve', 'ENVASE_NO_APROVECHABLE', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 6
UNION ALL
SELECT p.id, 'Lo echo con los reciclables tal cual, con los restos adentro', 'ENVASE_CON_LIQUIDO', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 6
UNION ALL
SELECT p.id, 'Lo vacío y lo enjuago, lo aplasto si puedo y lo meto con los reciclables', 'ENVASE_PREPARADO', 1, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 6;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'En la caneca verde', 'CASCARAS_VERDE', 1, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 7
UNION ALL
SELECT p.id, 'En la caneca negra', 'CASCARAS_NEGRA', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 7
UNION ALL
SELECT p.id, 'En la caneca blanca', 'CASCARAS_BLANCA', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 7;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Desarmarla, aplastarla y echarla en la caneca blanca', 'CAJA_DESARMADA_BLANCA', 1, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 8
UNION ALL
SELECT p.id, 'Dejarla armada junto a las canecas', 'CAJA_ARMADA', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 8
UNION ALL
SELECT p.id, 'Romperla en pedacitos y echarla en la caneca negra', 'CAJA_PICADA_NEGRA', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 8;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Solo hace que la bolsa pese más', 'BOLSA_MAS_PESADA', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 9
UNION ALL
SELECT p.id, 'Nada, el camión de la basura los separa después', 'SIN_CONSECUENCIA', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 9
UNION ALL
SELECT p.id, 'Se ensucian los demás reciclables y ya no se pueden aprovechar', 'CONTAMINA_RECICLABLES', 1, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 9;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Lo echo en la caneca blanca con los reciclables', 'VIDRIO_BLANCA', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 10
UNION ALL
SELECT p.id, 'Lo echo suelto en la caneca negra', 'VIDRIO_SUELTO', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 10
UNION ALL
SELECT p.id, 'Lo envuelvo en papel periódico, escribo "vidrio roto" en el paquete y lo echo en la caneca negra', 'VIDRIO_ENVUELTO_MARCADO', 1, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 10;

-- Escalas POSPRUEBA: 11-14 frecuencia, 15-18 acuerdo, 20-22 acuerdo
INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, o.texto, o.valor, 0, o.orden
    FROM pregunta p
    JOIN (SELECT 'Nunca' AS texto, '1' AS valor, 1 AS orden
          UNION ALL SELECT 'Casi nunca', '2', 2
          UNION ALL SELECT 'A veces', '3', 3
          UNION ALL SELECT 'Casi siempre', '4', 4
          UNION ALL SELECT 'Siempre', '5', 5) o
    WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden BETWEEN 11 AND 14;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, o.texto, o.valor, 0, o.orden
    FROM pregunta p
    JOIN (SELECT 'Muy en desacuerdo' AS texto, '1' AS valor, 1 AS orden
          UNION ALL SELECT 'En desacuerdo', '2', 2
          UNION ALL SELECT 'Neutral', '3', 3
          UNION ALL SELECT 'De acuerdo', '4', 4
          UNION ALL SELECT 'Muy de acuerdo', '5', 5) o
    WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL')
      AND p.orden BETWEEN 15 AND 18;

-- Parte 4 · ítem 19: frecuencia de uso de la plataforma (A=1 … E=5)
INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Nunca', '1', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 19
UNION ALL
SELECT p.id, 'Una vez por semana o menos', '2', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 19
UNION ALL
SELECT p.id, 'Dos o tres veces por semana', '3', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 19
UNION ALL
SELECT p.id, 'Casi todos los días', '4', 0, 4
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 19
UNION ALL
SELECT p.id, 'Todos los días', '5', 0, 5
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 19;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, o.texto, o.valor, 0, o.orden
    FROM pregunta p
    JOIN (SELECT 'Muy en desacuerdo' AS texto, '1' AS valor, 1 AS orden
          UNION ALL SELECT 'En desacuerdo', '2', 2
          UNION ALL SELECT 'Neutral', '3', 3
          UNION ALL SELECT 'De acuerdo', '4', 4
          UNION ALL SELECT 'Muy de acuerdo', '5', 5) o
    WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL')
      AND p.orden BETWEEN 20 AND 22;

-- ------------------------------------------------------------
-- Verificación esperada tras la migración:
--   INICIAL: 18 preguntas (10 CONOCIMIENTO / 4 PRACTICA / 4 PERCEPCION),
--            30 opciones de conocimiento + 20 de escala.
--   FINAL:   22 preguntas (10 / 4 / 8), con el ítem 19 de frecuencia
--            con valores 1-5; cada pregunta de conocimiento tiene
--            exactamente UNA opción es_correcta=1.
--   estudio_config: posprueba_habilitada = FALSE (deshabilitada por
--            defecto; la habilita el admin desde el panel).
-- ------------------------------------------------------------
SELECT 'preguntas_inicial' AS dato, COUNT(*) AS cantidad FROM pregunta p
    JOIN encuesta e ON e.id = p.encuesta_id WHERE e.tipo = 'INICIAL'
UNION ALL SELECT 'preguntas_final', COUNT(*) FROM pregunta p
    JOIN encuesta e ON e.id = p.encuesta_id WHERE e.tipo = 'FINAL'
UNION ALL SELECT 'claves_inicial', COUNT(*) FROM opcion o
    JOIN pregunta p ON p.id = o.pregunta_id
    JOIN encuesta e ON e.id = p.encuesta_id
    WHERE e.tipo = 'INICIAL' AND o.es_correcta = 1
UNION ALL SELECT 'claves_final', COUNT(*) FROM opcion o
    JOIN pregunta p ON p.id = o.pregunta_id
    JOIN encuesta e ON e.id = p.encuesta_id
    WHERE e.tipo = 'FINAL' AND o.es_correcta = 1;
