-- ============================================================
-- V9: Encuesta INICIAL definitiva: 18 items individuales
--     (10 conocimiento + 5 practicas + 3 percepcion).
--     - pregunta.tipo_pregunta agrupa por seccion (backend).
--     - opcion.es_correcta marca la clave de conocimiento; nunca
--       se expone al cliente para no filtrar respuestas.
--     - Las 18 respuestas se guardan una por fila en `respuesta`
--       asociadas a la vivienda (unidad de datos del sistema).
--     - La encuesta INICIAL ya registrada no se sobrescribe:
--       el servicio la bloquea (409) y esta migracion solo
--       reemplaza el banco de preguntas del contenido.
-- ============================================================

ALTER TABLE opcion
    ADD COLUMN es_correcta TINYINT(1) NOT NULL DEFAULT 0
    AFTER valor;

ALTER TABLE pregunta
    ADD COLUMN tipo_pregunta VARCHAR(15) NOT NULL DEFAULT 'CONOCIMIENTO'
    AFTER tipo;

-- Reemplazo del banco de la encuesta INICIAL (respuestas previas
-- del esquema antiguo de 8 items dejan de aplicar).
-- Sintaxis con IN (portable MySQL/H2): DELETE multi-tabla no es
-- soportado por H2 en el perfil de tests.
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
-- PARTE 1 - CONOCIMIENTO (items 1-10, seleccion multiple,
-- una unica respuesta correcta marcada con es_correcta = 1)
-- ------------------------------------------------------------
INSERT INTO pregunta (encuesta_id, texto, tipo, tipo_pregunta, orden, requerida) VALUES
    (1, '¿En qué contenedor van las botellas plásticas limpias?', 'OPCION', 'CONOCIMIENTO', 1, 1),
    (1, '¿Qué residuos van en el contenedor negro?', 'OPCION', 'CONOCIMIENTO', 2, 1),
    (1, '¿Qué norma regula la separación de residuos por colores?', 'OPCION', 'CONOCIMIENTO', 3, 1),
    (1, '¿Qué debes hacer con una botella antes de depositarla en el contenedor blanco?', 'OPCION', 'CONOCIMIENTO', 4, 1),
    (1, '¿Dónde va el papel sucio o con grasa?', 'OPCION', 'CONOCIMIENTO', 5, 1),
    (1, '¿Con cuánto tiempo de anticipación se deben sacar los residuos para la recolección?', 'OPCION', 'CONOCIMIENTO', 6, 1),
    (1, '¿Qué días hay recolección de residuos en el condominio?', 'OPCION', 'CONOCIMIENTO', 7, 1),
    (1, '¿Cuál de los siguientes es un residuo especial?', 'OPCION', 'CONOCIMIENTO', 8, 1),
    (1, '¿Qué significa separar en la fuente?', 'OPCION', 'CONOCIMIENTO', 9, 1),
    (1, '¿Qué problema genera no separar correctamente los residuos?', 'OPCION', 'CONOCIMIENTO', 10, 1);

-- Item 1: contenedor de botellas plasticas limpias
INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden) VALUES
    (9, 'Blanco', 'BLANCO', 1, 1),
    (9, 'Negro', 'NEGRO', 0, 2),
    (9, 'Verde', 'VERDE', 0, 3),
    (9, 'No se puede reciclar', 'NO_RECICLABLE', 0, 4);

-- Item 2: residuos del contenedor negro
INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden) VALUES
    (10, 'Restos de comida y cáscaras', 'ORGANICOS', 1, 1),
    (10, 'Botellas plásticas limpias', 'PLASTICOS', 0, 2),
    (10, 'Papel y cartón limpios', 'PAPEL', 0, 3),
    (10, 'Vidrio y metales', 'VIDRIO_METALES', 0, 4);

-- Item 3: norma que regula la separacion por colores
INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden) VALUES
    (11, 'Resolución 2184 de 2019', 'RES_2184_2019', 1, 1),
    (11, 'Ley 142 de 1994', 'LEY_142_1994', 0, 2),
    (11, 'Decreto 2981 de 2013', 'DEC_2981_2013', 0, 3),
    (11, 'No existe ninguna norma', 'NINGUNA', 0, 4);

-- Item 4: que hacer con la botella antes del contenedor blanco
INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden) VALUES
    (12, 'Enjuagarla y aplastarla', 'ENJUAGAR_APLASTAR', 1, 1),
    (12, 'Depositarla completa con líquido', 'COMPLETA_LIQUIDO', 0, 2),
    (12, 'Envolverla en papel', 'ENVOLVER', 0, 3),
    (12, 'Solo sacar la tapa', 'SOLO_TAPA', 0, 4);

-- Item 5: papel sucio o con grasa
INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden) VALUES
    (13, 'Negro', 'NEGRO', 1, 1),
    (13, 'Blanco', 'BLANCO', 0, 2),
    (13, 'Contenedor de papel', 'PAPEL', 0, 3),
    (13, 'Punto de acopio', 'ACOPIO', 0, 4);

-- Item 6: anticipacion para sacar los residuos
INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden) VALUES
    (14, '2 horas antes', 'HORAS_2', 1, 1),
    (14, 'La noche anterior', 'NOCHE_ANTERIOR', 0, 2),
    (14, 'Al momento del paso del camión', 'MOMENTO', 0, 3),
    (14, 'Un día antes', 'DIA_ANTERIOR', 0, 4);

-- Item 7: dias de recoleccion
INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden) VALUES
    (15, 'Martes, jueves y sábados', 'MAR_JUE_SAB', 1, 1),
    (15, 'Lunes y viernes', 'LUN_VIE', 0, 2),
    (15, 'Solo los miércoles', 'SOLO_MIE', 0, 3),
    (15, 'Todos los días', 'TODOS', 0, 4);

