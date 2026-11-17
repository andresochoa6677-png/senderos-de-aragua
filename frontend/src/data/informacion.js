/**
 * Contenido del módulo de INFORMACIÓN (zona de consulta del residente).
 * Datos estructurados, coherentes con las 6 lecciones y la regla del
 * proyecto: BLANCO = aprovechables, NEGRO = no aprovechables,
 * VERDE = orgánicos aprovechables. Los especiales NO van en las tres
 * canecas convencionales: requieren manejo/punto de disposición especial.
 */

export const CATEGORIAS = [
  {
    id: 'blanco',
    nombre: 'Caneca blanca',
    tipo: 'Aprovechables',
    color: 'blanco',
    imagen: '/images/informacion/info-aprovechables.webp',
    queEs: 'Materiales limpios y secos que pueden recuperarse y transformarse en nuevos productos.',
    ejemplos: ['Botellas plásticas', 'Vidrio (frascos, botellas)', 'Latas y metales', 'Papel y cartón limpios'],
    errores: [
      'Meter envases con restos de comida o líquidos',
      'Incluir servilletas o papel higiénico "por si acaso"'
    ],
    queHacer: 'Vacía y enjuaga los envases antes de depositarlos. Si un material está contaminado con grasa o comida, ya no es aprovechable: va en la caneca negra.'
  },
  {
    id: 'negro',
    nombre: 'Caneca negra',
    tipo: 'No aprovechables',
    color: 'negro',
    imagen: '/images/informacion/info-no-aprovechables.webp',
    queEs: 'Residuos que nadie puede recuperar: están contaminados, son higiénicos o no tienen uso posterior.',
    ejemplos: ['Servilletas y papel higiénico usados', 'Empaques contaminados con comida', 'Icopor y plásticos sucios', 'Residuos sanitarios (pañales)'],
    errores: [
      'Ponerlos en la caneca blanca "por si acaso": un solo residuo sucio contamina una bolsa completa',
      'Mezclar restos de comida aquí: los orgánicos van en la verde'
    ],
    queHacer: 'Entrégalos en bolsa bien cerrada para proteger a quienes manejan los residuos y mantener el punto limpio.'
  },
  {
    id: 'verde',
    nombre: 'Caneca verde',
    tipo: 'Orgánicos aprovechables',
    color: 'verde',
    imagen: '/images/informacion/info-organicos.webp',
    queEs: 'Restos de origen natural que pueden convertirse en abono (compostaje).',
    ejemplos: ['Cáscaras de frutas y verduras', 'Restos de comida', 'Cáscara de huevo', 'Residuos de jardín'],
    errores: [
      'Confundirlos con basura y enviarlos a la caneca negra',
      'Incluir empaques plásticos junto a los restos'
    ],
    queHacer: 'Deposita solo restos vegetales y de cocina, sin bolsas ni empaques plásticos mezclados.'
  }
]

