import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  CheckCircle2, XCircle, Trophy, GripVertical, Lock, ArrowRight, PartyPopper,
  Dumbbell, RotateCcw, Lightbulb
} from 'lucide-react'
import { fetchLecciones, fetchEjercicios, responderEjercicio } from '../api/client.js'
import { Aviso, BotonCargando } from '../components/ui.jsx'
import PantallaCarga from '../components/PantallaCarga.jsx'

const COLOR_DESTINO = {
  blanco: 'bg-white border-stone-300 text-stone-800',
  negro: 'bg-stone-800 border-stone-800 text-white',
  verde: 'bg-leaf-600 border-leaf-600 text-white',
  rojo: 'bg-red-600 border-red-600 text-white',
  morado: 'bg-purple-700 border-purple-700 text-white'
}

const PUNTO_ITEM = {
  blanco: 'bg-stone-200 border border-stone-400',
  negro: 'bg-stone-800',
  verde: 'bg-leaf-600',
  rojo: 'bg-red-500',
  morado: 'bg-purple-600'
}

export function ListaEjercicios() {
  const [lecciones, setLecciones] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let activo = true
    ;(async () => {
      try {
        const data = await fetchLecciones()
        if (activo) setLecciones(data)
      } catch (err) {
        if (activo) setError(err.handledMessage || 'No pudimos cargar las lecciones.')
      }
    })()
    return () => { activo = false }
  }, [])

  if (error) {
    return <section className="max-w-3xl mx-auto"><Aviso tipo="error">{error}</Aviso></section>
  }
  if (!lecciones) return <PantallaCarga />

  return (
    <div className="space-y-6">
      <header>
        <h1 className="font-display text-3xl font-semibold text-leaf-900">Práctica</h1>
        <p className="mt-2 max-w-2xl text-sm leading-relaxed text-stone-600">
          Las actividades de cada lección del recorrido. Supéralas todas para completar el módulo.
        </p>
      </header>
      <ul className="grid gap-4 sm:grid-cols-2">
        {lecciones.map((leccion) => {
          const bloqueada = leccion.estado === 'BLOQUEADA'
          const completada = leccion.estado === 'COMPLETADA'
          const contenido = (
            <span className={`flex h-full items-center justify-between gap-4 rounded-3xl border p-5 transition-all focus:outline-none focus-visible:ring-2 focus-visible:ring-leaf-600 ${
              bloqueada
                ? 'cursor-not-allowed border-stone-200 bg-white opacity-70'
                : leccion.estado === 'ACTUAL'
                  ? 'border-leaf-500 bg-white shadow-card hover:shadow-md'
                  : 'border-stone-200 bg-white shadow-sm hover:border-leaf-400 hover:shadow-card'
            }`}>
              <span className="min-w-0">
                <span className="flex flex-wrap items-center gap-2">
                  <span className="text-xs font-semibold uppercase tracking-wide text-stone-400">
                    Lección {leccion.orden}
                  </span>
                  {completada && (
                    <span className="inline-flex items-center gap-1 rounded-full bg-leaf-100 px-2 py-0.5 text-xs font-semibold text-leaf-700">
                      <CheckCircle2 size={12} aria-hidden="true" /> Completada
                    </span>
                  )}
                  {bloqueada && (
                    <span className="inline-flex items-center gap-1 rounded-full bg-stone-100 px-2 py-0.5 text-xs font-semibold text-stone-500">
                      <Lock size={12} aria-hidden="true" /> Bloqueada
                    </span>
                  )}
                </span>
                <span className={`mt-1 block font-semibold ${bloqueada ? 'text-stone-500' : 'text-stone-800'}`}>
                  {leccion.titulo}
                </span>
              </span>
              <span className={`shrink-0 text-sm font-medium ${bloqueada ? 'text-stone-400' : 'text-leaf-700'}`}>
                {bloqueada ? <Lock size={16} aria-hidden="true" /> : completada ? 'Repasar →' : 'Practicar →'}
              </span>
            </span>
          )

          return (
            <li key={leccion.id}>
              {bloqueada ? (
                <span className="block cursor-not-allowed" title="Completa la lección anterior">{contenido}</span>
              ) : (
                <Link to={`/ejercicios/${leccion.id}`} className="block h-full">{contenido}</Link>
              )}
            </li>
          )
        })}
      </ul>
    </div>
  )
}

