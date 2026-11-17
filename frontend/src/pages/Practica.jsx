import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  Recycle, ShowerHead, Home, ArrowRight, ArrowLeft, CheckCircle2, XCircle,
  Flame, Trophy, RotateCcw, GripVertical, Dumbbell
} from 'lucide-react'
import { fetchPracticaItem, responderPractica, fetchPracticaResumen } from '../api/client.js'
import { Aviso, BotonCargando } from '../components/ui.jsx'
import PantallaCarga from '../components/PantallaCarga.jsx'
import { OpcionesOrdenar } from './Ejercicios.jsx'

const MODOS = {
  CLASIFICA: {
    titulo: 'Clasifica',
    descripcion: 'Aprende a separar mejor',
    detalle: 'Toca el residuo y luego su caneca: blanco, negro o verde.',
    Icono: Recycle
  },
  PREPARA: {
    titulo: 'Prepara y decide',
    descripcion: 'Practica antes de desechar',
    detalle: '¿Qué hacer antes de llevar un residuo al punto? Decídelo tú.',
    Icono: ShowerHead
  },
  SITUACION: {
    titulo: 'Situación real',
    descripcion: 'Resuelve situaciones del condominio',
    detalle: 'Escenarios cotidianos de tu vivienda y del punto de residuos.',
    Icono: Home
  }
}

const COLOR_DESTINO = {
  blanco: 'bg-white border-stone-300 text-stone-800 hover:border-leaf-500',
  negro: 'bg-stone-800 border-stone-800 text-white hover:border-leaf-400',
  verde: 'bg-leaf-600 border-leaf-600 text-white hover:bg-leaf-700'
}

const PUNTO_ITEM = {
  blanco: 'bg-stone-200 border border-stone-400',
  negro: 'bg-stone-800',
  verde: 'bg-leaf-600'
}

export function PracticaInicio() {
  const [resumen, setResumen] = useState(null)

  useEffect(() => {
    let activo = true
    fetchPracticaResumen()
      .then((data) => { if (activo) setResumen(data) })
      .catch(() => {})
    return () => { activo = false }
  }, [])

  return (
    <div className="space-y-6">
      <header>
        <h1 className="font-display text-3xl font-semibold text-leaf-900">Clasificación</h1>
        <p className="mt-2 max-w-2xl text-sm leading-relaxed text-stone-600">
          Practica lo que aprendiste. Aquí no hay notas: es tu zona libre para ensayar
          la separación, la preparación y las decisiones del punto de residuos.
        </p>
      </header>

      {resumen && resumen.total > 0 && (
        <section className="flex flex-wrap items-center gap-x-6 gap-y-2 rounded-3xl border border-leaf-100 bg-leaf-50/70 px-5 py-4 text-sm text-leaf-900">
          <span className="flex items-center gap-1.5 font-semibold">
            <Trophy size={16} className="text-leaf-600" aria-hidden="true" />
            {resumen.correctas} de {resumen.total} respuestas correctas
          </span>
          <span className="flex items-center gap-1.5">
            <Flame size={15} className="text-amber-500" aria-hidden="true" />
            Racha actual: {resumen.racha} · Mejor racha: {resumen.rachaMaxima}
          </span>
        </section>
      )}

      <section aria-label="Modos de práctica" className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {Object.entries(MODOS).map(([modo, info]) => (
          <article
            key={modo}
            className="group flex h-full flex-col gap-3 rounded-3xl border border-stone-200 bg-white p-5 shadow-sm transition-all hover:-translate-y-0.5 hover:border-leaf-500 hover:shadow-card"
          >
            <span className="grid h-12 w-12 place-items-center rounded-2xl bg-leaf-100 text-leaf-700 transition-colors group-hover:bg-leaf-600 group-hover:text-white">
              <info.Icono size={24} aria-hidden="true" />
            </span>
            <h2 className="font-display text-lg font-semibold text-stone-800">{info.titulo}</h2>
            <p className="text-sm font-medium text-leaf-700">{info.descripcion}</p>
            <p className="flex-1 text-sm leading-relaxed text-stone-500">{info.detalle}</p>
            <Link
              to={`/clasificacion/${modo}`}
              className="mt-2 inline-flex items-center justify-center gap-2 rounded-xl bg-leaf-600 px-4 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-leaf-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-leaf-600"
            >
              Practicar <ArrowRight size={15} aria-hidden="true" />
            </Link>
          </article>
        ))}
      </section>

      <p className="text-xs text-stone-400">
        La práctica libre no afecta tu progreso en las lecciones: es tu espacio para ensayar sin presión.
      </p>
    </div>
  )
}

