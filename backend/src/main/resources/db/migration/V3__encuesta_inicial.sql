-- V3: Encuesta inicial (8 preguntas), encuesta final (inactiva) y
--      viviendas de muestra en bloques A/B/C para pruebas de registro.

INSERT INTO encuesta (tipo, titulo, activa) VALUES ('INICIAL', 'Encuesta inicial de residuos sólidos', 1);
INSERT INTO encuesta (tipo, titulo, activa) VALUES ('FINAL', 'Encuesta final de residuos sólidos', 0);

INSERT INTO pregunta (encuesta_id, texto, tipo, orden, requerida) VALUES
    (1, '¿Con qué frecuencia separas los residuos en tu vivienda?', 'OPCION', 1, 1),
    (1, '¿Qué tipos de residuos separas habitualmente?', 'OPCION', 2, 1),
    (1, '¿Conoces el punto de acopio del condominio?', 'OPCION', 3, 1),
    (1, '¿Sabes qué materiales se reciben en el punto de acopio?', 'OPCION', 4, 1),
    (1, '¿Cuántas bolsas de basura sacas aproximadamente por semana?', 'TEXTO', 5, 1),
    (1, '¿Qué haces con los residuos reciclables que generas?', 'OPCION', 6, 1),
    (1, '¿Has participado en actividades ambientales del condominio?', 'OPCION', 7, 1),
    (1, '¿Qué temas te gustaría aprender en la plataforma?', 'TEXTO', 8, 0);

-- Opciones para preguntas OPCION (encuesta 1)
INSERT INTO opcion (pregunta_id, texto, valor, orden) VALUES
    -- P1: frecuencia de separación
    (1, 'Siempre', 'SIEMPRE', 1), (1, 'Casi siempre', 'CASI_SIEMPRE', 2),
    (1, 'A veces', 'A_VECES', 3), (1, 'Nunca', 'NUNCA', 4),
    -- P2: tipos de residuos que separa
    (2, 'Orgánicos', 'ORGANICOS', 1), (2, 'Plásticos', 'PLASTICOS', 2),
    (2, 'Papel y cartón', 'PAPEL', 3), (2, 'Vidrio', 'VIDRIO', 4),
    (2, 'Metales', 'METALES', 5), (2, 'No separo', 'NINGUNO', 6),
    -- P3: conoce el punto de acopio
    (3, 'Sí, lo uso con frecuencia', 'SI_USO', 1), (3, 'Sí, pero no lo uso', 'SI_NO_USO', 2),
    (3, 'No lo conozco', 'NO', 3),
    -- P4: sabe qué se recibe
    (4, 'Sí, todos', 'SI_TODOS', 1), (4, 'Sí, algunos', 'SI_ALGUNOS', 2),
    (4, 'No estoy seguro', 'NO_SEGURO', 3),
    -- P6: qué hace con reciclables
    (6, 'Los llevo al punto de acopio', 'ACOPIO', 1), (6, 'Los reutilizo', 'REUTILIZO', 2),
    (6, 'Los junto con el resto de la basura', 'BASURA', 3), (6, 'Los vendo', 'VENDO', 4),
    -- P7: participación en actividades
    (7, 'Sí, varias veces', 'SI_VARIAS', 1), (7, 'Sí, una vez', 'SI_UNA', 2),
    (7, 'Nunca', 'NUNCA', 3);

-- Viviendas de muestra (no demo) para pruebas de registro
INSERT INTO vivienda (codigo, direccion, sector, demo, activa) VALUES
    ('A-101', 'Bloque A, Casa 101', 'Sector Norte', 0, 1),
    ('A-102', 'Bloque A, Casa 102', 'Sector Norte', 0, 1),
    ('A-103', 'Bloque A, Casa 103', 'Sector Norte', 0, 0),
    ('B-201', 'Bloque B, Casa 201', 'Sector Sur', 0, 1),
    ('B-202', 'Bloque B, Casa 202', 'Sector Sur', 0, 1),
    ('C-301', 'Bloque C, Casa 301', 'Sector Este', 0, 1);