export function DetalleEjercicios() {
  const { id } = useParams()
  const [ejercicios, setEjercicios] = useState(null)
  const [leccionInfo, setLeccionInfo] = useState(null)
  const [siguienteLeccion, setSiguienteLeccion] = useState(null)
  const [leccionCompletada, setLeccionCompletada] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let activo = true
    setEjercicios(null)
    setError('')
    ;(async () => {
      try {
        const [datos, lecciones] = await Promise.all([fetchEjercicios(id), fetchLecciones()])
        if (!activo) return
        setEjercicios(datos)
        const indice = lecciones.findIndex((l) => String(l.id) === String(id))
        const info = indice >= 0 ? lecciones[indice] : null
        setLeccionInfo(info)
        setLeccionCompletada(Boolean(info?.completadoEn))
        if (info && indice >= 0 && indice + 1 < lecciones.length) {
          setSiguienteLeccion(lecciones[indice + 1])
        }
      } catch (err) {
        if (activo) setError(err.handledMessage || 'No pudimos cargar la actividad.')
      }
    })()
    return () => { activo = false }
  }, [id])

  if (error) {
    return (
      <section className="max-w-3xl mx-auto space-y-4">
        <Aviso tipo="error">{error}</Aviso>
        <Link to="/aprendizaje" className="text-sm font-medium text-leaf-700 hover:underline">← Volver a lecciones</Link>
      </section>
    )
  }
  if (!ejercicios) return <PantallaCarga />

  const resueltos = ejercicios.filter((e) => e.resuelto).length
  const todoResuelto = ejercicios.length > 0 && resueltos === ejercicios.length
  const mostrarCelebracion = leccionCompletada || todoResuelto
  const variosPasos = ejercicios.length > 1

  const marcarResuelto = (eid, resultado) => {
    setEjercicios((prev) =>
      prev.map((e) => (e.id === eid ? { ...e, resuelto: true, intentos: e.intentos + 1 } : e)))
    if (resultado?.leccionCompletada) setLeccionCompletada(true)
  }

  return (
    <div className="max-w-2xl mx-auto space-y-5">
      <header>
        <Link to="/aprendizaje" className="text-sm font-medium text-leaf-700 hover:underline">← Volver a lecciones</Link>
        <h1 className="mt-2 flex items-center gap-2 font-display text-2xl font-semibold text-leaf-900 sm:text-3xl">
          <Dumbbell className="text-leaf-600" aria-hidden="true" /> Ahora practiquemos
        </h1>
        {leccionInfo && (
          <p className="mt-1 text-sm text-stone-500">
            Actividad de la Lección {leccionInfo.orden}: {leccionInfo.titulo}
          </p>
        )}
      </header>

      {mostrarCelebracion && (
        <section className="animate-fade-up rounded-3xl border border-leaf-200 bg-leaf-50 p-5" role="status">
          <p className="flex items-center gap-2 font-display text-lg font-semibold text-leaf-900">
            <PartyPopper size={20} aria-hidden="true" /> ¡Lección completada!
          </p>
          <p className="mt-1 text-sm text-stone-600">
            Resolviste correctamente la actividad.
            {siguienteLeccion ? ' Ya puedes continuar con la siguiente lección.' : ' Completaste todo el recorrido.'}
          </p>
          {siguienteLeccion ? (
            <Link
              to={`/aprendizaje/${siguienteLeccion.id}`}
              className="mt-4 inline-flex items-center gap-2 rounded-xl bg-leaf-600 px-4 py-2.5 text-sm font-semibold text-white transition-all hover:-translate-y-px hover:bg-leaf-700"
            >
              Continuar con la Lección {siguienteLeccion.orden}: {siguienteLeccion.titulo}
              <ArrowRight size={15} aria-hidden="true" />
            </Link>
          ) : (
            <Link
              to="/progreso"
              className="mt-4 inline-flex items-center gap-2 rounded-xl bg-leaf-600 px-4 py-2.5 text-sm font-semibold text-white transition-all hover:-translate-y-px hover:bg-leaf-700"
            >
              Ver tu progreso <ArrowRight size={15} aria-hidden="true" />
            </Link>
          )}
        </section>
      )}

      {ejercicios.length === 0 && (
        <Aviso tipo="info">Esta lección aún no tiene actividad disponible.</Aviso>
      )}
      <ol className="space-y-5">
        {ejercicios.map((ejercicio, indice) => (
          <li key={ejercicio.id}>
            {ejercicio.tipo === 'CLASIFICAR' ? (
              <ActividadClasificar
                ejercicio={ejercicio}
                paso={variosPasos ? indice + 1 : null}
                onResuelto={marcarResuelto}
              />
            ) : (
              <TarjetaInteraccion
                ejercicio={ejercicio}
                paso={variosPasos ? indice + 1 : null}
                onResuelto={marcarResuelto}
              />
            )}
          </li>
        ))}
      </ol>

      {!mostrarCelebracion && (
        <p className="flex items-center justify-center gap-1.5 text-center text-xs text-stone-400">
          <Trophy size={13} aria-hidden="true" />
          Supera la actividad para completar la lección y desbloquear la siguiente.
        </p>
      )}
    </div>
  )
}

