import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { fetchEncuestaInicial, responderEncuesta } from '../api/client.js'
import { BotonCargando, Aviso } from '../components/ui.jsx'
import PantallaCarga from '../components/PantallaCarga.jsx'

const SECCIONES = [
  { tipo: 'CONOCIMIENTO', titulo: 'Parte 1 · Conocimiento ambiental', descripcion: 'Selecciona la respuesta correcta (10 preguntas).' },
  { tipo: 'PRACTICA', titulo: 'Parte 2 · Prácticas de separación en la fuente', descripcion: 'Marca la casilla que mejor describa tu comportamiento real en el hogar durante las últimas tres semanas. No hay respuestas correctas ni incorrectas.' },
  { tipo: 'PERCEPCION', titulo: 'Parte 3 · Percepción y barreras', descripcion: 'Indica tu grado de acuerdo con cada afirmación (escala del 1 al 5).' }
]

/** Verdadero si la opcion elegida de una pregunta OPCION es la abierta ("Otra…"). */
function esOpcionAbierta(pregunta, respuesta) {
  if (!respuesta?.opcionId) return false
  const opcion = pregunta.opciones.find((o) => o.id === respuesta.opcionId)
  return !!opcion && opcion.texto.toLowerCase().startsWith('otra')
}

/** Criterio único de "pregunta respondida" para validar, contar y enviar. */
function estaRespondida(pregunta, r) {
  if (!r) return false
  if (pregunta.tipo === 'ESCALA') return r.opcionId != null
  if (pregunta.tipo === 'ABIERTA' || pregunta.tipo === 'TEXTO') {
    return !!(r.valorTexto && r.valorTexto.trim())
  }
  if (r.opcionId == null) return false
  return !esOpcionAbierta(pregunta, r) || !!(r.valorTexto && r.valorTexto.trim())
}

