-- ============================================================
-- V16: Reemplazo completo del banco de la encuesta INICIAL
--      (mantenimiento 2026-09): 18 items en 3 partes.
--        PARTE 1  items  1-10  CONOCIMIENTO (OPCION, 1 correcta)
--        PARTE 2  items 11-14  PRACTICA     (ESCALA 1-5 frecuencia)
--        PARTE 3  items 15-18  PERCEPCION   (15-17 ESCALA de acuerdo,
--                               18 barrera OPCION con "Otra" abierta)
--      Reutiliza el modelo de V9: opcion.es_correcta marca la clave
--      de conocimiento (nunca se expone al cliente), opcion.valor
--      guarda la escala 1-5 / simbolo de analisis, y respuesta.
--      valor_texto guarda la especificacion libre de "Otra".
--      Las respuestas previas a la INICIAL se eliminan: el contenido
--      nuevo ya no es comparable con el anterior, las viviendas que
--      la hubieran respondido vuelven al paso ENCUESTA_INICIAL.
--      Sintaxis con subconsultas por `orden` (portable MySQL/H2):
--      evita depender de ids auto-generados.
-- ============================================================

DELETE FROM respuesta
    WHERE pregunta_id IN (
        SELECT p.id FROM pregunta p
        JOIN encuesta e ON e.id = p.encuesta_id
        WHERE e.tipo = 'INICIAL'
    );
DELETE FROM pregunta
    WHERE encuesta_id IN (SELECT id FROM encuesta WHERE tipo = 'INICIAL');

UPDATE encuesta SET titulo = 'Encuesta inicial de residuos sólidos' WHERE tipo = 'INICIAL';

-- ------------------------------------------------------------
-- Banco de 18 preguntas (una sola insercion, portable)
-- ------------------------------------------------------------
INSERT INTO pregunta (encuesta_id, texto, tipo, tipo_pregunta, orden, requerida)
SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'Después de comer una pizza en casa, ¿qué hace con la caja de cartón que quedó manchada de grasa?',
       'OPCION', 'CONOCIMIENTO', 1, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'Si se rompe un vaso de vidrio en su cocina, ¿cómo lo dispone de forma segura?',
       'OPCION', 'CONOCIMIENTO', 2, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'Según el código de colores, ¿qué tipo de residuos deben ir exclusivamente en el contenedor VERDE?',
       'OPCION', 'CONOCIMIENTO', 3, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'De acuerdo con los horarios del Condominio Senderos de Aragua, ¿qué día debe sacar exclusivamente los residuos APROVECHABLES (reciclaje)?',
       'OPCION', 'CONOCIMIENTO', 4, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'Cuando consume un yogur o un jugo en caja, ¿qué debe hacer antes de botar el envase para que pueda ser reciclado?',
       'OPCION', 'CONOCIMIENTO', 5, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'Según el Código de Colores de Colombia (Resolución 2184 de 2019), ¿qué tipo de residuos van en el contenedor BLANCO?',
       'OPCION', 'CONOCIMIENTO', 6, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'Al preparar el almuerzo le sobran cáscaras de plátano, papa y cáscaras de huevo. ¿En qué contenedor deben ir?',
       'OPCION', 'CONOCIMIENTO', 7, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'Se le gastaron las pilas al control remoto o se le fundió un bombillo. ¿En qué bolsa del condominio los bota?',
       'OPCION', 'CONOCIMIENTO', 8, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'Usted compró un electrodoméstico y le quedó una caja de cartón muy grande. ¿Cuál es la forma correcta de entregarla para reciclar?',
       'OPCION', 'CONOCIMIENTO', 9, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'Si usted por error echa papel higiénico usado o pañales en la bolsa blanca (aprovechables), ¿qué consecuencia tiene esto?',
       'OPCION', 'CONOCIMIENTO', 10, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       '¿Con qué frecuencia separó los residuos reciclables (cartón, plástico limpio) de los restos de comida en su hogar?',
       'ESCALA', 'PRACTICA', 11, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       '¿Con qué frecuencia sacó la basura exactamente en los días y horarios establecidos por el condominio?',
       'ESCALA', 'PRACTICA', 12, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       '¿Con qué frecuencia utilizó los colores correctos de las bolsas (blanca, negra, verde) en su hogar?',
       'ESCALA', 'PRACTICA', 13, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       '¿Con qué frecuencia reportó a la administración cuando vio un desorden de basuras en las zonas comunes?',
       'ESCALA', 'PRACTICA', 14, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'La información que actualmente brinda el condominio sobre cómo manejar la basura es clara y fácil de entender.',
       'ESCALA', 'PERCEPCION', 15, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'Siento que mi esfuerzo individual por reciclar en casa realmente ayuda a mantener limpio el condominio.',
       'ESCALA', 'PERCEPCION', 16, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'Me resulta fácil y útil utilizar plataformas digitales o el celular para informarme sobre mi conjunto residencial.',
       'ESCALA', 'PERCEPCION', 17, 1