/** Feedback inmediato tras responder: nunca revela la respuesta correcta. */
function Retroalimentacion({ resultado, children }) {
  return (
    <div
      role="status"
      aria-live="polite"
      className={`animate-fade-up rounded-2xl p-4 ${resultado.correcto ? 'bg-leaf-50 border border-leaf-200' : 'bg-amber-50 border border-amber-200'}`}
    >
      <p className={`flex items-center gap-2 font-semibold ${resultado.correcto ? 'text-leaf-800' : 'text-amber-800'}`}>
        {resultado.correcto
          ? <CheckCircle2 size={18} aria-hidden="true" />
          : <XCircle size={18} aria-hidden="true" />}
        {resultado.correcto
          ? <>¡Correcto!{resultado.primeraCorrecta ? ` +${resultado.puntosObtenidos} puntos` : ''}</>
          : 'Casi. Revisa los elementos marcados y corrígelos.'}
      </p>
      {resultado.explicacion && (
        <p className={`mt-1.5 text-sm leading-relaxed ${resultado.correcto ? 'text-leaf-900' : 'text-amber-900'}`}>
          {resultado.explicacion}
        </p>
      )}
      {children}
    </div>
  )
}

/**
 * Actividad CLASIFICAR: seleccionar un elemento y asignarle un destino.
 * Tap/tap en movil; la solucion vive en el servidor y el feedback marca
 * elemento por elemento sin revelar el destino correcto.
 */
