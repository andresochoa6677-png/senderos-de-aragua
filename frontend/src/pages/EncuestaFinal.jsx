import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import {
  fetchEncuestaFinalDisponible,
  fetchEncuestaFinal,
  responderEncuestaFinal
} from '../api/client.js'
import { BotonCargando, Aviso } from '../components/ui.jsx'
import PantallaCarga from '../components/PantallaCarga.jsx'

const PARTES = [
  { desde: 1, hasta: 10, titulo: 'Parte 1 · Conocimiento' },
  { desde: 11, hasta: 14, titulo: 'Parte 2 · Prácticas de separación' },
  { desde: 15, hasta: 18, titulo: 'Parte 3 · Percepción' },
  { desde: 19, hasta: 22, titulo: 'Parte 4 · Sobre la plataforma' }
]

function esOpcionAbierta(pregunta, respuesta) {
  if (!respuesta?.opcionId) return false
  const opcion = pregunta.opciones.find((o) => o.id === respuesta.opcionId)
  return !!opcion && opcion.texto.toLowerCase().startsWith('otra')
}

function estaRespondida(pregunta, r) {
  if (!r) return false
  if (pregunta.tipo === 'ESCALA') return r.opcionId != null
  if (r.opcionId == null) return !!(r.valorTexto && r.valorTexto.trim())
  return !esOpcionAbierta(pregunta, r) || !!(r.valorTexto && r.valorTexto.trim())
}

