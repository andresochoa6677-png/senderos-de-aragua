-- ============================================================
-- V12: Rediseño del módulo educativo — actividades interactivas.
--      1) Nuevo tipo de ejercicio CLASIFICAR (clasificación visual
--         elemento -> destino) y columnas de respuesta más anchas.
--      2) Reset de progreso e intentos SOLO de la cuenta demo, para
--         mostrar el nuevo recorrido desde cero.
--      3) Contenido nuevo de las 6 lecciones (títulos, resúmenes,
--         contenido markdown y portadas WebP optimizadas).
--      4) Actividades de cierre por lección: las lecciones se
--         completan resolviendo su actividad (regla de los 3 colores:
--         blanco = aprovechables, negro = no aprovechables,
--         verde = orgánicos aprovechables).
-- ============================================================

-- 1) El nuevo tipo CLASIFICAR pasa la validación de tipos.
ALTER TABLE ejercicio DROP CONSTRAINT ck_ejercicio_tipo;
ALTER TABLE ejercicio
    ADD CONSTRAINT ck_ejercicio_tipo CHECK (
        tipo IN ('OPCION_MULTIPLE', 'VERDADERO_FALSO', 'ORDENAR', 'CLASIFICAR'));

-- Los mapas elemento -> destino se guardan como JSON y superan los 120 caracteres.
ALTER TABLE ejercicio MODIFY respuesta VARCHAR(1000) NOT NULL;
ALTER TABLE intento_ejercicio MODIFY respuesta VARCHAR(1000) NOT NULL;

-- 2) Reset de la cuenta DEMO (vivienda demo únicamente; residentes reales intactos).
DELETE FROM intento_ejercicio
WHERE vivienda_id = (SELECT id FROM (SELECT id FROM vivienda WHERE demo = 1 LIMIT 1) d);
DELETE FROM progreso_leccion
WHERE vivienda_id = (SELECT id FROM (SELECT id FROM vivienda WHERE demo = 1 LIMIT 1) d);

-- 3) Contenido de las lecciones.
UPDATE leccion SET
    titulo = 'El problema empieza en casa',
    resumen = 'Por qué separar desde tu vivienda mantiene el punto de residuos del condominio limpio y ordenado.',
    imagen = '/images/aprendizaje/leccion-01-problema.webp',
    contenido = '# El problema empieza en casa

Cuando los residuos salen mezclados de las viviendas, llegan al punto ecológico contaminados entre sí: el cartón manchado con comida ya nadie lo recupera, y los olores y líquidos terminan afectando a todo el condominio.

## Lo que ocurre cuando no separamos
- Bolsas mezcladas: restos de comida junto con plásticos, cartón y vidrio.
- Residuos fuera de las canecas y bolsas abandonadas junto al punto.
- Malos olores, líquidos y suciedad en un área que todos compartimos.
- Atracción de insectos, roedores y aves.
- Material aprovechable que se contamina y se pierde.

## Qué gana el condominio cuando separas desde casa
- El punto de residuos se mantiene limpio, ordenado y sin olores.
- Los aprovechables llegan en buen estado y sí pueden recuperarse.
- Menos volumen hacia el relleno sanitario.

> La separación correcta no comienza en el punto ecológico: comienza en tu cocina.'
WHERE orden = 1;

UPDATE leccion SET
    titulo = 'La regla de los tres colores',
    resumen = 'Blanco = aprovechables, negro = no aprovechables, verde = orgánicos aprovechables. La regla del condominio.',
    imagen = '/images/aprendizaje/leccion-02-tres-colores.webp',
    contenido = '# La regla de los tres colores

En Senderos de Aragua separamos con tres canecas. La regla es sencilla: cada color tiene un único trabajo.

## Caneca blanca — Aprovechables
Materiales limpios y secos que sí se pueden recuperar:
- Plásticos: botellas y envases.
- Vidrio: frascos y botellas.
- Latas y metales.
- Papel y cartón limpios.