function ActividadClasificar({ ejercicio, paso, onResuelto }) {
  const config = ejercicio.config || {}
  const destinos = Array.isArray(config.destinos) ? config.destinos : []
  const items = Array.isArray(config.items) ? config.items : []
  const modo = config.modo || 'CANECAS'
  const esSeleccion = modo === 'SELECCION'

  const [asignaciones, setAsignaciones] = useState({})
  const [seleccionado, setSeleccionado] = useState(null)
  const [resultado, setResultado] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState('')

  const resueltoAntes = ejercicio.resuelto && !resultado
  const detallePorItem = {}
  resultado?.detalleItems?.forEach((d) => { detallePorItem[d.itemId] = d.correcto })
  const hayDetalle = resultado?.detalleItems?.length > 0
  const bloqueado = resultado?.correcto

  // Modo SELECCION: marcar = destino[0]; el resto de elementos se envian
  // implicitamente como no-problema (destino complementario).
  const destinoMarca = destinos[0]?.id
  const destinoNoMarca = destinos.find((d) => d.id !== destinoMarca)?.id

  // Resumen pedagogico del intento fallido en SELECCION, sin revelar
  // cuales elementos son la respuesta.
  let marcadoFalso = 0
  let problemaSinIdentificar = 0
  if (resultado && esSeleccion && !resultado.correcto) {
    resultado.detalleItems?.forEach((d) => {
      if (!d.correcto) {
        if (asignaciones[d.itemId] === destinoMarca) marcadoFalso++
        else problemaSinIdentificar++
      }
    })
  }

  const itemsAsignados = items.filter((i) => asignaciones[i.id]).length
  const itemsEnPool = items.filter((i) => !asignaciones[i.id])

  const alternarSeleccion = (itemId) => {
    if (bloqueado) return
    setResultado(null)
    if (esSeleccion) {
      setAsignaciones((a) => {
        const nuevo = { ...a }
        if (nuevo[itemId] === destinoMarca) delete nuevo[itemId]
        else nuevo[itemId] = destinoMarca
        return nuevo
      })
    } else {
      setSeleccionado((sel) => (sel === itemId ? null : itemId))
    }
  }

  const asignar = (destinoId) => {
    if (bloqueado || esSeleccion) return
    if (!seleccionado) return
    setAsignaciones((a) => ({ ...a, [seleccionado]: destinoId }))
    setSeleccionado(null)
    setResultado(null)
  }

  const devolver = (itemId) => {
    if (bloqueado || esSeleccion) return
    setAsignaciones((a) => {
      const nuevo = { ...a }
      delete nuevo[itemId]
      return nuevo
    })
    setSeleccionado(null)
    setResultado(null)
  }

  async function manejarEnvio(evento) {
    evento.preventDefault()
    setEnviando(true)
    setError('')
    try {
      // En SELECCION se envia la seleccion COMPLETA: los marcados como
      // problema y el resto como no-problema, para que el backend evalúe
      // toda la seleccion contra la solucion.
      let respuestaFinal = asignaciones
      if (esSeleccion) {
        const completa = {}
        items.forEach((item) => {
          completa[item.id] = asignaciones[item.id] === destinoMarca ? destinoMarca : destinoNoMarca
        })
        respuestaFinal = completa
      }
      const data = await responderEjercicio(ejercicio.id, JSON.stringify(respuestaFinal))
      setResultado(data)
      if (data.correcto && !ejercicio.resuelto && onResuelto) onResuelto(ejercicio.id, data)
    } catch (err) {
      setError(err.handledMessage || 'No pudimos registrar tu respuesta.')
    } finally {
      setEnviando(false)
    }
  }

  const estadoChip = (itemId) => {
    if (bloqueado) return 'border-leaf-500 bg-leaf-50'
    const fallo = hayDetalle && detallePorItem[itemId] === false
    // En SELECCION solo se marca en rojo lo seleccionado de mas: los
    // problemas sin identificar no se revelan.
    if (fallo && (!esSeleccion || asignaciones[itemId] === destinoMarca)) return 'border-red-400 bg-red-50'
    if (hayDetalle && detallePorItem[itemId] === true) return 'border-leaf-500 bg-leaf-50'
    if (seleccionado === itemId) return 'border-leaf-600 bg-leaf-100 ring-2 ring-leaf-500/40'
    return 'border-stone-200 bg-white hover:border-leaf-300'
  }

  return (
    <article className="space-y-4 rounded-3xl border border-stone-200 bg-white p-5 shadow-sm sm:p-6">
      <div className="flex items-start justify-between gap-3">
        <h2 className="font-semibold text-stone-800">
          {paso && (
            <span className="mr-2 inline-grid h-6 w-6 place-items-center rounded-lg bg-leaf-100 text-xs font-bold text-leaf-700">
              {paso}
            </span>
          )}
          {ejercicio.enunciado}
        </h2>
        {ejercicio.resuelto && (
          <span className="flex shrink-0 items-center gap-1 text-sm font-medium text-leaf-700">
            <CheckCircle2 size={16} aria-hidden="true" /> Superada
          </span>
        )}
      </div>

      {config.tablero && (
        <figure className="overflow-hidden rounded-2xl border border-leaf-100">
          <img
            src={config.tablero}
            alt={`Escenario de la actividad: ${ejercicio.enunciado}`}
            loading="lazy"
            width={1200}
            height={800}
            className="h-auto w-full object-cover"
          />
        </figure>
      )}

      <form onSubmit={manejarEnvio} className="space-y-4">
        {esSeleccion ? (
          <>
            <p className="text-xs font-medium text-stone-500">
              Toca cada tarjeta para marcarla como problema. Desmarca si te equivocaste.
            </p>
            <ul className="grid gap-2 sm:grid-cols-2">
              {items.map((item) => {
                const marcado = asignaciones[item.id] === destinoMarca
                return (
                  <li key={item.id}>
                    <button
                      type="button"
                      onClick={() => alternarSeleccion(item.id)}
                      aria-pressed={marcado}
                      className={`flex w-full items-center gap-2.5 rounded-xl border p-3 text-left text-sm transition-all ${estadoChip(item.id)} ${
                        marcado ? 'font-medium text-leaf-900' : 'text-stone-700'
                      }`}
                    >
                      <span className={`grid h-5 w-5 shrink-0 place-items-center rounded-md border ${
                        marcado ? 'border-leaf-600 bg-leaf-600 text-white' : 'border-stone-300 bg-white'
                      }`} aria-hidden="true">
                        {marcado && <CheckCircle2 size={13} />}
                      </span>
                      {item.texto}
                    </button>
                  </li>
                )
              })}
            </ul>
          </>
        ) : (
          <>
            {itemsEnPool.length > 0 && (
              <div>
                <p className="text-xs font-medium text-stone-500">
                  {seleccionado ? 'Ahora toca un destino para ubicarlo.' : 'Toca un elemento para seleccionarlo.'}
                </p>
                <ul className="mt-2 flex flex-wrap gap-2">
                  {itemsEnPool.map((item) => (
                    <li key={item.id}>
                      <button
                        type="button"
                        onClick={() => alternarSeleccion(item.id)}
                        aria-pressed={seleccionado === item.id}
                        className={`flex items-center gap-1.5 rounded-full border px-3.5 py-2 text-sm transition-all active:scale-95 ${estadoChip(item.id)}`}
                      >
                        {item.color && PUNTO_ITEM[item.color] && (
                          <span className={`h-2.5 w-2.5 rounded-full ${PUNTO_ITEM[item.color]}`} aria-hidden="true" />
                        )}
                        {item.texto}
                      </button>
                    </li>
                  ))}
                </ul>
              </div>
            )}

            <fieldset className="space-y-2.5">
              <legend className="sr-only">Destinos de la actividad</legend>
              {destinos.map((destino) => {
                const enDestino = items.filter((i) => asignaciones[i.id] === destino.id)
                return (
                  <div key={destino.id} className={`rounded-2xl border-2 transition-colors ${COLOR_DESTINO[destino.color] || 'border-stone-200 bg-stone-50 text-stone-800'}`}>
                    <button
                      type="button"
                      onClick={() => asignar(destino.id)}
                      disabled={esSeleccion}
                      className="flex w-full items-center justify-between gap-3 p-3.5 text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-leaf-600"
                    >
                      <span className="font-semibold">{destino.etiqueta}</span>
                      <span className={`rounded-full px-2 py-0.5 text-xs font-bold ${enDestino.length > 0 ? 'bg-black/10' : 'bg-black/5 text-stone-500'}`}>
                        {enDestino.length}
                      </span>
                    </button>
                    {enDestino.length > 0 && (
                      <ul className="flex flex-wrap gap-1.5 px-3.5 pb-3.5">
                        {enDestino.map((item) => (
                          <li key={item.id}>
                            <button
                              type="button"
                              onClick={() => devolver(item.id)}
                              className={`flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs transition-all active:scale-95 ${estadoChip(item.id)}`}
                              title="Toca para devolverlo"
                            >
                              <RotateCcw size={11} aria-hidden="true" />
                              {item.texto}
                            </button>
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                )
              })}
            </fieldset>
          </>
        )}

        <div className="flex flex-wrap items-center gap-3">
          <BotonCargando
            cargando={enviando}
            type="submit"
            disabled={esSeleccion ? false : itemsAsignados < items.length || items.length === 0}
          >
            Comprobar
          </BotonCargando>
          {resultado && !resultado.correcto && !esSeleccion && (
            <span className="text-xs text-stone-500">Corrige los elementos marcados en rojo y vuelve a comprobar.</span>
          )}
        </div>
        {error && <Aviso tipo="error">{error}</Aviso>}
      </form>

      {resultado && (
        <Retroalimentacion resultado={resultado}>
          {!resultado.correcto && esSeleccion && (marcadoFalso > 0 || problemaSinIdentificar > 0) && (
            <div className="mt-2 space-y-1.5 text-sm text-amber-900">
              {marcadoFalso > 0 && (
                <p>
                  Marcaste {marcadoFalso === 1 ? 'una situación que no' : `${marcadoFalso} situaciones que no`}{' '}
                  representa{marcadoFalso === 1 ? '' : 'n'} un problema: el punto cuenta con una condición adecuada.
                  Desmárcala{marcadoFalso === 1 ? '' : 's'} y vuelve a comprobar.
                </p>
              )}
              {problemaSinIdentificar > 0 && (
                <p>
                  Hay {problemaSinIdentificar === 1 ? 'un problema que todavía no' : `${problemaSinIdentificar} problemas que todavía no`}{' '}
                  has identificado. Observa bien la foto del punto de residuos.
                </p>
              )}
            </div>
          )}
          {resultado.correcto && config.refuerzo && (
            <figure className="mt-3 overflow-hidden rounded-2xl border border-leaf-200">
              <img
                src={config.refuerzo.imagen}
                alt="Refuerzo visual de la actividad"
                loading="lazy"
                width={1200}
                height={800}
                className="h-auto w-full object-cover"
              />
              <figcaption className="flex items-center gap-2 bg-leaf-600 px-4 py-2.5 text-sm font-medium text-white">
                <Lightbulb size={15} aria-hidden="true" /> {config.refuerzo.texto}
              </figcaption>
            </figure>
          )}
          {resultado.correcto && !config.refuerzo && (
            <p className="mt-1.5 text-sm text-leaf-900">¡Bien hecho! Actividad superada.</p>
          )}
        </Retroalimentacion>
      )}

      {resueltoAntes && !resultado && (
        <Aviso tipo="info">
          Ya superaste esta actividad. Puedes repetirla para practicar: tu lección sigue completada.
        </Aviso>
      )}
    </article>
  )
}

/** Mezcla estable por ejercicio: evita mostrar los pasos ya en el orden correcto. */
function mezclarOpciones(opciones, ejercicioId) {
  if (opciones.length < 2) return [...opciones]
  let mezcla = [...opciones]
  let semilla = ejercicioId * 2654435761 % 4294967296
  for (let i = mezcla.length - 1; i > 0; i--) {
    semilla = (semilla * 1103515245 + 12345) % 2147483648
    const j = semilla % (i + 1)
    ;[mezcla[i], mezcla[j]] = [mezcla[j], mezcla[i]]
  }
  if (mezcla.join('|') === opciones.join('|')) {
    ;[mezcla[0], mezcla[1]] = [mezcla[1], mezcla[0]]
  }
  return mezcla
}

/** Ejercicio de ordenar pasos: se reordenan subiendo/bajando cada opcion. */
export function OpcionesOrdenar({ opciones, ejercicioId, onCambio }) {
  const [pasos, setPasos] = useState(() => mezclarOpciones(opciones, ejercicioId))

  useEffect(() => {
    onCambio(pasos.join('|'))
  }, [pasos, onCambio])

  const mover = (indice, delta) => {
    const destino = indice + delta
    if (destino < 0 || destino >= pasos.length) return
    const nuevos = [...pasos]
    const [paso] = nuevos.splice(indice, 1)
    nuevos.splice(destino, 0, paso)
    setPasos(nuevos)
  }

  return (
    <ol className="space-y-2">
      {pasos.map((paso, indice) => (
        <li key={paso} className="flex items-center gap-2 rounded-xl border border-stone-200 bg-white p-3">
          <GripVertical size={16} className="shrink-0 text-stone-400" aria-hidden="true" />
          <span className="text-xs font-semibold text-stone-400">{indice + 1}.</span>
          <span className="flex-1 text-sm text-stone-700">{paso}</span>
          <span className="flex shrink-0 flex-col gap-1.5">
            <button
              type="button"
              onClick={() => mover(indice, -1)}
              disabled={indice === 0}
              aria-label={`Subir el paso: ${paso}`}
              className="grid h-9 w-10 place-items-center rounded-lg border border-stone-200 text-base font-semibold text-stone-600 hover:bg-stone-50 disabled:opacity-40 focus:outline-none focus-visible:ring-2 focus-visible:ring-leaf-600"
            >
              ↑
            </button>
            <button
              type="button"
              onClick={() => mover(indice, 1)}
              disabled={indice === pasos.length - 1}
              aria-label={`Bajar el paso: ${paso}`}
              className="grid h-9 w-10 place-items-center rounded-lg border border-stone-200 text-base font-semibold text-stone-600 hover:bg-stone-50 disabled:opacity-40 focus:outline-none focus-visible:ring-2 focus-visible:ring-leaf-600"
            >
              ↓
            </button>
          </span>
        </li>
      ))}
    </ol>
  )
}

/** Interacciones que no son CLASIFICAR (ordenar pasos, opciones, texto). */
function TarjetaInteraccion({ ejercicio, paso, onResuelto }) {
  const [respuesta, setRespuesta] = useState('')
  const [resultado, setResultado] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState('')

  const esOrdenar = ejercicio.tipo === 'ORDENAR'
  const conOpciones = Array.isArray(ejercicio.opciones) && ejercicio.opciones.length > 0

  async function manejarEnvio(evento) {
    evento.preventDefault()
    setEnviando(true)
    setError('')
    try {
      const data = await responderEjercicio(ejercicio.id, respuesta)
      setResultado(data)
      if (data.correcto && !ejercicio.resuelto && onResuelto) onResuelto(ejercicio.id, data)
    } catch (err) {
      setError(err.handledMessage || 'No pudimos registrar tu respuesta.')
    } finally {
      setEnviando(false)
    }
  }

  const puedeEnviar = respuesta.trim().length > 0

  return (
    <article className="space-y-4 rounded-3xl border border-stone-200 bg-white p-5 shadow-sm sm:p-6">
      <div className="flex items-start justify-between gap-4">
        <h2 className="font-semibold text-stone-800">
          {paso && (
            <span className="mr-2 inline-grid h-6 w-6 place-items-center rounded-lg bg-leaf-100 text-xs font-bold text-leaf-700">
              {paso}
            </span>
          )}
          {ejercicio.enunciado}
        </h2>
        {ejercicio.resuelto && (
          <span className="flex shrink-0 items-center gap-1 text-sm font-medium text-leaf-700">
            <CheckCircle2 size={16} aria-hidden="true" /> Superado
          </span>
        )}
      </div>

      <form onSubmit={manejarEnvio} className="space-y-4">
        {esOrdenar ? (
          <OpcionesOrdenar
            opciones={ejercicio.opciones}
            ejercicioId={ejercicio.id}
            onCambio={setRespuesta}
          />
        ) : conOpciones ? (
          <fieldset className="space-y-2">
            <legend className="sr-only">Opciones de respuesta</legend>
            {ejercicio.opciones.map((opcion) => (
              <label
                key={opcion}
                className={`flex cursor-pointer items-start gap-3 rounded-xl border p-3 text-sm transition-colors ${
                  respuesta === opcion
                    ? 'border-leaf-500 bg-leaf-50 text-leaf-900'
                    : 'border-stone-200 bg-white text-stone-700 hover:border-leaf-300'
                }`}
              >
                <input
                  type="radio"
                  name={`ejercicio-${ejercicio.id}`}
                  value={opcion}
                  checked={respuesta === opcion}
                  onChange={(e) => { setRespuesta(e.target.value); setResultado(null) }}
                  className="mt-0.5 h-4 w-4 accent-leaf-700"
                />
                <span>{opcion}</span>
              </label>
            ))}
          </fieldset>
        ) : (
          <input
            type="text"
            value={respuesta}
            onChange={(e) => setRespuesta(e.target.value)}
            placeholder="Escribe tu respuesta"
            aria-label="Tu respuesta"
            className="w-full rounded-xl border border-stone-300 px-3 py-2 focus:outline-none focus-visible:ring-2 focus-visible:ring-leaf-600"
          />
        )}

        <div className="flex flex-wrap items-center gap-3">
          <BotonCargando cargando={enviando} type="submit" disabled={!puedeEnviar}>
            Comprobar
          </BotonCargando>
          {resultado && !resultado.correcto && (
            <span className="text-xs text-stone-500">Puedes intentarlo de nuevo.</span>
          )}
        </div>
        {error && <Aviso tipo="error">{error}</Aviso>}
      </form>

      {resultado && <Retroalimentacion resultado={resultado} />}
    </article>
  )
}