export function PracticaModo() {
  const { modo } = useParams()
  const info = MODOS[modo]

  const [item, setItem] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [respuesta, setRespuesta] = useState(null)   // respuesta en construcción
  const [seleccionado, setSeleccionado] = useState(null)
  const [resultado, setResultado] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const [ronda, setRonda] = useState({ correctas: 0, total: 0 })
  const [puntuacion, setPuntuacion] = useState(null)
  const [historial, setHistorial] = useState([])

  const cargarItem = async (idsExcluidos) => {
    setCargando(true)
    setError('')
    setItem(null)
    setRespuesta(null)
    setSeleccionado(null)
    setResultado(null)
    try {
      const data = await fetchPracticaItem(modo, idsExcluidos)
      setItem(data)
      setHistorial((h) => [...h, data.id])
    } catch (err) {
      setError(err.handledMessage || 'No pudimos cargar la actividad.')
    } finally {
      setCargando(false)
    }
  }

  useEffect(() => {
    if (!MODOS[modo]) return
    let activo = true
    ;(async () => {
      setCargando(true)
      setError('')
      setItem(null)
      setRespuesta(null)
      setResultado(null)
      try {
        const data = await fetchPracticaItem(modo, [])
        if (!activo) return
        setItem(data)
        setHistorial([data.id])
      } catch (err) {
        if (activo) setError(err.handledMessage || 'No pudimos cargar la actividad.')
      } finally {
        if (activo) setCargando(false)
      }
    })()
    return () => { activo = false }
  }, [modo])

  if (!info) {
    return (
      <section className="max-w-2xl mx-auto space-y-4">
        <Aviso tipo="error">Ese modo de práctica no existe.</Aviso>
        <Link to="/clasificacion" className="text-sm font-medium text-leaf-700 hover:underline">← Volver a Clasificación</Link>
      </section>
    )
  }

  const siguiente = () => cargarItem(historial)
  const terminar = () => setItem(null)

  const registrar = async (valor) => {
    setEnviando(true)
    setError('')
    try {
      const data = await responderPractica(item.id, valor)
      setResultado(data)
      setPuntuacion(data.puntuacion)
      setRonda((r) => ({ correctas: r.correctas + (data.correcto ? 1 : 0), total: r.total + 1 }))
    } catch (err) {
      setError(err.handledMessage || 'No pudimos registrar tu respuesta.')
    } finally {
      setEnviando(false)
    }
  }

  const contenido = item?.contenido || {}
  const detallePorItem = {}
  resultado?.detalleItems?.forEach((d) => { detallePorItem[d.itemId] = d.correcto })
  const grupoCompleto =
    item?.tipo === 'GRUPO' &&
    (contenido.items || []).every((i) => respuesta?.[i.id])

  return (
    <div className="max-w-2xl mx-auto space-y-5">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <Link to="/clasificacion" className="inline-flex items-center gap-1 text-sm font-medium text-leaf-700 hover:underline">
            <ArrowLeft size={14} aria-hidden="true" /> Clasificación
          </Link>
          <h1 className="mt-1.5 flex items-center gap-2 font-display text-2xl font-semibold text-leaf-900">
            <info.Icono className="text-leaf-600" aria-hidden="true" /> {info.titulo}
          </h1>
        </div>
        <div className="flex items-center gap-2 text-sm">
          <span className="rounded-full bg-leaf-100 px-3 py-1.5 font-semibold text-leaf-800">
            {ronda.correctas} de {ronda.total} correctas
          </span>
          {ronda.total > 0 && ronda.correctas === ronda.total && ronda.total > 1 && (
            <span className="flex items-center gap-1 rounded-full bg-amber-100 px-3 py-1.5 font-semibold text-amber-700">
              <Flame size={14} aria-hidden="true" /> ¡Racha perfecta!
            </span>
          )}
        </div>
      </header>

      {error && <Aviso tipo="error">{error}</Aviso>}

      {cargando && <PantallaCarga />}

      {!cargando && !item && (
        <section className="rounded-3xl border border-leaf-200 bg-leaf-50 p-6 text-center">
          <Trophy size={28} className="mx-auto text-leaf-600" aria-hidden="true" />
          <p className="mt-2 font-display text-lg font-semibold text-leaf-900">Gran ronda de práctica</p>
          <p className="mt-1 text-sm text-stone-600">
            Llevas {ronda.correctas} de {ronda.total} correctas en esta sesión.
          </p>
          <div className="mt-4 flex flex-wrap justify-center gap-3">
            <button
              onClick={() => setHistorial((h) => { cargarItem(h); return h })}
              className="inline-flex items-center gap-2 rounded-xl bg-leaf-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-leaf-700"
            >
              <RotateCcw size={15} aria-hidden="true" /> Seguir practicando
            </button>
            <Link
              to="/clasificacion"
              className="inline-flex items-center gap-2 rounded-xl border border-leaf-300 bg-white px-4 py-2.5 text-sm font-semibold text-leaf-800 hover:bg-leaf-50"
            >
              Volver a los modos
            </Link>
          </div>
        </section>
      )}

      {!cargando && item && (
        <article className="space-y-4 rounded-3xl border border-stone-200 bg-white p-5 shadow-sm sm:p-6">
          {item.escenario && (
            <p className="rounded-2xl border border-earth-200 bg-earth-50 px-4 py-3 text-sm leading-relaxed text-earth-900">
              {item.escenario}
            </p>
          )}
          <h2 className="font-semibold text-stone-800">{item.enunciado}</h2>

          {/* DESTINO: tocar el residuo ya está hecho (es el enunciado); tocar la caneca responde */}
          {item.tipo === 'DESTINO' && (
            <div className="grid gap-2.5 sm:grid-cols-3">
              {(contenido.destinos || []).map((destino) => (
                <button
                  key={destino.id}
                  type="button"
                  disabled={Boolean(resultado) || enviando}
                  onClick={() => registrar(destino.id)}
                  className={`rounded-2xl border-2 p-4 text-sm font-semibold transition-all active:scale-95 disabled:opacity-60 ${COLOR_DESTINO[destino.color] || 'border-stone-200 bg-stone-50'}`}
                >
                  {destino.etiqueta}
                </button>
              ))}
            </div>
          )}

          {/* ELECCION: elegir una acción y comprobar */}
          {item.tipo === 'ELECCION' && (
            <fieldset className="space-y-2">
              <legend className="sr-only">Opciones</legend>
              {(contenido.opciones || []).map((opcion) => {
                const elegida = respuesta === opcion.id
                const esCorrecta = resultado && elegida && resultado.correcto
                return (
                  <button
                    key={opcion.id}
                    type="button"
                    disabled={Boolean(resultado) || enviando}
                    onClick={() => { setRespuesta(opcion.id); setResultado(null) }}
                    className={`flex w-full items-center gap-3 rounded-xl border p-3.5 text-left text-sm transition-all disabled:cursor-default ${
                      esCorrecta
                        ? 'border-leaf-500 bg-leaf-50 font-medium text-leaf-900'
                        : elegida
                          ? 'border-leaf-500 bg-leaf-50 text-leaf-900'
                          : 'border-stone-200 bg-white text-stone-700 hover:border-leaf-300 disabled:opacity-70'
                    }`}
                  >
                    {esCorrecta && <CheckCircle2 size={17} className="shrink-0 text-leaf-600" aria-hidden="true" />}
                    {opcion.texto}
                  </button>
                )
              })}
              {!resultado && (
                <BotonCargando
                  cargando={enviando}
                  disabled={!respuesta}
                  onClick={() => registrar(respuesta)}
                  className="w-full sm:w-auto"
                >
                  Comprobar
                </BotonCargando>
              )}
            </fieldset>
          )}

          {/* ORDENAR: reordenar pasos y comprobar */}
          {item.tipo === 'ORDENAR' && !resultado && (
            <form onSubmit={(e) => { e.preventDefault(); registrar(respuesta) }} className="space-y-4">
              <OpcionesOrdenar opciones={contenido.pasos || []} ejercicioId={item.id} onCambio={setRespuesta} />
              <BotonCargando cargando={enviando} type="submit" disabled={!respuesta} className="w-full sm:w-auto">
                Comprobar
              </BotonCargando>
            </form>
          )}

          {/* GRUPO: asignar cada elemento a su destino */}
          {item.tipo === 'GRUPO' && (
            <div className="space-y-4">
              {(contenido.items || []).filter((i) => !respuesta?.[i.id]).length > 0 && (
                <div>
                  <p className="text-xs font-medium text-stone-500">Elementos por ubicar:</p>
                  <ul className="mt-2 flex flex-wrap gap-2">
                    {(contenido.items || []).filter((i) => !respuesta?.[i.id]).map((i) => (
                      <li key={i.id}>
                        <button
                          type="button"
                          disabled={Boolean(resultado)}
                          onClick={() => setSeleccionado(seleccionado === i.id ? null : i.id)}
                          className={`flex items-center gap-1.5 rounded-full border px-3.5 py-2 text-sm transition-all active:scale-95 ${
                            seleccionado === i.id && !resultado
                              ? 'border-leaf-600 bg-leaf-100 ring-2 ring-leaf-500/40'
                              : 'border-stone-200 bg-white text-stone-700 hover:border-leaf-300'
                          }`}
                        >
                          {i.color && PUNTO_ITEM[i.color] && (
                            <span className={`h-2.5 w-2.5 rounded-full ${PUNTO_ITEM[i.color]}`} aria-hidden="true" />
                          )}
                          {i.texto}
                        </button>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
              <div className="space-y-2.5">
                {(contenido.destinos || []).map((destino) => {
                  const enDestino = (contenido.items || []).filter((i) => respuesta?.[i.id] === destino.id)
                  return (
                    <div key={destino.id} className={`rounded-2xl border-2 transition-colors ${COLOR_DESTINO[destino.color] || 'border-stone-200 bg-stone-50 text-stone-800'}`}>
                      <button
                        type="button"
                        disabled={Boolean(resultado)}
                        onClick={() => {
                          if (seleccionado) {
                            setRespuesta((r) => ({ ...(r || {}), [seleccionado]: destino.id }))
                            setSeleccionado(null)
                          }
                        }}
                        className="flex w-full items-center justify-between p-3.5 text-left font-semibold focus:outline-none focus-visible:ring-2 focus-visible:ring-leaf-600"
                      >
                        {destino.etiqueta}
                        <span className={`rounded-full px-2 py-0.5 text-xs font-bold ${enDestino.length > 0 ? 'bg-black/10' : 'bg-black/5 text-stone-500'}`}>
                          {enDestino.length}
                        </span>
                      </button>
                      {enDestino.length > 0 && !resultado && (
                        <ul className="flex flex-wrap gap-1.5 px-3.5 pb-3.5">
                          {enDestino.map((i) => (
                            <li key={i.id}>
                              <button
                                type="button"
                                onClick={() => setRespuesta((r) => {
                                  const n = { ...(r || {}) }
                                  delete n[i.id]
                                  return n
                                })}
                                className="rounded-full border border-white/40 bg-black/5 px-3 py-1.5 text-xs hover:bg-black/10"
                                title="Toca para devolverlo"
                              >
                                <RotateCcw size={11} className="mr-1 inline" aria-hidden="true" />
                                {i.texto}
                              </button>
                            </li>
                          ))}
                        </ul>
                      )}
                      {resultado && enDestino.length > 0 && (
                        <ul className="flex flex-wrap gap-1.5 px-3.5 pb-3.5">
                          {enDestino.map((i) => (
                            <li key={i.id} className={`flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs ${
                              detallePorItem[i.id] === false ? 'border-red-400 bg-red-50 text-red-700' : 'border-leaf-500 bg-leaf-50 text-leaf-800'
                            }`}>
                              {detallePorItem[i.id] === false ? <XCircle size={12} aria-hidden="true" /> : <CheckCircle2 size={12} aria-hidden="true" />}
                              {i.texto}
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>
                  )
                })}
              </div>
              {!resultado && (
                <BotonCargando
                  cargando={enviando}
                  disabled={!grupoCompleto}
                  onClick={() => registrar(JSON.stringify(respuesta || {}))}
                  className="w-full sm:w-auto"
                >
                  Comprobar
                </BotonCargando>
              )}
            </div>
          )}

          {/* Feedback educativo */}
          {resultado && (
            <div
              role="status"
              aria-live="polite"
              className={`animate-fade-up rounded-2xl border p-4 ${
                resultado.correcto ? 'border-leaf-200 bg-leaf-50' : 'border-amber-200 bg-amber-50'
              }`}
            >
              <p className={`flex items-center gap-2 font-semibold ${resultado.correcto ? 'text-leaf-800' : 'text-amber-800'}`}>
                {resultado.correcto
                  ? <CheckCircle2 size={18} aria-hidden="true" />
                  : <XCircle size={18} aria-hidden="true" />}
                {resultado.correcto ? '¡Correcto!' : 'No exactamente.'}
              </p>
              {resultado.explicacion && (
                <p className={`mt-1.5 text-sm leading-relaxed ${resultado.correcto ? 'text-leaf-900' : 'text-amber-900'}`}>
                  {resultado.explicacion}
                </p>
              )}
              {puntuacion && (
                <p className="mt-2.5 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs font-medium text-stone-500">
                  <span className="flex items-center gap-1">
                    <Trophy size={13} aria-hidden="true" /> {puntuacion.correctas} de {puntuacion.total} históricas
                  </span>
                  <span className="flex items-center gap-1">
                    <Flame size={13} className={puntuacion.racha > 0 ? 'text-amber-500' : ''} aria-hidden="true" />
                    Racha: {puntuacion.racha} · Mejor: {puntuacion.rachaMaxima}
                  </span>
                </p>
              )}
              <div className="mt-4 flex flex-wrap gap-2.5">
                <button
                  onClick={siguiente}
                  className="inline-flex items-center gap-2 rounded-xl bg-leaf-600 px-4 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-leaf-700"
                >
                  Siguiente <ArrowRight size={15} aria-hidden="true" />
                </button>
                <button
                  onClick={terminar}
                  className="inline-flex items-center gap-2 rounded-xl border border-stone-300 bg-white px-4 py-2.5 text-sm font-semibold text-stone-700 hover:bg-stone-50"
                >
                  <Dumbbell size={14} aria-hidden="true" /> Terminar ronda
                </button>
              </div>
            </div>
          )}
        </article>
      )}

      {!cargando && item && (
        <p className="text-center text-xs text-stone-400">
          <Link to="/aprendizaje" className="inline-flex items-center gap-1 hover:text-leaf-600">
            <ArrowLeft size={12} aria-hidden="true" /> Refresca las lecciones cuando quieras
          </Link>
        </p>
      )}
    </div>
  )
}