export const RESIDUOS = [
  // --- Blanco · aprovechables ---
  { id: 'botella-plastica', nombre: 'Botella plástica', categoria: 'blanco', descripcion: 'Envase de plástico de bebidas o productos de aseo.', preparacion: 'Vacía todo el contenido, enjuágala y déjala escurrir antes de llevarla.', keywords: ['plastico', 'pet', 'envase', 'botella de agua', 'gaseosa'] },
  { id: 'lata', nombre: 'Lata de aluminio', categoria: 'blanco', descripcion: 'Latas de bebidas o conservas.', preparacion: 'Enjuágala para quitar restos y déjala escurrir.', keywords: ['aluminio', 'metal', 'atun', 'refresco', 'lata'] },
  { id: 'frasco-vidrio', nombre: 'Frasco de vidrio', categoria: 'blanco', descripcion: 'Frascos y botellas de vidrio de conservas o bebidas.', preparacion: 'Vacíalo y enjuágalo antes de depositarlo.', keywords: ['vidrio', 'frasco', 'mermelada', 'botella de vidrio'] },
  { id: 'papel-limpio', nombre: 'Papel y periódico limpio', categoria: 'blanco', descripcion: 'Hojas, cuadernos, periódicos y papel seco sin contaminar.', preparacion: 'Debe estar seco y limpio; si está manchado con comida va en la negra.', keywords: ['papel', 'periodico', 'hojas', 'cuaderno'] },
  { id: 'carton-limpio', nombre: 'Cartón', categoria: 'blanco', descripcion: 'Cajas y cartón limpios y secos.', preparacion: 'Desdóbblalo para ocupar menos espacio; debe estar sin grasa ni comida.', keywords: ['carton', 'caja', 'corrugado'] },
  { id: 'envase-detergente', nombre: 'Envase de detergente o aseo', categoria: 'blanco', descripcion: 'Envases plásticos de productos de limpieza.', preparacion: 'Vacíalo y enjuágalo bien antes de llevarlo.', keywords: ['detergente', 'aseo', 'limpieza', 'shampoo'] },
  // --- Verde · orgánicos aprovechables ---
  { id: 'cascara-fruta', nombre: 'Cáscara de fruta', categoria: 'verde', descripcion: 'Cáscaras de banano, naranja, manzana y otras frutas.', keywords: ['fruta', 'cascara', 'banana', 'platano', 'naranja'] },
  { id: 'restos-comida', nombre: 'Restos de comida', categoria: 'verde', descripcion: 'Sobras de alimentos de origen vegetal o animal de la cocina.', keywords: ['comida', 'sobras', 'cocina', 'alimentos'] },
  { id: 'cascara-huevo', nombre: 'Cáscara de huevo', categoria: 'verde', descripcion: 'Cáscaras de huevo de cocina.', keywords: ['huevo', 'cascara'] },
  { id: 'restos-verduras', nombre: 'Restos de verduras', categoria: 'verde', descripcion: 'Tallos, hojas y sobras de verduras de la preparación de alimentos.', keywords: ['verduras', 'vegetales', 'ensalada', 'lechuga'] },
  { id: 'hojas-jardin', nombre: 'Hojas y residuos de jardín', categoria: 'verde', descripcion: 'Hojas secas, pasto y restos de poda del jardín.', keywords: ['jardin', 'hojas', 'poda', 'cesped'] },
  // --- Negro · no aprovechables ---
  { id: 'papel-higienico', nombre: 'Papel higiénico usado', categoria: 'negro', descripcion: 'Papel higiénico usado, aunque sea de papel.', keywords: ['higienico', 'papel higienico'] },
  { id: 'servilleta-usada', nombre: 'Servilleta usada', categoria: 'negro', descripcion: 'Servilletas y pañuelos de papel usados en comidas.', preparacion: 'Ninguna: va directo a la caneca negra, en bolsa cerrada.', keywords: ['servilleta', 'panuelo', 'papel usado'] },
  { id: 'panal', nombre: 'Pañal desechable', categoria: 'negro', descripcion: 'Pañales usados y residuos sanitarios.', preparacion: 'Entrégalo en bolsa cerrada para proteger a quienes manipulan los residuos.', keywords: ['panal', 'sanitario', 'desechable'] },
  { id: 'empaque-contaminado', nombre: 'Empaque contaminado con comida', categoria: 'negro', descripcion: 'Cajas, bandejas o empaques manchados con grasa o restos.', keywords: ['empaque', 'contaminado', 'grasa', 'bandeja', 'yogur'] },
  { id: 'caja-pizza-grasosa', nombre: 'Caja de pizza con grasa', categoria: 'negro', descripcion: 'El cartón contaminado con grasa pierde su valor de reciclaje.', keywords: ['pizza', 'caja grasosa'] },
  { id: 'icopor', nombre: 'Icopor', categoria: 'negro', descripcion: 'Icopor y pláticos de espuma: no se recuperan en el flujo del condominio.', keywords: ['icopor', 'espuma', 'unicel'] },
  { id: 'bolsa-sucia', nombre: 'Bolsa plástica sucia', categoria: 'negro', descripcion: 'Bolsas de alimentos con migas o grasa: si no está limpia y seca, va en la negra.', keywords: ['bolsa', 'plastico sucio'] },
  // --- Especiales · NO van en las tres canecas ---
  { id: 'pilas', nombre: 'Pilas y baterías', categoria: 'especial', descripcion: 'Pilas comunes, recargables y baterías: contienen metales tóxicos.', keywords: ['pila', 'bateria', 'pilas gastadas'] },
  { id: 'bombillo', nombre: 'Bombillos y luminarias', categoria: 'especial', descripcion: 'Bombillos fundidos y tubos fluorescentes.', keywords: ['bombillo', 'luminaria', 'fluorescente'] },
  { id: 'electronicos', nombre: 'Pequeños aparatos electrónicos', categoria: 'especial', descripcion: 'Celulares, cargadores, audífonos y cables dañados.', keywords: ['electronico', 'celular', 'cargador', 'audifonos', 'tecnologia'] },
  { id: 'medicamentos', nombre: 'Medicamentos vencidos', categoria: 'especial', descripcion: 'Medicamentos vencidos o que ya no se usan.', keywords: ['medicamento', 'vencido', 'farmacia'] },
  { id: 'aceite-cocina', nombre: 'Aceite de cocina usado', categoria: 'especial', descripcion: 'Aceite de freír o de cocina: nunca lo viertas al desagüe ni a las canecas.', keywords: ['aceite', 'freir', 'cocina'] }
]

