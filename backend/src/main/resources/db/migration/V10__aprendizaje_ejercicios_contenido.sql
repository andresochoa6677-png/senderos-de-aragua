-- ============================================================
-- V10: Contenido definitivo del modulo APRENDIZAJE y EJERCICIOS.
--      - leccion: columnas imagen, ejemplo e idea_clave.
--      - UPDATE (no DELETE) de las 6 lecciones y 12 ejercicios:
--        se conservan ids, progreso_leccion e intento_ejercicio.
--      - Regla oficial del condominio: BLANCO / NEGRO / VERDE.
-- ============================================================

ALTER TABLE leccion ADD COLUMN imagen     VARCHAR(255) NULL;
ALTER TABLE leccion ADD COLUMN ejemplo    TEXT         NULL;
ALTER TABLE leccion ADD COLUMN idea_clave VARCHAR(300) NULL;

-- ------------------------------------------------------------
-- LECCION 1: Por que separar residuos
-- ------------------------------------------------------------
UPDATE leccion SET
    titulo = '¿Por qué separar residuos?',
    resumen = 'Qué significa separar en la fuente y por qué mezclar todo complica el manejo de los residuos.',
    imagen = '/images/aprendizaje/leccion-01-separacion.webp',
    idea_clave = 'Separar en la fuente es la primera decisión del manejo de residuos: quien genera el residuo lo clasifica.',
    contenido = '# ¿Por qué separar residuos?

**Separar en la fuente** significa clasificar cada residuo en el momento y lugar donde se genera: tu vivienda.

## Por qué mezclar complica el manejo

- Un residuo aprovechable mezclado con restos de comida **se contamina** y deja de servir.
- Mezclado, todo termina en el mismo destino: la bolsa común.
- Separar después cuesta mucho más que separar en casa.

## Consecuencias de una mala disposición

- Olores y plagas en zonas comunes.
- Contaminación del punto de acopio.
- Más volumen de residuos que enviar al relleno.

## Tu responsabilidad

Cada vivienda decide, en el momento de desechar, si un material se aprovecha o se pierde. Esa decisión es tuya.',
    ejemplo = 'Después del mercado tienes una botella plástica, una cáscara de plátano y una caja de cereal. Si van a la misma bolsa, la caja se humedece y la botella se ensucia: los tres pierden valor. Separados, cada uno va a su recipiente y todos sirven.'
WHERE orden = 1;

-- ------------------------------------------------------------
-- LECCION 2: La regla de los 3 colores
-- ------------------------------------------------------------
UPDATE leccion SET
    titulo = 'La regla de los 3 colores',
    resumen = 'Cada recipiente tiene un color y un propósito: blanco, negro y verde.',
    imagen = '/images/aprendizaje/leccion-02-colores.webp',
    idea_clave = 'El color del recipiente responde una sola pregunta: ¿este residuo se puede aprovechar y de qué forma?',
    contenido = '# La regla de los 3 colores

En Senderos de Aragua la separación se hace con **tres recipientes de color**.

## BLANCO = aprovechables

Botellas plásticas, frascos de vidrio, latas, papel y cartón limpios y secos.

## NEGRO = no aprovechables

Papel higiénico, servilletas usadas, papel sucio, icopor, restos de barrido y todo lo que hoy no puede reciclarse.

## VERDE = orgánicos aprovechables

Restos de alimentos, cáscaras de frutas y verduras. Van al recipiente verde para compostaje.

**No existen colores adicionales:** si un residuo no encaja en estos tres, es un residuo especial y se maneja aparte (lección 5).',
    ejemplo = 'Una lata de atún limpia va al blanco, una servilleta usada al negro y la cáscara de la guayaba al verde. Tres residuos, tres colores, un solo gesto.'
WHERE orden = 2;

-- ------------------------------------------------------------
-- LECCION 3: Preparar tus envases
-- ------------------------------------------------------------
UPDATE leccion SET
    titulo = 'Preparar tus envases',
    resumen = 'Vaciar, enjuagar y dejar listo: tres pasos para que un envase sirva.',
    imagen = '/images/aprendizaje/leccion-03-preparar-envases.webp',
    idea_clave = 'Un envase limpio y seco se aprovecha; un envase con restos contamina todo lo que toca.',
    contenido = '# Preparar tus envases

Antes de llevar un envase al recipiente blanco, prepáralo en tres pasos.

## Paso 1 — Vaciar

Bota el contenido. Un envase con líquido o restos no se puede aprovechar.

## Paso 2 — Enjuagar

Pásalo por agua y retira los restos. No necesita quedar impecable, sí sin residuos.

## Paso 3 — Dejar preparado