## Caneca negra — No aprovechables
Residuos que ya no se pueden recuperar:
- Servilletas y papel higiénico usados.
- Empaques contaminados con comida.
- Icopor y plásticos sucios.
- Residuos sanitarios.

## Caneca verde — Orgánicos aprovechables
Restos de origen natural que pueden convertirse en abono:
- Cáscaras de frutas y verduras.
- Restos de comida.
- Residuos de jardín.

> Si dudas entre blanco y negro, pregúntate: ¿está limpio y seco? Si la respuesta es no, va en la negra.'
WHERE orden = 2;

UPDATE leccion SET
    titulo = 'Preparar tus envases',
    resumen = 'Un aprovechable solo sirve si llega limpio y vacío: aprende a preparar tus envases.',
    imagen = '/images/aprendizaje/leccion-03-preparar-envases.webp',
    contenido = '# Preparar tus envases

Un envase aprovechable que llega sucio contamina todo lo demás. Por eso, antes de llevarlo al punto, debes prepararlo.

## Los pasos
1. Vacía todo el contenido: nada de líquidos ni restos.
2. Enjuaga con agua para quitar los residuos.
3. Déjalo escurrir: limpio y sin goteos.
4. Depósitalo en la caneca blanca.

## ¿Cuándo está listo?
Un envase está preparado cuando está limpio, vacío y no gotea. Si aún tiene restos o líquido, todavía necesita preparación: un envase sucio es un residuo contaminado.

> Un envase limpio vale doble: se puede recuperar y no contamina a los demás.'
WHERE orden = 3;

UPDATE leccion SET
    titulo = 'No aprovechables',
    resumen = 'Reconoce lo que nadie puede recuperar y evita contaminar los aprovechables.',
    imagen = '/images/aprendizaje/leccion-04-no-aprovechables.webp',
    contenido = '# No aprovechables

Los no aprovechables son residuos que nadie puede recuperar: están contaminados, son higiénicos o simplemente no tienen uso posterior.

## Ejemplos frecuentes en casa
- Servilletas y pañuelos usados.
- Papel higiénico.
- Cajas y empaques manchados con comida.
- Icopor y plásticos sucios.
- Residuos sanitarios.

## El error más común
Meterlos en la caneca blanca por si acaso. Un solo residuo sucio puede contaminar una bolsa completa de aprovechables y enviarla toda al relleno.

## Los orgánicos no son basura
Los restos de comida y las cáscaras no van con los no aprovechables: son orgánicos aprovechables y van en la caneca verde.

> Caneca negra: lo que sí es desecho. Caneca verde: lo orgánico. Caneca blanca: solo lo limpio y seco.'
WHERE orden = 4;

UPDATE leccion SET
    titulo = 'Residuos especiales',
    resumen = 'Pilas, bombillos y electrónicos no van en las canecas comunes: aprende dónde va cada uno.',
    imagen = '/images/aprendizaje/leccion-05-especiales.webp',
    contenido = '# Residuos especiales

Algunos residuos domésticos requieren un manejo diferente: no van en las tres canecas comunes porque pueden ser tóxicos o dañar el entorno.

## Ejemplos frecuentes
- Pilas y baterías.
- Bombillos y luminarias.
- Pequeños aparatos electrónicos dañados.
- Medicamentos vencidos.
- Aceites de cocina usados.

## La regla
Los residuos especiales nunca se mezclan con las canecas comunes. Se entregan en los puntos o jornadas de recolección autorizados que el condominio y el municipio informen.

> Acumular especiales en casa también es un riesgo. Guárdalos en un recipiente aparte y llévalos a su punto de recolección.'
WHERE orden = 5;

UPDATE leccion SET
    titulo = 'Tu rol en el condominio',
    resumen = 'El recorrido completo: separar, preparar, llevar al punto y dejarlo ordenado.',
    imagen = '/images/aprendizaje/leccion-06-llevar-al-punto.webp',
    contenido = '# Tu rol en el condominio