-- Item 8: residuo especial
INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden) VALUES
    (16, 'Pilas y baterías', 'PILAS', 1, 1),
    (16, 'Cáscaras de frutas', 'CASCARAS', 0, 2),
    (16, 'Cajas de cartón', 'CARTON', 0, 3),
    (16, 'Botellas de vidrio', 'VIDRIO', 0, 4);

-- Item 9: separar en la fuente
INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden) VALUES
    (17, 'Clasificar los residuos donde se generan', 'CLASIFICAR_ORIGEN', 1, 1),
    (17, 'Llevar todo al punto de acopio', 'ACOPIO_TODO', 0, 2),
    (17, 'Esperar la recolección mezclado', 'MEZCLADO', 0, 3),
    (17, 'Separar solo en el condominio', 'SOLO_CONDOMINIO', 0, 4);

-- Item 10: problema por no separar
INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden) VALUES
    (18, 'Contaminación de zonas comunes y fauna nociva', 'CONTAMINACION_FAUNA', 1, 1),
    (18, 'Ningún problema', 'NINGUNO', 0, 2),
    (18, 'Solo más trabajo para el aseo', 'SOLO_ASEO', 0, 3),
    (18, 'Únicamente malos olores', 'SOLO_OLORES', 0, 4);

-- ------------------------------------------------------------
-- PARTE 2 - PRACTICAS (items 11-15, escala 1-5
-- Nunca=1, Casi nunca=2, A veces=3, Casi siempre=4, Siempre=5)
-- ------------------------------------------------------------
INSERT INTO pregunta (encuesta_id, texto, tipo, tipo_pregunta, orden, requerida) VALUES
    (1, 'Separo los residuos aprovechables de los no aprovechables.', 'ESCALA', 'PRACTICA', 11, 1),
    (1, 'Saco los residuos en el horario establecido por el condominio.', 'ESCALA', 'PRACTICA', 12, 1),
    (1, 'Enjuago los envases antes de depositarlos en el contenedor.', 'ESCALA', 'PRACTICA', 13, 1),
    (1, 'Consulto información sobre el manejo adecuado de residuos.', 'ESCALA', 'PRACTICA', 14, 1),
    (1, 'Reporto residuos acumulados en las zonas comunes.', 'ESCALA', 'PRACTICA', 15, 1);

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden) VALUES
    (19, 'Nunca', '1', 0, 1),
    (19, 'Casi nunca', '2', 0, 2),
    (19, 'A veces', '3', 0, 3),
    (19, 'Casi siempre', '4', 0, 4),
    (19, 'Siempre', '5', 0, 5),
    (20, 'Nunca', '1', 0, 1),
    (20, 'Casi nunca', '2', 0, 2),
    (20, 'A veces', '3', 0, 3),
    (20, 'Casi siempre', '4', 0, 4),
    (20, 'Siempre', '5', 0, 5),
    (21, 'Nunca', '1', 0, 1),
    (21, 'Casi nunca', '2', 0, 2),
    (21, 'A veces', '3', 0, 3),
    (21, 'Casi siempre', '4', 0, 4),
    (21, 'Siempre', '5', 0, 5),
    (22, 'Nunca', '1', 0, 1),
    (22, 'Casi nunca', '2', 0, 2),
    (22, 'A veces', '3', 0, 3),
    (22, 'Casi siempre', '4', 0, 4),
    (22, 'Siempre', '5', 0, 5),
    (23, 'Nunca', '1', 0, 1),
    (23, 'Casi nunca', '2', 0, 2),
    (23, 'A veces', '3', 0, 3),
    (23, 'Casi siempre', '4', 0, 4),
    (23, 'Siempre', '5', 0, 5);

-- ------------------------------------------------------------
-- PARTE 3 - PERCEPCION (items 16-18, escala 1-5
-- Muy en desacuerdo=1 ... Muy de acuerdo=5)
-- ------------------------------------------------------------
INSERT INTO pregunta (encuesta_id, texto, tipo, tipo_pregunta, orden, requerida) VALUES
    (1, 'La información sobre el manejo de residuos en el condominio es clara y suficiente.', 'ESCALA', 'PERCEPCION', 16, 1),
    (1, 'La administración gestiona bien los residuos del condominio.', 'ESCALA', 'PERCEPCION', 17, 1),
    (1, 'Me siento corresponsable del manejo adecuado de los residuos.', 'ESCALA', 'PERCEPCION', 18, 1);

INSERT INTO opcion (pregunta_id, texto, valor, es_correcta, orden) VALUES
    (24, 'Muy en desacuerdo', '1', 0, 1),
    (24, 'En desacuerdo', '2', 0, 2),
    (24, 'Neutral', '3', 0, 3),
    (24, 'De acuerdo', '4', 0, 4),
    (24, 'Muy de acuerdo', '5', 0, 5),
    (25, 'Muy en desacuerdo', '1', 0, 1),
    (25, 'En desacuerdo', '2', 0, 2),
    (25, 'Neutral', '3', 0, 3),
    (25, 'De acuerdo', '4', 0, 4),
    (25, 'Muy de acuerdo', '5', 0, 5),
    (26, 'Muy en desacuerdo', '1', 0, 1),
    (26, 'En desacuerdo', '2', 0, 2),
    (26, 'Neutral', '3', 0, 3),
    (26, 'De acuerdo', '4', 0, 4),
    (26, 'Muy de acuerdo', '5', 0, 5);

-- Nota: `valor` conserva el numero 1-5 para permitir calcular puntaje
-- de conocimiento (0-10) y promedios de practicas/percepcion con los
-- valores originales, sin exponer la clave al cliente.


