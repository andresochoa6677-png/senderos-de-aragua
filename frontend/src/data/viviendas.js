/**
 * Censo oficial de viviendas del Condominio Residencial Senderos de
 * Aragua (identificación BLOQUE-NUMERO: A-1, G-5, Ñ-2…). Todas las
 * viviendas son casas.
 *
 * El orden de este arreglo es el que se muestra en el desplegable del
 * registro: alfabético en español (Ñ después de N, RR después de R).
 * La fuente de verdad de qué viviendas existen y están libres vive en
 * la base de datos (migración V25); este archivo solo define las
 * opciones del formulario, que el backend revalida al registrarse.
 */

export const BLOQUES = [
  { bloque: 'A', casas: 4 },
  { bloque: 'B', casas: 4 },
  { bloque: 'C', casas: 4 },
  { bloque: 'D', casas: 4 },
  { bloque: 'E', casas: 4 },
  { bloque: 'F', casas: 4 },
  { bloque: 'G', casas: 12 },
  { bloque: 'H', casas: 4 },
  { bloque: 'I', casas: 4 },
  { bloque: 'J', casas: 4 },
  { bloque: 'K', casas: 4 },
  { bloque: 'L', casas: 4 },
  { bloque: 'M', casas: 4 },
  { bloque: 'N', casas: 18 },
  { bloque: 'Ñ', casas: 4 },
  { bloque: 'O', casas: 4 },
  { bloque: 'P', casas: 4 },
  { bloque: 'Q', casas: 4 },
  { bloque: 'R', casas: 4 },
  { bloque: 'RR', casas: 4 },
  { bloque: 'S', casas: 4 },
  { bloque: 'T', casas: 4 },
  { bloque: 'U', casas: 4 },
  { bloque: 'V', casas: 4 },
  { bloque: 'W', casas: 4 },
  { bloque: 'X', casas: 4 },
  { bloque: 'Y', casas: 4 },
  { bloque: 'Z', casas: 9 }
]

/** Números de casa disponibles en un bloque (1…casas, ascendente). */
export function numerosDeBloque(bloque) {
  const b = BLOQUES.find((x) => x.bloque === bloque)
  return b ? Array.from({ length: b.casas }, (_, i) => i + 1) : []
}

/** Total de viviendas del censo (139). */
export const TOTAL_VIVIENDAS = BLOQUES.reduce((s, b) => s + b.casas, 0)