export const NORMATIVA = [
  {
    nombre: 'Resolución 2184 de 2019',
    relacion: 'Regula la gestión integral de residuos sólidos en Colombia.',
    relevancia: 'Es la base de la separación en la fuente y del aprovechamiento con los tres colores que usa el condominio.'
  },
  {
    nombre: 'Decreto 1077 de 2015',
    relacion: 'Decreto Único Reglamentario del Sector Vivienda, Ciudad y Territorio.',
    relevancia: 'Compila las normas de aseo y manejo de residuos que orientan el servicio en los conjuntos residenciales.'
  },
  {
    nombre: 'Ley 142 de 1994',
    relacion: 'Régimen de servicios públicos domiciliarios.',
    relevancia: 'Define los deberes de usuarios y prestadores del servicio de aseo, incluyendo el manejo adecuado de los residuos.'
  },
  {
    nombre: 'PGIRS de Yopal 2025–2030',
    relacion: 'Plan de Gestión Integral de Residuos Sólidos del municipio.',
    relevancia: 'Orienta las decisiones locales sobre prevención, separación y aprovechamiento de residuos en Yopal.'
  }
]

export const HORARIOS = {
  dias: 'Martes, jueves y sábados',
  horario: 'En la mañana',
  nota: 'Horario de recolección de residuos del condominio: lleva tus residuos al punto de disposición esas mañanas.'
}

export const IMAGENES = {
  portada: '/images/informacion/info-portada.webp',
  tresCategorias: '/images/informacion/info-tres-categorias.webp',
  especiales: '/images/informacion/info-residuos-especiales.webp',
  preparar: '/images/informacion/info-preparar-residuos.webp',
  antesDespues: '/images/informacion/info-antes-despues.webp',
  puntoSenderos: '/images/informacion/info-punto-senderos.webp',
  rutaCorrecta: '/images/informacion/info-ruta-correcta.webp',
  horarios: '/images/informacion/info-horarios.webp',
  normativa: '/images/informacion/info-normativa.webp'
}

/** Normaliza texto: minúsculas y sin acentos, para búsquedas tolerantes. */
export function normalizar(texto) {
  return (texto || '')
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .trim()
}

/** Búsqueda tolerante a mayúsculas, acentos y coincidencias parciales. */
export function buscarResiduos(consulta) {
  const q = normalizar(consulta)
  if (!q) return []
  return RESIDUOS.filter((r) =>
    normalizar(r.nombre).includes(q) ||
    normalizar(r.descripcion).includes(q) ||
    (r.keywords || []).some((k) => normalizar(k).includes(q))
  )
}