Seca, aplasta si puedes y deposítalo en el recipiente blanco, sin tapa si es botella plástica.

Envases así llegan limpios al punto de acopio: sin olores, sin plagas y con más valor.',
    ejemplo = 'Terminaste el jugo de una botella plástica: la vacías en el lavaplatos, la enjuagas, la aplastas y ya está lista para el recipiente blanco. Tres pasos, menos de un minuto.'
WHERE orden = 3;

-- ------------------------------------------------------------
-- LECCION 4: Organicos
-- ------------------------------------------------------------
UPDATE leccion SET
    titulo = 'Orgánicos',
    resumen = 'Qué son los residuos orgánicos, cómo van al recipiente verde y una primera idea del compostaje.',
    imagen = '/images/aprendizaje/leccion-04-organicos.webp',
    idea_clave = 'Un residuo orgánico no es basura: es material que la naturaleza puede convertir en abono.',
    contenido = '# Orgánicos

## Qué son

Son los residuos de origen vegetal o animal que se descomponen naturalmente: restos de alimentos, frutas y verduras.

## Ejemplos

- Cáscaras de plátano, papaya, yuca y huevo.
- Restos de verduras y legumbres.
- Café molido y hojas de plantas.

## Relación con el recipiente verde

Todo orgánico aprovechable va al **recipiente verde**, separado de plásticos y papeles. Mezclado con los demás pierde su valor como abono.

## Compostaje, en una frase

El compostaje es dejar que estos restos se descompongan de forma controlada hasta convertirse en **compost**, un abono natural para plantas y jardines. El condominio se encarga del proceso: tu aporte es separar bien el verde.',
    ejemplo = 'Al preparar la ensalada te sobran cáscaras de zanahoria, tallos de cilantro y una cáscara de papaya. Todo eso va junto al recipiente verde. Ni al blanco ni al negro: es alimento para el compostaje.'
WHERE orden = 4;

-- ------------------------------------------------------------
-- LECCION 5: Residuos especiales
-- ------------------------------------------------------------
UPDATE leccion SET
    titulo = 'Residuos especiales',
    resumen = 'Pilas, medicamentos, aceite usado y electrónicos: no van en los recipientes comunes.',
    imagen = '/images/aprendizaje/leccion-05-especiales.webp',
    idea_clave = 'Un residuo especial mezclado con la bolsa común termina donde no debe: en el ambiente.',
    contenido = '# Residuos especiales

Algunos residuos **no van en ningún recipiente común** (ni blanco, ni negro, ni verde) porque son peligrosos o requieren un manejo particular.

## Cuáles son

- **Pilas y baterías.** Contienen sustancias tóxicas que contaminan suelo y agua.
- **Medicamentos.** Vencidos o sobrantes deben entregarse donde se indique, nunca a la basura ni al sifón.
- **Aceite usado de cocina.** Una cucharada puede contaminar mucha agua; se entrega en envase cerrado.
- **Residuos electrónicos.** Cargadores, cables, pilas de celular o equipos en desuso.

## Qué hacer

Guárdalos por separado y entrégalos en el punto que el condominio indique para cada uno. Consulta la sección Información antes de desecharlos.',
    ejemplo = 'Se te acabaron las pilas del control remoto y tienes un frasco con medicamento vencido. Ninguno de los dos va a la bolsa común: se guardan por separado y se entregan en el punto indicado por la administración.'
WHERE orden = 5;

-- ------------------------------------------------------------
-- LECCION 6: Tu rol en el condominio
-- ------------------------------------------------------------
UPDATE leccion SET
    titulo = 'Tu rol en el condominio',
    resumen = 'Separar, usar bien los recipientes, respetar horarios, cuidar el punto de acopio y reportar.',
    imagen = '/images/aprendizaje/leccion-06-comunidad.webp',
    idea_clave = 'Corresponsabilidad: el buen manejo de los residuos es un acuerdo entre todas las viviendas.',
    contenido = '# Tu rol en el condominio

El punto de acopio funciona porque cada vivienda cumple una parte.

## Tus compromisos

- **Separar correctamente** en la fuente, según la regla de 3 colores.
- **Usar los recipientes** dispuestos: blanco, negro y verde.
- **Respetar los horarios** de salida de residuos.
- **Cuidar el punto de acopio:** dejarlo limpio y ordenado.
- **Reportar problemas** a la administración: derrames, recipientes llenos o dañados.

## Corresponsabilidad

Tu vivienda no es una vecina más: es parte del sistema. Si una vivienda mezcla, todo el esfuerzo de las demás se pierde; si todas separan, el condominio entero gana.',
    ejemplo = 'Es viernes en la tarde y los recipientes del punto de acopio están llenos y uno está caído. Lo correcto: no dejar tu bolsa al lado, reportarlo a la administración y esperar a que se restablezca la recolección.'
