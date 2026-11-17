// Reproducción UI del flujo L1 "Detecta los errores" con happy-dom:
// marca tarjetas, pulsa Comprobar con selección INCORRECTA (no debe
// colgarse) y luego con la CORRECTA (debe completar la lección).
import { Window } from 'happy-dom'
import React from 'react'
import { createRoot } from 'react-dom/client'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { DetalleEjercicios } from '../src/pages/Ejercicios.jsx'

const window = new Window({ url: 'http://localhost:5173/' })
globalThis.window = window
globalThis.document = window.document
Object.defineProperty(globalThis, 'navigator', { value: window.navigator, configurable: true })
globalThis.HTMLElement = window.HTMLElement
globalThis.Node = window.Node
globalThis.Event = window.Event
globalThis.CustomEvent = window.CustomEvent
globalThis.MutationObserver = window.MutationObserver

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
async function esperar(condicion, descripcion, maxMs = 8000) {
  const inicio = Date.now()
  while (Date.now() - inicio < maxMs) {
    const valor = condicion()
    if (valor) return valor
    await sleep(60)
  }
  throw new Error('TIEMPO AGOTADO esperando: ' + descripcion)
}

const contenedor = document.createElement('div')
document.body.appendChild(contenedor)
const root = createRoot(contenedor)
root.render(
  <MemoryRouter initialEntries={['/ejercicios/1']}>
    <Routes>
      <Route path="/ejercicios/:id" element={<DetalleEjercicios />} />
    </Routes>
  </MemoryRouter>
)

const texto = () => document.body.textContent || ''
const botonesMarcables = () => [...document.querySelectorAll('button[aria-pressed]')]
const botonPorTexto = (re) => [...document.querySelectorAll('button')].find((b) => re.test(b.textContent || ''))

// 1. La página carga con las 6 tarjetas de L1.
await esperar(() => botonesMarcables().length === 6, 'las 6 tarjetas de L1')
console.log('OK: página montada con 6 tarjetas')

const marcar = async (re) => {
  for (let intento = 0; intento < 3; intento++) {
    const b = botonesMarcables().find((x) => re.test(x.textContent || ''))
    if (!b) throw new Error('tarjeta no encontrada: ' + re)
    if (b.getAttribute('aria-pressed') === 'true') return
    b.click()
    await sleep(80)
    if (b.getAttribute('aria-pressed') === 'true' || botonesMarcables().find((x) => re.test(x.textContent || ''))?.getAttribute('aria-pressed') === 'true') {
      console.log('marcada:', (b.textContent || '').trim().slice(0, 40))
      return
    }
  }
  throw new Error('no se pudo marcar tras 3 intentos: ' + re)
}

// 2. Selección INCORRECTA: solo 2 de los 4 problemas.
await marcar(/fuera de las canecas/)
await marcar(/residuos mezclados/)
const comprobar = botonPorTexto(/^Comprobar$/)
if (!comprobar) throw new Error('botón Comprobar no encontrado')
if (comprobar.disabled) throw new Error('BUG: Comprobar está deshabilitado con selección parcial')
comprobar.click()

// 3. No se cuelga: aparece feedback de error y la UI sigue viva.
await esperar(() => texto().includes('Casi'), 'feedback de respuesta incorrecta')
if (!texto().includes('todavía no has identificado')) {
  throw new Error('falta el aviso pedagógico de problema sin identificar')
}
if (botonesMarcables().length !== 6) throw new Error('la UI perdió las tarjetas tras el fallo')
console.log('OK: respuesta incorrecta -> feedback mostrado, UI viva, sin colgado')

// 4. Corregir y volver a comprobar.
await marcar(/Suciedad/)
await marcar(/terminaron en la caneca negra/)
const estado = botonesMarcables().map((b) => (b.getAttribute('aria-pressed') === 'true' ? '[X]' : '[ ]') + ' ' + (b.textContent || '').trim().slice(0, 40))
console.log('estado tras corrección:', JSON.stringify(estado))
document.querySelector('form').dispatchEvent(new window.Event('submit', { bubbles: true, cancelable: true }))

// 5. Selección exacta: feedback positivo + lección completada.
try {
  await esperar(() => texto().includes('¡Correcto!'), 'feedback de acierto')
} catch (e) {
  console.error('TEXTO EN PANTALLA:', texto().slice(0, 800))
  throw e
}
if (!texto().includes('¡Lección completada!')) throw new Error('no apareció el banner de lección completada')
console.log('OK: selección exacta -> actividad y lección completadas')
console.log('REPRODUCCION UI L1: SIN CUELGUES, FLUJO CORRECTO')
process.exit(0)
