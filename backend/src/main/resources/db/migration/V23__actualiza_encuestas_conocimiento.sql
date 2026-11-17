-- ============================================================
-- V23: Actualización de contenido de encuestas + horario.
--      CAMBIO 1: reemplaza las 10 preguntas de CONOCIMIENTO de la
--        encuesta INICIAL (nueva redacción, claves nuevas).
--      CAMBIO 2: reemplaza las 10 de la encuesta FINAL con las MISMAS
--        preguntas en ORDEN ALTERNADO (mismas opciones y claves).
--      CAMBIO 3: actualiza los textos de las prácticas 12 y 13 en
--        ambas encuestas (sus opciones de escala 1-5 se conservan).
--      La percepción/prácticas restantes y los ítems TAM no cambian.
--      ADVERTENCIA: borra las respuestas de conocimiento ya
--      recolectadas de ambas encuestas (al aplicarse la BD no tenía
--      respuestas; si hubiera datos recolectados, esta migración los
--      elimina — las claves nuevas no son comparables con las viejas).
-- ============================================================

DELETE FROM respuesta
 WHERE pregunta_id IN (
     SELECT p.id FROM pregunta p
     JOIN encuesta e ON e.id = p.encuesta_id
     WHERE e.tipo IN ('INICIAL', 'FINAL') AND p.tipo_pregunta = 'CONOCIMIENTO'
 );
DELETE FROM pregunta
 WHERE encuesta_id IN (SELECT id FROM encuesta WHERE tipo IN ('INICIAL', 'FINAL'))
   AND tipo_pregunta = 'CONOCIMIENTO';

-- ------------------------------------------------------------
-- CAMBIO 3 (parte textual): prácticas 12 y 13 en ambas encuestas
-- ------------------------------------------------------------
UPDATE pregunta
   SET texto = '¿Con qué frecuencia llevó los residuos al punto ecológico del condominio en el momento adecuado, evitando dejarlos acumulados?'
 WHERE orden = 12 AND tipo_pregunta = 'PRACTICA'
   AND encuesta_id IN (SELECT id FROM encuesta WHERE tipo IN ('INICIAL', 'FINAL'));

UPDATE pregunta
   SET texto = '¿Con qué frecuencia utilizó bolsas separadas para los distintos tipos de residuos en su hogar (así todas fueran negras)?'
 WHERE orden = 13 AND tipo_pregunta = 'PRACTICA'
   AND encuesta_id IN (SELECT id FROM encuesta WHERE tipo IN ('INICIAL', 'FINAL'));

-- ------------------------------------------------------------
-- CAMBIO 1: 10 preguntas nuevas de CONOCIMIENTO — INICIAL
-- ------------------------------------------------------------
INSERT INTO pregunta (encuesta_id, texto, tipo, tipo_pregunta, orden, requerida)
SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'En el punto ecológico del condominio hay tres canecas: blanca, negra y verde. ¿Qué residuos van en la caneca VERDE?',
       'OPCION', 'CONOCIMIENTO', 1, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'En tu casa solo usas bolsas negras. ¿Cómo puedes separar los residuos antes de llevarlos al punto ecológico?',
       'OPCION', 'CONOCIMIENTO', 2, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'Después de comer pizza, la caja de cartón quedó manchada de grasa. ¿Dónde debe ir?',
       'OPCION', 'CONOCIMIENTO', 3, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       '¿Qué residuos van en la caneca BLANCA del punto ecológico?',
       'OPCION', 'CONOCIMIENTO', 4, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'Se gastaron las pilas del control o se fundió un bombillo. ¿Dónde los dispones en el condominio?',
       'OPCION', 'CONOCIMIENTO', 5, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'Terminas una botella de agua o un envase de yogur. ¿Qué debes hacer antes de llevarlo al punto ecológico?',
       'OPCION', 'CONOCIMIENTO', 6, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'Al preparar el almuerzo te sobran cáscaras de plátano, papa y huevo. ¿En qué caneca van en el punto ecológico?',
       'OPCION', 'CONOCIMIENTO', 7, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'Compraste un electrodoméstico y quedó una caja de cartón grande. ¿Cuál es la forma correcta de disponerla?',
       'OPCION', 'CONOCIMIENTO', 8, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'Si echas papel higiénico usado o pañales en la bolsa de aprovechables, ¿qué consecuencia tiene?',
       'OPCION', 'CONOCIMIENTO', 9, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'Se rompió un vaso de vidrio en la cocina. ¿Cómo lo dispones de forma segura?',
       'OPCION', 'CONOCIMIENTO', 10, 1;

