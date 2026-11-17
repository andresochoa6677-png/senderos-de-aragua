-- ============================================================
-- V13: Módulo de CLASIFICACIÓN — zona de práctica libre.
--      Práctica independiente del recorrido de lecciones:
--      no bloquea ni es bloqueada por el gating educativo.
--
--      practica_item: banco de situaciones/elementos por modo
--        (CLASIFICA / PREPARA / SITUACION) con su solución solo
--        en backend.
--      intento_practica: registro por vivienda de cada intento
--        (modo, respuesta, resultado, fecha) para análisis
--        posterior de errores. La vivienda siempre se resuelve
--        desde la sesión autenticada, nunca desde el cliente.
--
--      Contenido coherente con las 6 lecciones y la regla de los
--      3 colores (blanco = aprovechables, negro = no
--      aprovechables, verde = orgánicos aprovechables).
-- ============================================================

CREATE TABLE practica_item (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    modo        VARCHAR(20)  NOT NULL,
    tipo        VARCHAR(20)  NOT NULL,
    escenario   VARCHAR(300) NULL,
    enunciado   VARCHAR(400) NOT NULL,
    imagen      VARCHAR(200) NULL,
    contenido   JSON         NOT NULL,
    respuesta   VARCHAR(1000) NOT NULL,
    explicacion VARCHAR(500) NOT NULL,
    activo      TINYINT(1)   NOT NULL DEFAULT 1,
    creado_en   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_practica_modo CHECK (modo IN ('CLASIFICA', 'PREPARA', 'SITUACION')),
    CONSTRAINT ck_practica_tipo CHECK (tipo IN ('DESTINO', 'ELECCION', 'ORDENAR', 'GRUPO')),
    INDEX ix_practica_item_modo (modo, activo)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE intento_practica (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    vivienda_id       BIGINT       NOT NULL,
    practica_item_id  BIGINT       NOT NULL,
    modo              VARCHAR(20)  NOT NULL,
    respuesta         VARCHAR(1000) NOT NULL,
    correcto          TINYINT(1)   NOT NULL,
    creado_en         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_intpractica_vivienda FOREIGN KEY (vivienda_id) REFERENCES vivienda (id) ON DELETE CASCADE,
    CONSTRAINT fk_intpractica_item FOREIGN KEY (practica_item_id) REFERENCES practica_item (id) ON DELETE CASCADE,
    INDEX ix_intpractica_vivienda (vivienda_id, creado_en),
    INDEX ix_intpractica_item (practica_item_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- Banco de práctica: 25 actividades (12 CLASIFICA, 5 PREPARA,
-- 8 SITUACION). Contenido NUEVO que amplía lo enseñado en las
-- lecciones; la solución (respuesta) nunca viaja al cliente.
-- ------------------------------------------------------------

INSERT INTO practica_item (modo, tipo, escenario, enunciado, imagen, contenido, respuesta, explicacion) VALUES
-- ===================== MODO CLASIFICA (12) =====================
('CLASIFICA', 'DESTINO', NULL, 'Periódico del domingo, limpio y seco', NULL,
 '{"destinos":[{"id":"blanco","etiqueta":"Blanco · Aprovechables","color":"blanco"},{"id":"negro","etiqueta":"Negro · No aprovechables","color":"negro"},{"id":"verde","etiqueta":"Verde · Orgánicos","color":"verde"}]}',
 'blanco', 'El papel limpio y seco es aprovechable: va en la caneca blanca. Si está manchado con comida, ya sería no aprovechable.'),
('CLASIFICA', 'DESTINO', NULL, 'Frasco de mermelada de vidrio, vacío', NULL,
 '{"destinos":[{"id":"blanco","etiqueta":"Blanco · Aprovechables","color":"blanco"},{"id":"negro","etiqueta":"Negro · No aprovechables","color":"negro"},{"id":"verde","etiqueta":"Verde · Orgánicos","color":"verde"}]}',
 'blanco', 'El vidrio es un material muy aprovechable cuando llega vacío. Enjuágalo y va a la caneca blanca.'),
('CLASIFICA', 'DESTINO', NULL, 'Lata de atún vacía y limpia', NULL,
 '{"destinos":[{"id":"blanco","etiqueta":"Blanco · Aprovechables","color":"blanco"},{"id":"negro","etiqueta":"Negro · No aprovechables","color":"negro"},{"id":"verde","etiqueta":"Verde · Orgánicos","color":"verde"}]}',
 'blanco', 'Los metales como las latas son aprovechables. Lávala, escúrrela y depósitala en la caneca blanca.'),
('CLASIFICA', 'DESTINO', NULL, 'Botella de detergente vacía y enjuagada', NULL,
 '{"destinos":[{"id":"blanco","etiqueta":"Blanco · Aprovechables","color":"blanco"},{"id":"negro","etiqueta":"Negro · No aprovechables","color":"negro"},{"id":"verde","etiqueta":"Verde · Orgánicos","color":"verde"}]}',
 'blanco', 'Preparada (vacía y enjuagada) es un aprovechable: caneca blanca.'),
('CLASIFICA', 'DESTINO', NULL, 'Cáscara de plátano', NULL,
 '{"destinos":[{"id":"blanco","etiqueta":"Blanco · Aprovechables","color":"blanco"},{"id":"negro","etiqueta":"Negro · No aprovechables","color":"negro"},{"id":"verde","etiqueta":"Verde · Orgánicos","color":"verde"}]}',
 'verde', 'Las cáscaras de frutas son orgánicos aprovechables: caneca verde, donde pueden convertirse en abono.'),
('CLASIFICA', 'DESTINO', NULL, 'Hojas secas recogidas del jardín', NULL,
 '{"destinos":[{"id":"blanco","etiqueta":"Blanco · Aprovechables","color":"blanco"},{"id":"negro","etiqueta":"Negro · No aprovechables","color":"negro"},{"id":"verde","etiqueta":"Verde · Orgánicos","color":"verde"}]}',
 'verde', 'Los residuos de jardín son orgánicos aprovechables: caneca verde.'),
('CLASIFICA', 'DESTINO', NULL, 'Cáscara de huevo', NULL,
 '{"destinos":[{"id":"blanco","etiqueta":"Blanco · Aprovechables","color":"blanco"},{"id":"negro","etiqueta":"Negro · No aprovechables","color":"negro"},{"id":"verde","etiqueta":"Verde · Orgánicos","color":"verde"}]}',
 'verde', 'La cáscara de huevo es un resto de cocina: orgánico aprovechable, caneca verde.'),
('CLASIFICA', 'DESTINO', NULL, 'Restos de ensalada del almuerzo', NULL,
 '{"destinos":[{"id":"blanco","etiqueta":"Blanco · Aprovechables","color":"blanco"},{"id":"negro","etiqueta":"Negro · No aprovechables","color":"negro"},{"id":"verde","etiqueta":"Verde · Orgánicos","color":"verde"}]}',
 'verde', 'Los restos de comida son orgánicos aprovechables: caneca verde.'),
('CLASIFICA', 'DESTINO', NULL, 'Servilleta de papel usada en la comida', NULL,
 '{"destinos":[{"id":"blanco","etiqueta":"Blanco · Aprovechables","color":"blanco"},{"id":"negro","etiqueta":"Negro · No aprovechables","color":"negro"},{"id":"verde","etiqueta":"Verde · Orgánicos","color":"verde"}]}',
 'negro', 'El papel higiénico y las servilletas usadas no se pueden recuperar: caneca negra, aunque sean de papel.'),
('CLASIFICA', 'DESTINO', NULL, 'Caja de pizza manchada con grasa', NULL,
 '{"destinos":[{"id":"blanco","etiqueta":"Blanco · Aprovechables","color":"blanco"},{"id":"negro","etiqueta":"Negro · No aprovechables","color":"negro"},{"id":"verde","etiqueta":"Verde · Orgánicos","color":"verde"}]}',
 'negro', 'El cartón contaminado con grasa pierde su valor de reciclaje: caneca negra. Solo el cartón limpio va en la blanca.'),
('CLASIFICA', 'DESTINO', NULL, 'Pañal desechable usado', NULL,
 '{"destinos":[{"id":"blanco","etiqueta":"Blanco · Aprovechables","color":"blanco"},{"id":"negro","etiqueta":"Negro · No aprovechables","color":"negro"},{"id":"verde","etiqueta":"Verde · Orgánicos","color":"verde"}]}',
 'negro', 'Los residuos sanitarios como los pañales usados son no aprovechables: caneca negra, siempre en bolsa cerrada.'),
('CLASIFICA', 'DESTINO', NULL, 'Bolsa de pan recién terminada: la bolsa interior está limpia', NULL,
 '{"destinos":[{"id":"blanco","etiqueta":"Blanco · Aprovechables","color":"blanco"},{"id":"negro","etiqueta":"Negro · No aprovechables","color":"negro"},{"id":"verde","etiqueta":"Verde · Orgánicos","color":"verde"}]}',
 'negro', 'Las bolsas de plástico de alimentos suelen estar contaminadas con migas y grasas: si no está limpia y seca, va en la negra.'),
-- ===================== MODO PREPARA (5) =====================
('PREPARA', 'ELECCION', 'Encontraste una botella con jugo restante.', '¿Qué haces primero antes de llevarla al punto?', NULL,
 '{"opciones":[{"id":"o1","texto":"Depositarla así, con el jugo adentro, en la blanca"},{"id":"o2","texto":"Vaciar todo el contenido"},{"id":"o3","texto":"Mezclarla con los orgánicos"},{"id":"o4","texto":"Guardarla bajo el fregadero para otro día"}]}',
 'o2', 'El primer paso siempre es vaciar todo el contenido. Después la enjuagas, la dejas escurrir y sí va a la caneca blanca.'),
('PREPARA', 'ELECCION', 'Una lata quedó con restos de atún pegados.', '¿Cómo la preparas correctamente?', NULL,
 '{"opciones":[{"id":"o1","texto":"La pongo sucia en la caneca blanca"},{"id":"o2","texto":"La tiro a la negra por si acaso"},{"id":"o3","texto":"La enjuago con agua y la dejo escurrir"},{"id":"o4","texto":"La guardo en la caneca verde"}]}',
 'o3', 'Los metales se recuperan perfectamente si llegan limpios: enjuaga la lata, déjala escurrir y depósitala en la blanca.'),
('PREPARA', 'ELECCION', 'Enjuagaste un frasco de vidrio, pero aún gotea agua.', '¿Está listo para la caneca blanca?', NULL,
 '{"opciones":[{"id":"o1","texto":"Sí, el agua no molesta"},{"id":"o2","texto":"No: deja que escurra hasta estar limpio y sin goteos"},{"id":"o3","texto":"Sí, si lo sacudes una vez"},{"id":"o4","texto":"Sí, pero solo los viernes"}]}',
 'o2', 'Un envase está preparado cuando está limpio, vacío y sin goteos. El exceso de agua también ensucia el punto de residuos.'),
('PREPARA', 'ELECCION', 'El envase de yogur quedó manchado con grasa.', '¿Qué haces con él?', NULL,
 '{"opciones":[{"id":"o1","texto":"Va a la caneca negra: está contaminado"},{"id":"o2","texto":"Lo enjuago y va a la blanca igual"},{"id":"o3","texto":"Va a la caneca verde junto a la comida"},{"id":"o4","texto":"Lo guardo para el punto especial"}]}',
 'o1', 'Los empaques contaminados con grasa no se pueden recuperar: son no aprovechables y van en la caneca negra.'),
('PREPARA', 'ELECCION', 'Vas a depositar varios envases en la caneca blanca.', '¿Qué condición deben cumplir todos?', NULL,
 '{"opciones":[{"id":"o1","texto":"Estar limpios, vacíos y sin goteos"},{"id":"o2","texto":"Estar rotos para ocupar menos espacio"},{"id":"o3","texto":"Estar dentro de una bolsa negra"},{"id":"o4","texto":"Estar mojados para que no vuelen"}]}',
 'o1', 'La regla de preparación: limpios, vacíos y sin goteos. Así los aprovechables no se contaminan entre sí.'),
-- ===================== MODO SITUACION (8) =====================
('SITUACION', 'GRUPO', 'Terminaste de cocinar y la mesa quedó así.', 'Ubica cada residuo en la caneca que le corresponde.', NULL,
 '{"destinos":[{"id":"blanco","etiqueta":"Blanco · Aprovechables","color":"blanco"},{"id":"negro","etiqueta":"Negro · No aprovechables","color":"negro"},{"id":"verde","etiqueta":"Verde · Orgánicos","color":"verde"}],"items":[{"id":"caja-cereal","texto":"Caja de cereal vacía","color":"blanco"},{"id":"cascaras-cebolla","texto":"Cáscaras de cebolla","color":"verde"},{"id":"toallas-usadas","texto":"Toallas de papel usadas","color":"negro"}]}',
 '{"caja-cereal":"blanco","cascaras-cebolla":"verde","toallas-usadas":"negro"}',
 'El cartón limpio va a la blanca, las cáscaras al verde y las toallas de papel usadas a la negra.'),
('SITUACION', 'GRUPO', 'Vas a salir hacia el punto de disposición con tus tres bolsas.', 'Deposita cada bolsa en la caneca correcta.', NULL,
 '{"destinos":[{"id":"blanco","etiqueta":"Blanco · Aprovechables","color":"blanco"},{"id":"negro","etiqueta":"Negro · No aprovechables","color":"negro"},{"id":"verde","etiqueta":"Verde · Orgánicos","color":"verde"}],"items":[{"id":"bolsa-envases","texto":"Bolsa con botellas y latas limpias","color":"blanco"},{"id":"bolsa-higienico","texto":"Bolsa con papel higiénico y servilletas","color":"negro"},{"id":"bolsa-cascaras","texto":"Bolsa con cáscaras y restos de comida","color":"verde"}]}',
 '{"bolsa-envases":"blanco","bolsa-higienico":"negro","bolsa-cascaras":"verde"}',
 'Cada bolsa se entrega en su caneca: los envases preparados en la blanca, los no aprovechables en la negra y los orgánicos en la verde.'),
('SITUACION', 'GRUPO', 'Terminó la limpieza del patio.', 'Clasifica lo que recogiste.', NULL,
 '{"destinos":[{"id":"blanco","etiqueta":"Blanco · Aprovechables","color":"blanco"},{"id":"negro","etiqueta":"Negro · No aprovechables","color":"negro"},{"id":"verde","etiqueta":"Verde · Orgánicos","color":"verde"}],"items":[{"id":"hojas-poda","texto":"Hojas y restos de poda","color":"verde"},{"id":"manguera-rota","texto":"Manguera rota y sucia","color":"negro"},{"id":"caja-herramientas","texto":"Caja de cartón limpia","color":"blanco"}]}',
 '{"hojas-poda":"verde","manguera-rota":"negro","caja-herramientas":"blanco"}',
 'La poda es orgánica (verde), el plástico sucio no se recupera (negro) y el cartón limpio es aprovechable (blanca).'),
('SITUACION', 'ELECCION', 'Llegas al punto de disposición y la caneca verde está completamente llena.', '¿Qué haces con tu bolsa de orgánicos?', NULL,
 '{"opciones":[{"id":"o1","texto":"Dejar la bolsa al lado de la caneca"},{"id":"o2","texto":"Abrir la caneca y empujar todo hacia adentro"},{"id":"o3","texto":"Regresar con tu bolsa y avisar a la administración para que la vacíen"},{"id":"o4","texto":"Dejarla en la caneca blanca, que tiene espacio"}]}',
 'o3', 'Dejar bolsas fuera o mezclarlas en otra caneca desordena el punto y contamina. La opción correcta es no ensuciar y avisar a la administración.'),
('SITUACION', 'ELECCION', 'Al organizar un cajón encontraste pilas viejas.', '¿Qué haces con ellas?', NULL,
 '{"opciones":[{"id":"o1","texto":"Caneca negra, como cualquier basura"},{"id":"o2","texto":"Guardadas aparte para llevarlas a un punto de recolección autorizado"},{"id":"o3","texto":"Caneca blanca, son metálicas"},{"id":"o4","texto":"Caneca verde, son naturales"}]}',
 'o2', 'Las pilas son residuos especiales: nunca van en las canecas comunes. Se guardan aparte y se entregan en los puntos de recolección autorizados.'),
('SITUACION', 'ELECCION', 'Encontraste un medicamento vencido en el botiquín.', '¿Cómo lo dispones?', NULL,
 '{"opciones":[{"id":"o1","texto":"Caneca negra, envuelto en papel"},{"id":"o2","texto":"Caneca verde, es natural"},{"id":"o3","texto":"Separado, para llevarlo a un punto o jornada de recolección autorizada"},{"id":"o4","texto":"Caneca blanca, viene en caja de cartón"}]}',
 'o3', 'Los medicamentos vencidos son residuos especiales: se separan y se entregan donde el condominio o el municipio lo indiquen, nunca en las canecas comunes.'),
('SITUACION', 'ELECCION', 'Quieres ayudar a que el punto de residuos del condominio se mantenga ordenado.', '¿Cuál es el comportamiento correcto?', NULL,
 '{"opciones":[{"id":"o1","texto":"Dejar mis bolsas al lado de las canecas si están llenas"},{"id":"o2","texto":"Llevar las bolsas cerradas y depositarlas en su caneca"},{"id":"o3","texto":"Sacar lo aprovechable de las bolsas ajenas"},{"id":"o4","texto":"Usar la caneca que quede más cerca, sin importar el color"}]}',
 'o2', 'El punto se mantiene ordenado cuando cada vivienda entrega bolsas cerradas, en la caneca del color correcto, sin dejar nada fuera.'),
('SITUACION', 'ORDENAR', 'Son las 7 de la noche y juntaste los residuos del día.', 'Ordena tu rutina completa hasta dejar el punto ordenado.', NULL,
 '{"pasos":["Cerrar bien cada bolsa","Llevar las bolsas al punto de disposición","Depositar cada bolsa en su caneca","Revisar que el área quede limpia y ordenada"]}',
 'Cerrar bien cada bolsa|Llevar las bolsas al punto de disposición|Depositar cada bolsa en su caneca|Revisar que el área quede limpia y ordenada',
 'Cerrar las bolsas evita derrames, cada bolsa va en su caneca por color, y una última mirada deja el punto limpio para tus vecinos.');