WHERE orden = 6;

-- ============================================================
-- EJERCICIOS (2 por leccion, ids estables por leccion_id+orden)
-- ============================================================

-- ------------------------------------------------------------
-- Leccion 1: clasificacion + situacion separar/mezclar
-- ------------------------------------------------------------
UPDATE ejercicio SET
    enunciado = 'En la mesa quedaron tres cosas: una botella plástica vacía, una cáscara de plátano y una caja de cereal. ¿Qué haces con ellas?',
    opciones = '["Cada una va a su recipiente: botella al blanco, cáscara al verde y caja al blanco","Las meto todas en la misma bolsa para sacarlas más rápido","Primero la caja y la botella, la cáscara la dejo para mañana","Solo separo la botella, el resto va junto"]',
    respuesta = 'Cada una va a su recipiente: botella al blanco, cáscara al verde y caja al blanco',
    explicacion = 'Cada residuo tiene su recipiente. Mezclarlos contamina los aprovechables y complica todo el manejo posterior.'
WHERE leccion_id = (SELECT id FROM leccion WHERE orden = 1) AND orden = 1;

UPDATE ejercicio SET
    enunciado = 'Situación: es lunes, ya tienes la bolsa llena y un envase de vidrio suelto. ¿Qué es separar en la fuente?',
    opciones = '["Clasificar los residuos en tu vivienda, donde se generan","Mezclar todo y separarlo en el punto de acopio","Guardar todo en la cocina hasta el fin de semana","Sacar solo lo que huele mal"]',
    respuesta = 'Clasificar los residuos en tu vivienda, donde se generan',
    explicacion = 'Separar en la fuente es clasificar en el lugar donde el residuo se genera: en tu casa, antes de sacarlo.'
WHERE leccion_id = (SELECT id FROM leccion WHERE orden = 1) AND orden = 2;

-- ------------------------------------------------------------
-- Leccion 2: clasificar residuo + detectar clasificacion incorrecta
-- ------------------------------------------------------------
UPDATE ejercicio SET
    enunciado = '¿En qué recipiente va una lata de atún limpia y seca?',
    opciones = '["Blanco","Negro","Verde","A ninguno, va aparte"]',
    respuesta = 'Blanco',
    explicacion = 'El blanco es para aprovechables: metales, plásticos, vidrio, papel y cartón limpios y secos.'
WHERE leccion_id = (SELECT id FROM leccion WHERE orden = 2) AND orden = 1;

UPDATE ejercicio SET
    enunciado = 'Detecta el error: ¿cuál de estas clasificaciones está MAL hecha?',
    opciones = '["Cáscara de papaya en el recipiente verde","Servilleta usada en el recipiente negro","Botella plástica enjuagada en el recipiente negro","Cartón limpio en el recipiente blanco"]',
    respuesta = 'Botella plástica enjuagada en el recipiente negro',
    explicacion = 'La botella plástica enjuagada es un aprovechable: va al blanco, no al negro.'
WHERE leccion_id = (SELECT id FROM leccion WHERE orden = 2) AND orden = 2;

UPDATE ejercicio SET
    tipo = 'OPCION_MULTIPLE'
WHERE leccion_id = (SELECT id FROM leccion WHERE orden = 2) AND orden = 2;

-- ------------------------------------------------------------
-- Leccion 3: ordenar pasos + situacion botella
-- ------------------------------------------------------------
UPDATE ejercicio SET
    tipo = 'ORDENAR',
    enunciado = 'Ordena los pasos para preparar un envase antes de depositarlo en el recipiente blanco.',
    opciones = '["Vaciar el contenido","Enjuagar con agua","Dejarlo preparado y depositarlo"]',
    respuesta = 'Vaciar el contenido|Enjuagar con agua|Dejarlo preparado y depositarlo',
    explicacion = 'El orden correcto es vaciar, enjuagar y dejar listo. Un envase con restos no se puede aprovechar.'
WHERE leccion_id = (SELECT id FROM leccion WHERE orden = 3) AND orden = 1;

UPDATE ejercicio SET
    enunciado = 'Situación: terminaste una botella de refresco que aún tiene líquido y quieres depositarla ya. ¿Qué haces?',
    opciones = '["La vacío, la enjuago, la aplasto y la llevo al recipiente blanco","La echo al blanco tal cual, con el líquido","La echo al negro para que no ensucie","La tapo y la guardo en la cocina varios días"]',
    respuesta = 'La vacío, la enjuago, la aplasto y la llevo al recipiente blanco',
    explicacion = 'Un envase con líquido contamina el recipiente blanco. Vaciar y enjuagar toma menos de un minuto.'