-- Opciones de la INICIAL (claves marcadas con es_correcta = 1)
INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Plásticos y botellas limpias.', 'VERDE_PLASTICOS', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 1
UNION ALL
SELECT p.id, 'Papel higiénico y servilletas sucias.', 'VERDE_PAPEL_HIGIENICO', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 1
UNION ALL
SELECT p.id, 'Restos de comida, cáscaras de frutas y vegetales.', 'VERDE_ORGANICOS', 1, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 1;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Uso una bolsa negra para aprovechables, otra para no aprovechables y otra para orgánicos.', 'SEP_TRES_BOLSAS', 1, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 2
UNION ALL
SELECT p.id, 'Mezclo todo en una sola bolsa, porque el color de la bolsa es lo que importa.', 'SEP_MEZCLADO', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 2
UNION ALL
SELECT p.id, 'No se puede separar si no tengo bolsas de colores.', 'SEP_IMPOSIBLE', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 2;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'En la caneca negra (no aprovechables), porque la grasa contamina el cartón.', 'PIZZA_NEGRA', 1, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 3
UNION ALL
SELECT p.id, 'En la caneca blanca (aprovechables), porque es cartón.', 'PIZZA_BLANCA', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 3
UNION ALL
SELECT p.id, 'En la caneca verde (orgánicos), porque tiene restos de comida.', 'PIZZA_VERDE', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 3;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Restos de comida y cáscaras de frutas.', 'BLANCO_ORGANICOS', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 4
UNION ALL
SELECT p.id, 'Papel higiénico, servilletas usadas y colillas.', 'BLANCO_NO_APROVECHABLES', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 4
UNION ALL
SELECT p.id, 'Papel, cartón, plástico, vidrio y latas limpias y secas.', 'BLANCO_APROVECHABLES', 1, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 4;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'En una caneca especial para residuos peligrosos, porque no van con los residuos comunes.', 'PILAS_ESPECIALES', 1, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 5
UNION ALL
SELECT p.id, 'En la caneca negra, bien envueltos.', 'PILAS_NEGRA', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 5
UNION ALL
SELECT p.id, 'En la caneca verde.', 'PILAS_VERDE', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 5;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Vaciar los restos, aplastar el envase y llevarlo en la bolsa de aprovechables.', 'ENVASE_PREPARADO', 1, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 6
UNION ALL
SELECT p.id, 'Echarlo tal cual, con restos de líquido, en la bolsa de aprovechables.', 'ENVASE_CON_LIQUIDO', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 6
UNION ALL
SELECT p.id, 'Echarlo en la bolsa de no aprovechables porque ya no sirve.', 'ENVASE_NO_APROVECHABLE', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 6;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'En la caneca blanca.', 'CASCARAS_BLANCA', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 7
UNION ALL
SELECT p.id, 'En la caneca verde (orgánicos).', 'CASCARAS_VERDE', 1, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 7
UNION ALL
SELECT p.id, 'En la caneca negra.', 'CASCARAS_NEGRA', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 7;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Dejarla armada junto a las canecas.', 'CAJA_ARMADA', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 8
UNION ALL
SELECT p.id, 'Romperla en pedacitos y echarla en la caneca negra.', 'CAJA_PICADA_NEGRA', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 8
UNION ALL
SELECT p.id, 'Desarmarla, aplastarla y ponerla en la caneca blanca.', 'CAJA_DESARMADA_BLANCA', 1, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 8;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Ninguna, el camión lo separa después.', 'SIN_CONSECUENCIA', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 9
UNION ALL
SELECT p.id, 'Contamina el resto de los materiales reciclables limpios y arruina el esfuerzo de separación.', 'CONTAMINA_RECICLABLES', 1, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 9
UNION ALL
SELECT p.id, 'Hace que la bolsa pese más.', 'BOLSA_MAS_PESADA', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 9;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Lo echo suelto en la caneca negra.', 'VIDRIO_SUELTO', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 10
UNION ALL
SELECT p.id, 'Lo envuelvo en papel periódico, lo marco para evitar accidentes y lo echo en la caneca negra.', 'VIDRIO_ENVUELTO_MARCADO', 1, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 10
UNION ALL
SELECT p.id, 'Lo echo en la caneca blanca de reciclaje.', 'VIDRIO_BLANCA', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 10;

-- ------------------------------------------------------------
-- CAMBIO 2: las MISMAS 10 preguntas en ORDEN ALTERNADO — FINAL
-- ------------------------------------------------------------
INSERT INTO pregunta (encuesta_id, texto, tipo, tipo_pregunta, orden, requerida)
SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       '¿Qué residuos van en la caneca BLANCA del punto ecológico?',
       'OPCION', 'CONOCIMIENTO', 1, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'Se rompió un vaso de vidrio en la cocina. ¿Cómo lo dispones de forma segura?',
       'OPCION', 'CONOCIMIENTO', 2, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'Se gastaron las pilas del control o se fundió un bombillo. ¿Dónde los dispones en el condominio?',
       'OPCION', 'CONOCIMIENTO', 3, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'Después de comer pizza, la caja de cartón quedó manchada de grasa. ¿Dónde debe ir?',
       'OPCION', 'CONOCIMIENTO', 4, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'En el punto ecológico del condominio hay tres canecas: blanca, negra y verde. ¿Qué residuos van en la caneca VERDE?',
       'OPCION', 'CONOCIMIENTO', 5, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'Compraste un electrodoméstico y quedó una caja de cartón grande. ¿Cuál es la forma correcta de disponerla?',
       'OPCION', 'CONOCIMIENTO', 6, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'En tu casa solo usas bolsas negras. ¿Cómo puedes separar los residuos antes de llevarlos al punto ecológico?',
       'OPCION', 'CONOCIMIENTO', 7, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'Terminas una botella de agua o un envase de yogur. ¿Qué debes hacer antes de llevarlo al punto ecológico?',
       'OPCION', 'CONOCIMIENTO', 8, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'Si echas papel higiénico usado o pañales en la bolsa de aprovechables, ¿qué consecuencia tiene?',
       'OPCION', 'CONOCIMIENTO', 9, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'FINAL'),
       'Al preparar el almuerzo te sobran cáscaras de plátano, papa y huevo. ¿En qué caneca van en el punto ecológico?',
       'OPCION', 'CONOCIMIENTO', 10, 1;

