// Prueba de humo UI del módulo de Participación: selección de tipo,
// tema, contador, envío, éxito, error y conservación del contenido.
import { Window } from 'happy-dom'
import React from 'react'
import { createRoot } from 'react-dom/client'
import Comentarios from '../src/pages/Comentarios.jsx'

// Cliente API simulado controlado por la prueba.
let resultadoEnvio = { ok: true }
globalThis.__resultadoEnvio = () => resultadoEnvio
const window = new Window({ url: 'http://localhost:5173/comentarios' })
globalThis.window = window
globalThis.document = window.document
Object.defineProperty(globalThis, 'navigator', { value: window.navigator, configurable: true })
globalThis.HTMLElement = window.HTMLElement
globalThis.Node = window.Node

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
async function esperar(condicion, descripcion, maxMs = 6000) {
  const inicio = Date.now()
  while (Date.now() - inicio < maxMs) {
    if (condicion()) return
    await sleep(50)
  }
  throw new Error('TIEMPO AGOTADO esperando: ' + descripcion + ' | pantalla: ' + (document.body.textContent || '').slice(0, 900))
}

const texto = () => document.body.textContent || ''
const botonPorTexto = (re) => [...document.querySelectorAll('button')].find((b) => re.test((b.textContent || '').trim()))
const textarea = () => document.querySelector('#comentario-contenido')

const escribir = (valor) => {
  // happy-dom no propaga el evento hasta los listeners delegados de React,
  // así que invocamos el onChange REAL del textarea vía sus props: se
  // ejercita la lógica completa del componente (estado, contador, límite).
  const propsKey = Object.keys(textarea()).find((k) => k.startsWith('__reactProps$'))
  if (!propsKey) throw new Error('textarea sin props de React')
  textarea()[propsKey].onChange({ target: { value: valor, id: 'comentario-contenido' } })
}

// Render
const contenedor = document.createElement('div')
document.body.appendChild(contenedor)
const root = createRoot(contenedor)
root.render(<Comentarios />)
await esperar(() => texto().includes('¿Qué quieres compartir?'), 'formulario inicial')
if (!texto().includes('Tu opinión ayuda a mejorar Senderos')) throw new Error('falta el título')
if (!texto().includes('Privacidad')) throw new Error('falta la nota de privacidad')
console.log('OK: formulario con tipos, temas y nota de privacidad')

// 1. Seleccionar tipo "Tengo una sugerencia"
const btnTipo = botonPorTexto(/Tengo una sugerencia/)
btnTipo.click()
await sleep(120)
if (btnTipo.getAttribute('aria-pressed') !== 'true') throw new Error('el tipo no quedó seleccionado')
console.log('OK: tipo seleccionado visualmente (aria-pressed)')

// 2. Seleccionar tema "Punto de residuos"
const btnTema = botonPorTexto(/Punto de residuos/)
btnTema.click()
await sleep(120)
if (btnTema.getAttribute('aria-pressed') !== 'true') throw new Error('el tema no quedó seleccionado')
console.log('OK: tema declarado seleccionado')

// 3. Contador: escribir y ver "n / 500"
escribir('Sugiero mejorar la señalización del punto de residuos.')
const esperado = `${'Sugiero mejorar la señalización del punto de residuos.'.length} / 500`
await esperar(() => texto().includes(esperado), `contador actualizado (${esperado})`)
console.log('OK: contador refleja los caracteres (' + esperado + ')')

// 4. Enviar con éxito (el stub responde OK)
globalThis.__stubFallo = false
const enviarBtn = botonPorTexto(/Enviar comentario/)
enviarBtn.click()
await esperar(() => texto().includes('¡Gracias por participar!'), 'confirmación de envío')
if (!texto().includes('será tenido en cuenta')) throw new Error('falta el mensaje de uso académico')
console.log('OK: envío exitoso con confirmación clara')

// 5. Nuevo comentario con ERROR: el formulario se reinicia (hay que
// volver a elegir el tipo) y el fallo del envío no pierde el contenido.
globalThis.__stubFallo = true
botonPorTexto(/Enviar otro comentario/).click()
await sleep(150)
await esperar(() => texto().includes('¿Qué quieres compartir?'), 'formulario de nuevo')
// Tras el re-monte del formulario, los clicks sintéticos de happy-dom no
// llegan a la delegación de React 18: se invoca el onClick REAL vía props.
const tipoBtn = botonPorTexto(/Tengo una sugerencia/)
const tipoPropsKey = Object.keys(tipoBtn).find((k) => k.startsWith('__reactProps$'))
if (!tipoPropsKey) throw new Error('botón de tipo sin props de React')
tipoBtn[tipoPropsKey].onClick()
await sleep(150)
if (tipoBtn.getAttribute('aria-pressed') !== 'true') throw new Error('el tipo no quedó seleccionado tras el re-monte')
escribir('Este comentario va a fallar.')
// Como un usuario real: esperar el flush del render ANTES de enviar
// (el submit usa la closure del render vigente).
await esperar(() => texto().includes('28 / 500'), 'contenido en el contador')
// Submit vía despacho directo del evento sobre el nodo del formulario:
// burbujea hasta el contenedor de React y dispara el onSubmit real.
const form = textarea().closest('form')
form.dispatchEvent(new window.Event('submit', { bubbles: true, cancelable: true }))
await esperar(() => texto().includes('No pudimos registrar tu comentario'), 'mensaje de error')
if (!textarea().value.includes('va a fallar')) throw new Error('el contenido se perdió tras el error')
console.log('OK: error mostrado sin perder el contenido escrito')

// 6. Límite: el textarea declara maxLength=500 (el navegador lo aplica
// al teclear; el límite duro también se valida en el backend).
if (Number(textarea().getAttribute('maxlength')) !== 500) {
  throw new Error('el textarea debe declarar el límite de 500 caracteres')
}
console.log('OK: límite de 500 caracteres declarado en el textarea')

console.log('SMOKE TEST UI COMENTARIOS: TODO CORRECTO')
process.exit(0)
