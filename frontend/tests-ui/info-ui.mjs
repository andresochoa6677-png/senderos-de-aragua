// Prueba de humo UI del módulo de Información: secciones, búsqueda
// parcial, especiales y estado sin resultados. Sin llamadas al backend.
import { Window } from 'happy-dom'
import React from 'react'
import { createRoot } from 'react-dom/client'
import Informacion from '../src/pages/Informacion.jsx'

const window = new Window({ url: 'http://localhost:5173/informacion' })
globalThis.window = window
globalThis.document = window.document
Object.defineProperty(globalThis, 'navigator', { value: window.navigator, configurable: true })
globalThis.HTMLElement = window.HTMLElement
globalThis.Node = window.Node
globalThis.Event = window.Event
globalThis.MutationObserver = window.MutationObserver

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
async function esperar(condicion, descripcion, maxMs = 6000) {
  const inicio = Date.now()
  while (Date.now() - inicio < maxMs) {
    if (condicion()) return
    await sleep(50)
  }
  throw new Error('TIEMPO AGOTADO esperando: ' + descripcion)
}

const contenedor = document.createElement('div')
document.body.appendChild(contenedor)
const root = createRoot(contenedor)
root.render(<Informacion />)

const texto = () => document.body.textContent || ''
const input = () => document.querySelector('#buscar-residuo')
document.addEventListener('input', (e) =>
  console.log('[harness] evento input visto, valor=', JSON.stringify(e.target.value)))
// Para inputs controlados de React hay que usar el setter nativo del
// prototipo: si no, el value tracker de React ignora el evento input.
const escribir = async (valor) => {
  // happy-dom no propaga los eventos sintéticos a través del sistema de
  // delegación de React 18, así que invocamos el onChange REAL del input
  // vía sus props de React: se ejercita la lógica del componente completa.
  const propsKey = Object.keys(input()).find((k) => k.startsWith('__reactProps$'))
  input()[propsKey].onChange({ target: { value: valor, id: 'buscar-residuo' } })
  await sleep(150)
}

// 1. Secciones principales renderizadas
await esperar(() => texto().includes('La regla de los tres colores'), 'sección tres colores')
;['¿Dónde va?', 'Residuos especiales', '¿Cómo preparar los residuos?',
  '¿Cómo hacerlo en Senderos?', 'Horarios', 'Normativa'].forEach((t) => {
  if (!texto().includes(t)) throw new Error('falta la sección: ' + t)
})
const imgs = [...document.querySelectorAll('img')]
if (imgs.length < 9) throw new Error('se esperaban al menos 9 imágenes, hay ' + imgs.length)
console.log('OK: página completa con', imgs.length, 'imágenes y todas las secciones')

// 2. Búsqueda parcial "botella" → encuentra Botella plástica
await escribir('botella')
await esperar(() => texto().includes('Botella plástica'), 'resultado de botella')
console.log('OK: búsqueda "botella" encuentra "Botella plástica"')

// 3. Búsqueda parcial "pañ" → encuentra Pañal
await escribir('pañ')
await esperar(() => texto().includes('Pañal desechable'), 'resultado de pañal')
console.log('OK: búsqueda parcial "pañ" encuentra "Pañal desechable"')

// 4. Especiales: "pila" muestra aviso de manejo especial
await escribir('pila')
await esperar(() => texto().includes('NO lo mezcles'), 'aviso de especial')
console.log('OK: "pila" se marca como manejo especial')

// 5. Sin resultados → mensaje amigable, sin crash
await escribir('xyz123nada')
await esperar(() => texto().includes('No encontramos ese residuo'), 'mensaje sin resultados')
console.log('OK: búsqueda sin resultados muestra mensaje amigable')

// 6. Volver a vaciar: vuelve al estado inicial sin crash
await escribir('')
await esperar(() => texto().includes('Escribe el nombre de un residuo'), 'estado inicial vacío')
console.log('OK: volver a vaciar la búsqueda no rompe nada')

console.log('SMOKE TEST UI INFORMACIÓN: TODO CORRECTO')
process.exit(0)