WHERE leccion_id = (SELECT id FROM leccion WHERE orden = 3) AND orden = 2;

-- ------------------------------------------------------------
-- Leccion 4: identificar organicos + detectar error
-- ------------------------------------------------------------
UPDATE ejercicio SET
    enunciado = '¿Cuál de estos residuos es orgánico?',
    opciones = '["Cáscara de zanahoria","Frasco de vidrio","Papel higiénico usado","Icopor de paquete"]',
    respuesta = 'Cáscara de zanahoria',
    explicacion = 'Las cáscaras de frutas y verduras son orgánicos: se descomponen y van al recipiente verde.'
WHERE leccion_id = (SELECT id FROM leccion WHERE orden = 4) AND orden = 1;

UPDATE ejercicio SET
    enunciado = 'Detecta el error: ¿cuál NO va en el recipiente verde?',
    opciones = '["Cáscara de plátano","Tallos de cilantro","Café molido usado","Botella de agua"]',
    respuesta = 'Botella de agua',
    explicacion = 'La botella es un aprovechable y va al blanco. El verde es solo para orgánicos.'
WHERE leccion_id = (SELECT id FROM leccion WHERE orden = 4) AND orden = 2;

-- ------------------------------------------------------------
-- Leccion 5: identificar especiales + situacion
-- ------------------------------------------------------------
UPDATE ejercicio SET
    enunciado = '¿Cuál de estos es un residuo especial?',
    opciones = '["Pila usada del control remoto","Caja de cartón","Botella de vidrio limpia","Restos de verduras"]',
    respuesta = 'Pila usada del control remoto',
    explicacion = 'Las pilas contienen sustancias tóxicas: no van en blanco, negro ni verde; se entregan aparte.'
WHERE leccion_id = (SELECT id FROM leccion WHERE orden = 5) AND orden = 1;

UPDATE ejercicio SET
    enunciado = 'Situación: te sobró aceite de freír y tienes medicamento vencido. ¿Qué haces?',
    opciones = '["Los guardo por separado y los entrego en el punto indicado por el condominio","El aceite lo echo al sifón y el medicamento a la bolsa común","Los pongo en la bolsa negra junto al resto","El aceite al recipiente verde porque es de comida"]',
    respuesta = 'Los guardo por separado y los entrego en el punto indicado por el condominio',
    explicacion = 'Aceite y medicamentos son residuos especiales: jamás al sifón, ni a la bolsa común, ni al verde.'
WHERE leccion_id = (SELECT id FROM leccion WHERE orden = 5) AND orden = 2;

-- ------------------------------------------------------------
-- Leccion 6: situacion comunitaria + detectar error
-- ------------------------------------------------------------
UPDATE ejercicio SET
    enunciado = 'Situación: llegas al punto de acopio y el recipiente blanco está lleno. ¿Qué haces?',
    opciones = '["Dejo mi material aparte, sin ensuciar, y reporto el recipiente lleno a la administración","Lo meto a la fuerza para que quepa","Lo echo al recipiente negro","Me lo llevo de regreso y lo boto en la calle"]',
    respuesta = 'Dejo mi material aparte, sin ensuciar, y reporto el recipiente lleno a la administración',
    explicacion = 'Cuidar el punto de acopio y reportar problemas es parte de la corresponsabilidad de tu vivienda.'
WHERE leccion_id = (SELECT id FROM leccion WHERE orden = 6) AND orden = 1;

UPDATE ejercicio SET
    enunciado = 'Detecta el error: ¿qué conducta rompe la corresponsabilidad en el condominio?',
    opciones = '["Sacar los residuos fuera del horario establecido","Separar en la fuente según los 3 colores","Enjuagar los envases antes de reciclarlos","Reportar un derrame en el punto de acopio"]',
    respuesta = 'Sacar los residuos fuera del horario establecido',
    explicacion = 'Respetar los horarios mantiene el punto de acopio limpio y ordenado para todas las viviendas.'
WHERE leccion_id = (SELECT id FROM leccion WHERE orden = 6) AND orden = 2;

-- ------------------------------------------------------------
-- Normalizacion de tipos: todos los ejercicios usan opciones
-- de texto (4 opciones), por lo que se califican como
-- OPCION_MULTIPLE salvo el de ordenar pasos de la leccion 3.
-- ------------------------------------------------------------
UPDATE ejercicio SET tipo = 'OPCION_MULTIPLE'
WHERE tipo = 'VERDADERO_FALSO';