Ya sabes separar, preparar y diferenciar los especiales. Ahora, todo junto: el recorrido completo de tus residuos.

## El recorrido completo
1. Separa en tu vivienda: blanco, negro y verde.
2. Prepara los aprovechables: limpios, vacíos y secos.
3. Guarda los especiales aparte para su punto de recolección.
4. Lleva tus bolsas al punto de disposición del condominio.
5. Deposita cada bolsa en su caneca y deja el área ordenada.

## Por qué importa tu parte
El punto ecológico es de todos: cuando cada vivienda entrega bien, el área se mantiene limpia, sin olores y sin molestias, y los materiales sí llegan a aprovecharse.

> Un condominio más sostenible se construye con gestos pequeños y constantes, como cerrar bien las bolsas y respetar las canecas.'
WHERE orden = 6;

-- 4) Actividades de cierre: se reemplazan los 12 ejercicios tipo examen
--    por actividades prácticas (una o dos por leccion).
DELETE FROM ejercicio;

INSERT INTO ejercicio (leccion_id, orden, tipo, enunciado, explicacion, opciones, respuesta, puntos, activo) VALUES
-- L1 · Detecta los errores (seleccion de problemas sobre la foto)
(1, 1, 'CLASIFICAR',
 'Mira la foto de nuestro punto de residuos. Marca todo lo que sí es un problema para el condominio.',
 'Bolsas fuera de las canecas, residuos mezclados, suciedad y aprovechables en la caneca equivocada son problemas. El techo y las etiquetas de las canecas son cosas que ya tenemos bien.',
 '{"modo":"SELECCION","tablero":"/images/aprendizaje/leccion-01-problema.webp","destinos":[{"id":"PROBLEMA","etiqueta":"Sí es un problema"},{"id":"OK","etiqueta":"No es un problema"}],"items":[{"id":"residuos-fuera","texto":"Bolsas y residuos fuera de las canecas"},{"id":"bolsa-mezclada","texto":"Bolsas con residuos mezclados"},{"id":"area-sucia","texto":"Suciedad, líquidos y malos olores en el área"},{"id":"aprovechable-en-negro","texto":"Plásticos y cartón terminaron en la caneca negra"},{"id":"canecas-etiquetadas","texto":"Las canecas están identificadas por color"},{"id":"punto-con-techo","texto":"El punto cuenta con techo y señalética"}],"refuerzo":{"imagen":"/images/actividades/l1-comparacion.webp","texto":"Así queremos encontrar nuestro punto de residuos."}}',
 '{"residuos-fuera":"PROBLEMA","bolsa-mezclada":"PROBLEMA","area-sucia":"PROBLEMA","aprovechable-en-negro":"PROBLEMA","canecas-etiquetadas":"OK","punto-con-techo":"OK"}',
 10, 1),
-- L2 · Separa los residuos (clasificacion en 3 colores)
(2, 1, 'CLASIFICAR',
 'Toca un residuo y luego la caneca donde va. Puedes corregir antes de comprobar.',
 'Blanco para aprovechables limpios y secos; negro para no aprovechables; verde para orgánicos. Recuerda: los restos de comida son orgánicos.',
 '{"modo":"CANECAS","tablero":"/images/aprendizaje/leccion-02-tres-colores.webp","destinos":[{"id":"blanco","etiqueta":"Blanco · Aprovechables","color":"blanco","imagen":"/images/actividades/l2-aprovechables.webp"},{"id":"negro","etiqueta":"Negro · No aprovechables","color":"negro","imagen":"/images/actividades/l2-no-aprovechables.webp"},{"id":"verde","etiqueta":"Verde · Orgánicos","color":"verde","imagen":"/images/actividades/l2-organicos.webp"}],"items":[{"id":"botella-plastica","texto":"Botella plástica limpia"},{"id":"lata","texto":"Lata de aluminio"},{"id":"caja-carton","texto":"Caja de cartón"},{"id":"frasco-vidrio","texto":"Frasco de vidrio"},{"id":"servilleta-usada","texto":"Servilleta usada"},{"id":"papel-higienico","texto":"Papel higiénico"},{"id":"empaque-contaminado","texto":"Empaque grasoso de comida"},{"id":"cascara-fruta","texto":"Cáscara de fruta"},{"id":"restos-verduras","texto":"Restos de verduras"},{"id":"restos-comida","texto":"Restos de comida"}]}',
 '{"botella-plastica":"blanco","lata":"blanco","caja-carton":"blanco","frasco-vidrio":"blanco","servilleta-usada":"negro","papel-higienico":"negro","empaque-contaminado":"negro","cascara-fruta":"verde","restos-verduras":"verde","restos-comida":"verde"}',
 20, 1),