export default function EncuestaFinal() {
  const navigate = useNavigate()
  const { refreshOnboarding } = useAuth()
  const [estado, setEstado] = useState(null)
  const [encuesta, setEncuesta] = useState(null)
  const [parte, setParte] = useState(0)
  const [respuestas, setRespuestas] = useState({})
  const [puntaje, setPuntaje] = useState(null)
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)
  const cajaRef = useRef(null)

  useEffect(() => {
    let activo = true
    ;(async () => {
      try {
        const s = await fetchEncuestaFinalDisponible()
        if (!activo) return
        setEstado(s)
        if (s.disponible) {
          const data = await fetchEncuestaFinal()
          if (activo) setEncuesta(data)
        }
      } catch (err) {
        if (activo) setError(err.handledMessage || 'No pudimos verificar la encuesta final.')
      }
    })()
    return () => { activo = false }
  }, [])

  const seleccionar = (preguntaId, opcionId) =>
    setRespuestas((r) => ({ ...r, [preguntaId]: { ...r[preguntaId], opcionId } }))

  const escribirTexto = (preguntaId, valor) =>
    setRespuestas((r) => ({ ...r, [preguntaId]: { ...r[preguntaId], valorTexto: valor } }))

  const partesConItems = useMemo(() => {
    if (!encuesta) return []
    return PARTES.map((p) => ({
      ...p,
      preguntas: encuesta.preguntas.filter((q) => q.orden >= p.desde && q.orden <= p.hasta)
    })).filter((p) => p.preguntas.length > 0)
  }, [encuesta])

  const total = encuesta?.preguntas.length ?? 22
  const respondidas = encuesta
    ? encuesta.preguntas.filter((p) => estaRespondida(p, respuestas[p.id])).length
    : 0

  const parteActual = partesConItems[parte]
  const parteCompleta = parteActual
    ? parteActual.preguntas.every((p) => estaRespondida(p, respuestas[p.id]))
    : false

  const siguiente = () => {
    if (!parteCompleta) {
      setError('Completa todas las preguntas de esta parte para continuar.')
      return
    }
    setError('')
    setParte((p) => Math.min(p + 1, partesConItems.length - 1))
  }

  const enviar = async () => {
    setError('')
    const faltantes = encuesta.preguntas.filter((p) => !estaRespondida(p, respuestas[p.id]))
    if (faltantes.length > 0) {
      setError(`Falta responder ${faltantes.length} pregunta(s) obligatoria(s).`)
      setParte(0)
      return
    }
    setEnviando(true)
    try {
      const items = encuesta.preguntas.map((p) => ({
        preguntaId: p.id,
        opcionId: respuestas[p.id]?.opcionId ?? null,
        valorTexto: respuestas[p.id]?.valorTexto?.trim() || null
      }))
      const resultado = await responderEncuestaFinal(items)
      setPuntaje(resultado.puntajeConocimiento)
      window.scrollTo({ top: 0, behavior: 'smooth' })
    } catch (err) {
      setError(err.handledMessage || 'No pudimos guardar tus respuestas. Intenta de nuevo.')
    } finally {
      setEnviando(false)
    }
  }

  if (puntaje !== null) {
    return (
      <section className="mx-auto max-w-xl animate-fade-up text-center">
        <div className="rounded-3xl bg-white p-8 shadow-card">
          <p className="text-4xl" aria-hidden="true">🎉</p>
          <h1 className="mt-3 font-display text-2xl font-semibold text-leaf-900">
            ¡Gracias por completar el estudio!
          </h1>
          <p className="mt-3 text-sm text-stone-600">
            Tu puntaje de conocimiento en la encuesta final fue de{' '}
            <span className="font-semibold text-leaf-700">{puntaje}/10</span>.
          </p>
          <p className="mt-2 text-xs text-stone-400">
            Tus respuestas quedaron registradas de forma confidencial para la investigación.
          </p>
          <button
            type="button"
            onClick={async () => { await refreshOnboarding(); navigate('/dashboard', { replace: true }) }}
            className="mt-6 w-full rounded-2xl bg-leaf-600 px-4 py-3 text-sm font-medium text-white transition-colors hover:bg-leaf-700"
          >
            Ir al inicio
          </button>
        </div>
      </section>
    )
  }

  if (error && !estado) {
    return (
      <section className="mx-auto max-w-2xl">
        <Aviso tipo="error">{error}</Aviso>
      </section>
    )
  }

  if (!estado) return <PantallaCarga />

  if (estado.yaRespondida) {
    return (
      <section className="mx-auto max-w-xl animate-fade-up text-center">
        <div className="rounded-3xl bg-white p-8 shadow-card">
          <p className="text-4xl" aria-hidden="true">✅</p>
          <h1 className="mt-3 font-display text-2xl font-semibold text-leaf-900">
            Ya respondiste la encuesta final. Gracias.
          </h1>
          <button
            type="button"
            onClick={() => navigate('/dashboard', { replace: true })}
            className="mt-6 w-full rounded-2xl bg-leaf-600 px-4 py-3 text-sm font-medium text-white transition-colors hover:bg-leaf-700"
          >
            Volver al inicio
          </button>
        </div>
      </section>
    )
  }

  if (!estado.disponible) {
    return (
      <section className="mx-auto max-w-xl animate-fade-up text-center">
        <div className="rounded-3xl bg-white p-8 shadow-card">
          <p className="text-4xl" aria-hidden="true">🗓️</p>
          <h1 className="mt-3 font-display text-2xl font-semibold text-leaf-900">
            La encuesta final aún no está disponible.
          </h1>
          <p className="mt-3 text-sm text-stone-600">
            Te avisaremos cuando se habilite.
          </p>
          <button
            type="button"
            onClick={() => navigate('/dashboard', { replace: true })}
            className="mt-6 w-full rounded-2xl border border-stone-300 px-4 py-3 text-sm font-medium text-stone-600 hover:border-leaf-400"
          >
            Volver al inicio
          </button>
        </div>
      </section>
    )
  }

  // La encuesta falló al cargar pero el estado llegó: error con reintento
  // en vez de una página muerta (antes: crash por parteActual indefinido).
  if (error && estado.disponible && !encuesta) {
    return (
      <section className="mx-auto max-w-2xl">
        <Aviso tipo="error">{error}</Aviso>
        <button
          type="button"
          onClick={() => window.location.reload()}
          className="mt-4 w-full rounded-2xl border border-stone-300 px-4 py-3 text-sm font-medium text-stone-600 hover:border-leaf-400"
        >
          Reintentar
        </button>
      </section>
    )
  }

  if (!encuesta) return <PantallaCarga />
  if (!parteActual) return <PantallaCarga />

  return (
    <section className="mx-auto max-w-2xl animate-fade-up">
      <div className="rounded-3xl bg-white p-6 shadow-card sm:p-8">
        <p className="text-xs font-semibold uppercase tracking-wide text-leaf-700">
          Encuesta final · {parteActual.titulo}
        </p>
        <h1 className="mt-2 font-display text-2xl font-semibold text-leaf-900">{encuesta.titulo}</h1>
        <p className="mt-1 text-sm text-stone-500">
          Este instrumento es confidencial y sus respuestas serán utilizadas exclusivamente
          con fines académicos, conforme a la Ley 1581 de 2012.
        </p>

        <div className="mt-4 flex items-center gap-3" aria-live="polite">
          <div className="h-2 flex-1 overflow-hidden rounded-full bg-stone-100">
            <div
              className="h-full rounded-full bg-leaf-600 transition-all duration-300"
              style={{ width: `${total ? (respondidas / total) * 100 : 0}%` }}
            />
          </div>
          <span className="text-xs font-semibold tabular-nums text-stone-600">{respondidas}/{total}</span>
        </div>

        {error && <div className="mt-4"><Aviso tipo="error">{error}</Aviso></div>}

        <div className="mt-6 space-y-6" ref={cajaRef}>
          {parteActual.preguntas.map((pregunta) => (
            <fieldset key={pregunta.id} className="space-y-3">
              <legend className="text-sm font-semibold text-stone-800">
                {pregunta.orden}. {pregunta.texto}
                <span className="text-red-600" aria-hidden="true"> *</span>
              </legend>
              {pregunta.tipo === 'ESCALA' ? (
                <div className="grid grid-cols-5 gap-1.5 sm:gap-2" role="radiogroup" aria-label={pregunta.texto}>
                  {pregunta.opciones.map((op) => {
                    const seleccionada = respuestas[pregunta.id]?.opcionId === op.id
                    return (
                      <label
                        key={op.id}
                        className={`flex cursor-pointer flex-col items-center justify-center gap-1 rounded-2xl border px-1.5 py-2.5 text-center text-[11px] font-medium leading-tight transition-colors ${
                          seleccionada
                            ? 'border-leaf-600 bg-leaf-50 text-leaf-900'
                            : 'border-stone-200 bg-white text-stone-600 hover:border-leaf-400'
                        }`}
                      >
                        <input
                          type="radio"
                          name={`preg-${pregunta.id}`}
                          value={op.id}
                          checked={seleccionada}
                          onChange={() => seleccionar(pregunta.id, op.id)}
                          className="h-3.5 w-3.5 accent-leaf-700"
                          aria-label={op.texto}
                        />
                        <span>{op.texto}</span>
                      </label>
                    )
                  })}
                </div>
              ) : (
                <div className="space-y-2" role="radiogroup" aria-label={pregunta.texto}>
                  {pregunta.opciones.map((op) => {
                    const seleccionada = respuestas[pregunta.id]?.opcionId === op.id
                    return (
                      <label
                        key={op.id}
                        className={`flex cursor-pointer items-center gap-3 rounded-2xl border px-4 py-3 text-sm transition-colors ${
                          seleccionada
                            ? 'border-leaf-600 bg-leaf-50 text-leaf-900'
                            : 'border-stone-200 bg-white text-stone-700 hover:border-leaf-400'
                        }`}
                      >
                        <input
                          type="radio"
                          name={`preg-${pregunta.id}`}
                          value={op.id}
                          checked={seleccionada}
                          onChange={() => seleccionar(pregunta.id, op.id)}
                          className="h-4 w-4 accent-leaf-700"
                        />
                        <span>{op.texto}</span>
                      </label>
                    )
                  })}
                  {esOpcionAbierta(pregunta, respuestas[pregunta.id]) && (
                    <textarea
                      rows={2}
                      maxLength={500}
                      className="w-full rounded-2xl border border-stone-300 bg-white px-4 py-3 text-sm text-stone-800 placeholder:text-stone-400 focus:outline-none focus:ring-2 focus:ring-leaf-600"
                      placeholder="Cuéntanos cuál es tu caso…"
                      value={respuestas[pregunta.id]?.valorTexto || ''}
                      onChange={(e) => escribirTexto(pregunta.id, e.target.value)}
                      aria-required="true"
                    />
                  )}
                </div>
              )}
            </fieldset>
          ))}
        </div>

        <div className="mt-8 flex gap-3">
          {parte > 0 && (
            <button
              type="button"
              onClick={() => { setError(''); setParte((p) => p - 1) }}
              className="rounded-2xl border border-stone-300 px-4 py-3 text-sm font-medium text-stone-600 hover:border-leaf-400"
            >
              Anterior
            </button>
          )}
          {parte < partesConItems.length - 1 ? (
            <BotonCargando type="button" onClick={siguiente} cargando={false} className="flex-1">
              Siguiente
            </BotonCargando>
          ) : (
            <BotonCargando
              type="button"
              onClick={enviar}
              cargando={enviando}
              disabled={enviando || respondidas < total}
              className="flex-1"
            >
              {enviando ? 'Guardando…' : `Enviar respuestas (${respondidas}/${total})`}
            </BotonCargando>
          )}
        </div>
      </div>
    </section>
  )
}