-- Opciones de la FINAL (mismas preguntas, claves según el orden alternado)
INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Restos de comida y cáscaras de frutas.', 'BLANCO_ORGANICOS', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 1
UNION ALL
SELECT p.id, 'Papel higiénico, servilletas usadas y colillas.', 'BLANCO_NO_APROVECHABLES', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 1
UNION ALL
SELECT p.id, 'Papel, cartón, plástico, vidrio y latas limpias y secas.', 'BLANCO_APROVECHABLES', 1, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 1;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Lo echo suelto en la caneca negra.', 'VIDRIO_SUELTO', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 2
UNION ALL
SELECT p.id, 'Lo envuelvo en papel periódico, lo marco para evitar accidentes y lo echo en la caneca negra.', 'VIDRIO_ENVUELTO_MARCADO', 1, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 2
UNION ALL
SELECT p.id, 'Lo echo en la caneca blanca de reciclaje.', 'VIDRIO_BLANCA', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 2;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'En una caneca especial para residuos peligrosos, porque no van con los residuos comunes.', 'PILAS_ESPECIALES', 1, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 3
UNION ALL
SELECT p.id, 'En la caneca negra, bien envueltos.', 'PILAS_NEGRA', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 3
UNION ALL
SELECT p.id, 'En la caneca verde.', 'PILAS_VERDE', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 3;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'En la caneca negra (no aprovechables), porque la grasa contamina el cartón.', 'PIZZA_NEGRA', 1, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 4
UNION ALL
SELECT p.id, 'En la caneca blanca (aprovechables), porque es cartón.', 'PIZZA_BLANCA', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 4
UNION ALL
SELECT p.id, 'En la caneca verde (orgánicos), porque tiene restos de comida.', 'PIZZA_VERDE', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 4;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Plásticos y botellas limpias.', 'VERDE_PLASTICOS', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 5
UNION ALL
SELECT p.id, 'Papel higiénico y servilletas sucias.', 'VERDE_PAPEL_HIGIENICO', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 5
UNION ALL
SELECT p.id, 'Restos de comida, cáscaras de frutas y vegetales.', 'VERDE_ORGANICOS', 1, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 5;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Dejarla armada junto a las canecas.', 'CAJA_ARMADA', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 6
UNION ALL
SELECT p.id, 'Romperla en pedacitos pequeños y echarla en la caneca negra.', 'CAJA_PICADA_NEGRA', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 6
UNION ALL
SELECT p.id, 'Desarmarla, aplastarla y ponerla en la caneca blanca.', 'CAJA_DESARMADA_BLANCA', 1, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 6;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Uso una bolsa negra para aprovechables, otra para no aprovechables y otra para orgánicos.', 'SEP_TRES_BOLSAS', 1, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 7
UNION ALL
SELECT p.id, 'Mezclo todo en una sola bolsa, porque el color de la bolsa es lo que importa.', 'SEP_MEZCLADO', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 7
UNION ALL
SELECT p.id, 'No se puede separar si no tengo bolsas de colores.', 'SEP_IMPOSIBLE', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 7;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Vaciar los restos, aplastar el envase y llevarlo en la bolsa de aprovechables.', 'ENVASE_PREPARADO', 1, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 8
UNION ALL
SELECT p.id, 'Echarlo tal cual, con restos de líquido, en la bolsa de aprovechables.', 'ENVASE_CON_LIQUIDO', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 8
UNION ALL
SELECT p.id, 'Echarlo en la bolsa de no aprovechables porque ya no sirve.', 'ENVASE_NEGRA', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 8;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Ninguna, el camión lo separa después.', 'SIN_CONSECUENCIA', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 9
UNION ALL
SELECT p.id, 'Contamina el resto de los materiales reciclables limpios y arruina el esfuerzo de separación.', 'CONTAMINA_RECICLABLES', 1, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 9
UNION ALL
SELECT p.id, 'Hace que la bolsa pese más.', 'BOLSA_MAS_PESADA', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 9;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'En la caneca blanca.', 'CASCARAS_BLANCA', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 10
UNION ALL
SELECT p.id, 'En la caneca verde (orgánicos).', 'CASCARAS_VERDE', 1, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 10
UNION ALL
SELECT p.id, 'En la caneca negra.', 'CASCARAS_NEGRA', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'FINAL') AND p.orden = 10;