-- L3 · Prepara el envase (ordenar pasos + clasificar envases)
(3, 1, 'ORDENAR',
 'Ordena los pasos para preparar un envase antes de llevarlo al punto.',
 'Primero vaciar, luego enjuagar, después dejarlo escurrir y solo al final depositarlo en la caneca blanca.',
 '["Vaciar todo el contenido","Enjuagar con agua","Dejarlo limpio y escurrido","Depositarlo en la caneca blanca"]',
 'Vaciar todo el contenido|Enjuagar con agua|Dejarlo limpio y escurrido|Depositarlo en la caneca blanca',
 10, 1),
(3, 2, 'CLASIFICAR',
 '¿Está listo para la caneca blanca? Clasifica cada envase.',
 'Un envase está preparado cuando está limpio, vacío y no gotea. Con restos o líquido todavía necesita preparación.',
 '{"modo":"DOS_DESTINOS","tablero":"/images/aprendizaje/leccion-03-preparar-envases.webp","destinos":[{"id":"PREPARADO","etiqueta":"Está preparado","color":"verde"},{"id":"NECESITA_PREPARACION","etiqueta":"Aún necesita preparación","color":"rojo"}],"items":[{"id":"botella-sucia","texto":"Botella con restos de bebida","imagen":"/images/actividades/l3-envase-sucio.webp"},{"id":"envases-listos","texto":"Envases limpios, vacíos y secos","imagen":"/images/actividades/l3-envases-preparados.webp"},{"id":"frasco-con-liquido","texto":"Frasco de vidrio aún con líquido"},{"id":"lata-limpia","texto":"Lata vacía, limpia y seca"}]}',
 '{"botella-sucia":"NECESITA_PREPARACION","envases-listos":"PREPARADO","frasco-con-liquido":"NECESITA_PREPARACION","lata-limpia":"PREPARADO"}',
 10, 1),
-- L4 · Corrige la bolsa (clasificacion corrigiendo la bolsa mezclada)
(4, 1, 'CLASIFICAR',
 'Esta bolsa llegó mezclada al punto. Dinos dónde va cada residuo para separarla bien.',
 'La botella, la lata y el cartón son aprovechables; las servilletas, el icopor y la bandeja grasosa son no aprovechables; los restos de comida son orgánicos y van al verde.',
 '{"modo":"CANECAS","tablero":"/images/aprendizaje/leccion-04-no-aprovechables.webp","destinos":[{"id":"blanco","etiqueta":"Blanco · Aprovechables","color":"blanco"},{"id":"negro","etiqueta":"Negro · No aprovechables","color":"negro"},{"id":"verde","etiqueta":"Verde · Orgánicos","color":"verde"}],"items":[{"id":"botella-plastica","texto":"Botella plástica"},{"id":"lata-aluminio","texto":"Lata de aluminio"},{"id":"caja-carton","texto":"Caja de cartón"},{"id":"servilletas-usadas","texto":"Servilletas usadas"},{"id":"icopor","texto":"Icopor de empaque"},{"id":"bandeja-grasosa","texto":"Bandeja con grasa de comida"},{"id":"restos-comida","texto":"Restos de comida y cáscaras"}]}',
 '{"botella-plastica":"blanco","lata-aluminio":"blanco","caja-carton":"blanco","servilletas-usadas":"negro","icopor":"negro","bandeja-grasosa":"negro","restos-comida":"verde"}',
 20, 1),