export default function EncuestaInicial() {
  const navigate = useNavigate()
  const { refreshOnboarding } = useAuth()
  const [encuesta, setEncuesta] = useState(null)
  const [respuestas, setRespuestas] = useState({})
  const [errorCarga, setErrorCarga] = useState('')
  const [errorEnvio, setErrorEnvio] = useState('')
  const [cargandoEncuesta, setCargandoEncuesta] = useState(true)
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    let activo = true
    ;(async () => {
      try {
        const data = await fetchEncuestaInicial()
        if (!activo) return
        setEncuesta(data)
      } catch (err) {
        if (activo) setErrorCarga(err.handledMessage || 'No pudimos cargar la encuesta.')
      } finally {
        if (activo) setCargandoEncuesta(false)
      }
    })()
    return () => { activo = false }
  }, [])

  const seleccionar = (preguntaId, opcionId) => {
    setRespuestas((r) => ({ ...r, [preguntaId]: { ...r[preguntaId], opcionId } }))
  }

  const escribirTexto = (preguntaId, valor) => {
    setRespuestas((r) => ({ ...r, [preguntaId]: { ...r[preguntaId], valorTexto: valor } }))
  }

  const validar = () => {
    if (!encuesta) return false
    return encuesta.preguntas
      .filter((p) => p.requerida)
      .every((p) => estaRespondida(p, respuestas[p.id]))
  }

  const enviar = async (e) => {
    e.preventDefault()
    setErrorEnvio('')
    const faltantes = encuesta.preguntas.filter((p) => p.requerida && !estaRespondida(p, respuestas[p.id]))
    if (faltantes.length > 0) {
      setErrorEnvio(`Falta responder ${faltantes.length} pregunta(s) obligatoria(s).`)
      return
    }
    setEnviando(true)
    try {
      const items = Object.entries(respuestas).map(([preguntaId, r]) => ({
        preguntaId: Number(preguntaId),
        opcionId: r.opcionId ?? null,
        valorTexto: r.valorTexto?.trim() || null
      }))
      await responderEncuesta(encuesta.id, items)
      await refreshOnboarding()
      navigate('/dashboard', { replace: true })
    } catch (err) {
      setErrorEnvio(err.handledMessage || 'No pudimos guardar tus respuestas. Intenta de nuevo.')
    } finally {
      setEnviando(false)
    }
  }

  if (cargandoEncuesta) return <PantallaCarga />

  if (errorCarga) {
    return (
      <section className="max-w-2xl mx-auto">
        <Aviso tipo="error">{errorCarga}</Aviso>
      </section>
    )
  }

  if (!encuesta) return <PantallaCarga />

  const puedeEnviar = validar()
  const total = encuesta.preguntas.length
  const respondidas = encuesta.preguntas.filter((p) => estaRespondida(p, respuestas[p.id])).length
  const grupos = SECCIONES
    .map((s) => ({ ...s, preguntas: encuesta.preguntas.filter((p) => p.tipoPregunta === s.tipo) }))
    .filter((g) => g.preguntas.length > 0)

  return (
    <section className="max-w-2xl mx-auto">
      <div className="bg-white rounded-3xl shadow-card p-6 sm:p-8">
        <p className="text-xs font-semibold uppercase tracking-wide text-leaf-700">Paso 2 de 2</p>
        <h1 className="mt-2 font-display text-2xl font-semibold text-leaf-900">{encuesta.titulo}</h1>
        <p className="mt-1 text-sm text-stone-500">
          Tus respuestas se asocian a tu vivienda y nos permiten conocer el punto de partida de la comunidad.
        </p>

        <div className="mt-4 flex items-center gap-3" aria-live="polite">
          <div className="h-2 flex-1 rounded-full bg-stone-100 overflow-hidden">
            <div
              className="h-full rounded-full bg-leaf-600 transition-all duration-300"
              style={{ width: `${total ? (respondidas / total) * 100 : 0}%` }}
            />
          </div>
          <span className="text-xs font-semibold text-stone-600 tabular-nums">
            {respondidas}/{total}
          </span>
        </div>

        {errorEnvio && <div className="mt-4"><Aviso tipo="error">{errorEnvio}</Aviso></div>}

        <form onSubmit={enviar} className="mt-6 space-y-10" noValidate>
          {grupos.map((grupo) => (
            <div key={grupo.tipo} className="space-y-6">
              <div className="border-b border-stone-100 pb-2">
                <h2 className="font-display text-lg font-semibold text-leaf-900">{grupo.titulo}</h2>
                <p className="text-xs text-stone-500">{grupo.descripcion}</p>
              </div>
              {grupo.preguntas.map((pregunta) => (
            <fieldset key={pregunta.id} className="space-y-3">
              <legend className="text-sm font-semibold text-stone-800">
                {pregunta.texto}
                {pregunta.requerida && <span className="text-red-600" aria-hidden="true"> *</span>}
              </legend>
              {pregunta.tipo === 'ABIERTA' || pregunta.tipo === 'TEXTO' ? (
                <textarea
                  id={`preg-${pregunta.id}`}
                  rows={3}
                  maxLength={500}
                  className="w-full rounded-2xl border border-stone-300 bg-white px-4 py-3 text-sm text-stone-800 placeholder:text-stone-400 focus:outline-none focus:ring-2 focus:ring-leaf-600"
                  placeholder="Escribe tu respuesta…"
                  onChange={(e) => escribirTexto(pregunta.id, e.target.value)}
                  aria-required={pregunta.requerida}
                />
              ) : pregunta.tipo === 'ESCALA' ? (
                <div className="grid grid-cols-5 gap-1.5 sm:gap-2" role="radiogroup" aria-label={pregunta.texto}>
                  {pregunta.opciones.map((op) => {
                    const seleccionada = respuestas[pregunta.id]?.opcionId === op.id
                    return (
                      <label
                        key={op.id}
                        className={`flex flex-col items-center justify-center gap-1 rounded-2xl border px-1.5 py-2.5 text-[11px] font-medium cursor-pointer text-center leading-tight transition-colors ${
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
                        className={`flex items-center gap-3 rounded-2xl border px-4 py-3 text-sm cursor-pointer transition-colors ${
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
                      id={`preg-${pregunta.id}-texto`}
                      rows={2}
                      maxLength={500}
                      className="w-full rounded-2xl border border-stone-300 bg-white px-4 py-3 text-sm text-stone-800 placeholder:text-stone-400 focus:outline-none focus:ring-2 focus:ring-leaf-600"
                      placeholder="Cuéntanos cuál es tu caso…"
                      onChange={(e) => escribirTexto(pregunta.id, e.target.value)}
                      value={respuestas[pregunta.id]?.valorTexto || ''}
                      aria-required="true"
                    />
                  )}
                </div>
              )}
            </fieldset>
              ))}
            </div>
          ))}

          <BotonCargando type="submit" cargando={enviando} disabled={!puedeEnviar || enviando} className="w-full">
            {enviando ? 'Guardando…' : `Enviar respuestas (${respondidas}/${total})`}
          </BotonCargando>
        </form>
      </div>
    </section>
  )
}