UNION ALL SELECT (SELECT id FROM encuesta WHERE tipo = 'INICIAL'),
       'Con sinceridad, ¿cuál considera que es la principal barrera o dificultad en su vivienda para separar correctamente la basura? (Seleccione solo UNA opción)',
       'OPCION', 'PERCEPCION', 18, 1;

-- ------------------------------------------------------------
-- PARTE 1: opciones de conocimiento (clave con es_correcta = 1)
-- ------------------------------------------------------------
INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'La echo en la bolsa negra (no aprovechables) porque está sucia y contaminada.', 'CAJA_MANCHADA_NEGRA', 1, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 1
UNION ALL
SELECT p.id, 'La rompo y la echo en la bolsa blanca (aprovechables).', 'CAJA_ROMPIDA_BLANCA', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 1
UNION ALL
SELECT p.id, 'La echo en la bolsa verde con los restos de comida.', 'CAJA_VERDE_CON_COMIDA', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 1;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Lo echo suelto en la bolsa negra.', 'VIDRIO_SUELTO', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 2
UNION ALL
SELECT p.id, 'Lo envuelvo en papel periódico, lo marco para evitar accidentes y lo echo en la bolsa negra.', 'VIDRIO_ENVUELTO_MARCADO', 1, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 2
UNION ALL
SELECT p.id, 'Lo echo en la bolsa blanca de reciclaje.', 'VIDRIO_BLANCA', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 2;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Plásticos y botellas limpias.', 'VERDE_PLASTICOS', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 3
UNION ALL
SELECT p.id, 'Papel higiénico y servilletas sucias.', 'VERDE_PAPEL_HIGIENICO', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 3
UNION ALL
SELECT p.id, 'Restos de comida, cáscaras de frutas y vegetales (residuos orgánicos).', 'VERDE_ORGANICOS', 1, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 3;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Lunes y jueves.', 'APROVECHABLES_LUN_JUE', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 4
UNION ALL
SELECT p.id, 'Miércoles.', 'APROVECHABLES_MIERCOLES', 1, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 4
UNION ALL
SELECT p.id, 'Martes y viernes.', 'APROVECHABLES_MAR_VIE', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 4;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Enjuagarlo con un poco de agua, escurrirlo y echarlo en la bolsa blanca.', 'ENVASE_ENJUAGADO', 1, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 5
UNION ALL
SELECT p.id, 'Echarlo directamente a la bolsa blanca con restos de líquido.', 'ENVASE_CON_LIQUIDO', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 5
UNION ALL
SELECT p.id, 'Echarlo en la bolsa negra porque ya no sirve.', 'ENVASE_NEGRA', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 5;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Restos de comida y cáscaras de frutas.', 'BLANCO_ORGANICOS', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 6
UNION ALL
SELECT p.id, 'Papel higiénico, servilletas usadas y colillas.', 'BLANCO_NO_APROVECHABLES', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 6
UNION ALL
SELECT p.id, 'Papel, cartón, plástico, vidrio y latas limpias y secas (aprovechables).', 'BLANCO_APROVECHABLES', 1, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 6;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'En la bolsa blanca.', 'CASCARAS_BLANCA', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 7
UNION ALL
SELECT p.id, 'En la bolsa verde (residuos orgánicos).', 'CASCARAS_VERDE', 1, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 7
UNION ALL
SELECT p.id, 'En la bolsa negra.', 'CASCARAS_NEGRA', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 7;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'En ninguna de las bolsas normales; son residuos especiales o peligrosos y van en un punto de recolección diferente.', 'ESPECIALES_PUNTO', 1, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 8
UNION ALL
SELECT p.id, 'En la bolsa negra, bien envueltos.', 'ESPECIALES_NEGRA', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 8
UNION ALL
SELECT p.id, 'En la bolsa verde.', 'ESPECIALES_VERDE', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 8;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Dejarla armada junto a las canecas.', 'CAJA_ARMADA', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 9
UNION ALL
SELECT p.id, 'Romperla en pedacitos pequeños y echarla en la bolsa negra.', 'CAJA_PICADA_NEGRA', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 9
UNION ALL
SELECT p.id, 'Desarmarla, aplastarla para que no ocupe espacio y ponerla junto a la bolsa blanca.', 'CAJA_DESARMADA_APLASTADA', 1, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 9;

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Ninguna, el camión separará el papel higiénico después.', 'SIN_CONSECUENCIA', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 10
UNION ALL
SELECT p.id, 'Contamina el resto de los materiales reciclables limpios, arruinando el esfuerzo de separación.', 'CONTAMINA_RECICLABLES', 1, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 10
UNION ALL
SELECT p.id, 'Hace que la bolsa pese más.', 'BOLSA_MAS_PESADA', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 10;