-- L5 · Donde va (comunes vs especiales)
(5, 1, 'CLASIFICAR',
 '¿Dónde va cada residuo? Recuerda que los especiales nunca van con las canecas comunes.',
 'Pilas, bombillos, electrónicos y medicamentos requieren manejo especial: se entregan en los puntos de recolección autorizados. Lo común va en blanco, negro o verde.',
 '{"modo":"DOS_DESTINOS","tablero":"/images/aprendizaje/leccion-05-especiales.webp","destinos":[{"id":"COMUNES","etiqueta":"Canecas comunes (blanco, negro o verde)","color":"verde"},{"id":"ESPECIAL","etiqueta":"Manejo especial · punto autorizado","color":"morado"}],"items":[{"id":"pila","texto":"Pilas gastadas","imagen":"/images/actividades/l5-residuos-especiales.webp"},{"id":"bombillo","texto":"Bombillo quemado"},{"id":"celular-danado","texto":"Celular dañado"},{"id":"medicamento-vencido","texto":"Medicamentos vencidos"},{"id":"botella-limpia","texto":"Botella plástica limpia"},{"id":"cascara-platano","texto":"Cáscara de plátano"}]}',
 '{"pila":"ESPECIAL","bombillo":"ESPECIAL","celular-danado":"ESPECIAL","medicamento-vencido":"ESPECIAL","botella-limpia":"COMUNES","cascara-platano":"COMUNES"}',
 20, 1),
-- L6 · Integradora (ordenar el recorrido + entregar bolsas)
(6, 1, 'ORDENAR',
 'Ordena el recorrido completo de tus residuos, desde casa hasta el punto.',
 'Separar en casa, preparar, llevar al punto, depositar cada bolsa en su caneca y dejar el área ordenada.',
 '["Separar los residuos en la vivienda","Preparar los aprovechables: limpios y secos","Llevar las bolsas al punto de disposición","Depositar cada bolsa en su caneca","Dejar el punto limpio y ordenado"]',
 'Separar los residuos en la vivienda|Preparar los aprovechables: limpios y secos|Llevar las bolsas al punto de disposición|Depositar cada bolsa en su caneca|Dejar el punto limpio y ordenado',
 10, 1),
(6, 2, 'CLASIFICAR',
 'Llegaste al punto con tus tres bolsas. Deposita cada bolsa en su caneca.',
 'La bolsa blanca con aprovechables va en la caneca blanca; la negra con no aprovechables, en la negra; la verde con orgánicos, en la verde.',
 '{"modo":"CANECAS","tablero":"/images/aprendizaje/leccion-06-llevar-al-punto.webp","destinos":[{"id":"blanco","etiqueta":"Blanco · Aprovechables","color":"blanco"},{"id":"negro","etiqueta":"Negro · No aprovechables","color":"negro"},{"id":"verde","etiqueta":"Verde · Orgánicos","color":"verde"}],"items":[{"id":"bolsa-blanca","texto":"Bolsa blanca: aprovechables limpios y secos","color":"blanco"},{"id":"bolsa-negra","texto":"Bolsa negra: no aprovechables","color":"negro"},{"id":"bolsa-verde","texto":"Bolsa verde: orgánicos","color":"verde"}],"refuerzo":{"imagen":"/images/actividades/l6-punto-final-ordenado.webp","texto":"Así contribuimos entre todos a mantener nuestro punto de residuos organizado."}}',
 '{"bolsa-blanca":"blanco","bolsa-negra":"negro","bolsa-verde":"verde"}',
 20, 1);