-- ------------------------------------------------------------
-- PARTE 2 (items 11-14): escala de frecuencia 1-5
-- ------------------------------------------------------------
INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, o.texto, o.valor, 0, o.orden
    FROM pregunta p
    JOIN (SELECT 'Nunca' AS texto, '1' AS valor, 1 AS orden
          UNION ALL SELECT 'Casi nunca', '2', 2
          UNION ALL SELECT 'A veces', '3', 3
          UNION ALL SELECT 'Casi siempre', '4', 4
          UNION ALL SELECT 'Siempre', '5', 5) o
    WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden BETWEEN 11 AND 14;

-- ------------------------------------------------------------
-- PARTE 3 (items 15-17): escala de acuerdo 1-5
-- ------------------------------------------------------------
INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, o.texto, o.valor, 0, o.orden
    FROM pregunta p
    JOIN (SELECT 'Muy en desacuerdo' AS texto, '1' AS valor, 1 AS orden
          UNION ALL SELECT 'En desacuerdo', '2', 2
          UNION ALL SELECT 'Neutral', '3', 3
          UNION ALL SELECT 'De acuerdo', '4', 4
          UNION ALL SELECT 'Muy de acuerdo', '5', 5) o
    WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden BETWEEN 15 AND 17;

-- ------------------------------------------------------------
-- PARTE 3 (item 18): barrera principal, "Otra" abre campo de texto
-- (la especificacion la escribe el residente en respuesta.valor_texto)
-- ------------------------------------------------------------
INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden)
SELECT p.id, 'Falta de espacio en la cocina para tener varias bolsas o canecas.', 'ESPACIO', 0, 1
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 18
UNION ALL
SELECT p.id, 'Falta de tiempo en el día a día.', 'TIEMPO', 0, 2
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 18
UNION ALL
SELECT p.id, 'Desconfianza (siento que el personal de aseo o el camión vuelve a mezclar todo).', 'DESCONFIANZA', 0, 3
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 18
UNION ALL
SELECT p.id, 'Desconocimiento de cómo clasificar correctamente cada cosa.', 'DESCONOCIMIENTO', 0, 4
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 18
UNION ALL
SELECT p.id, 'Otra (campo abierto).', 'OTRA', 0, 5
    FROM pregunta p WHERE p.encuesta_id = (SELECT id FROM encuesta WHERE tipo = 'INICIAL') AND p.orden = 18;
