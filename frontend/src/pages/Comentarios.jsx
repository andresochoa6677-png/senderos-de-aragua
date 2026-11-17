import { useEffect, useState } from 'react'
import {
  AlertTriangle, CheckCircle2, Eye, HelpCircle, Lightbulb, Lock,
  MessageSquare, Send
} from 'lucide-react'
import { enviarComentario, fetchMisComentarios } from '../api/client.js'
import { Aviso, BotonCargando } from '../components/ui.jsx'
import PantallaCarga from '../components/PantallaCarga.jsx'

const MAX_CARACTERES = 500

const TIPOS = [
  { id: 'INQUIETUD', titulo: 'Tengo una inquietud', Icono: HelpCircle },
  { id: 'SUGERENCIA', titulo: 'Tengo una sugerencia', Icono: Lightbulb },
  { id: 'PERCEPCION', titulo: 'Quiero compartir mi percepción', Icono: Eye },
  { id: 'SITUACION', titulo: 'Quiero reportar una situación', Icono: AlertTriangle }
]

const TEMAS = [
  { id: 'SEPARACION', titulo: 'Separación de residuos' },
  { id: 'PUNTO_RESIDUOS', titulo: 'Punto de residuos' },
  { id: 'HORARIOS_RECOLECCION', titulo: 'Horarios o recolección' },
  { id: 'INFORMACION_PLATAFORMA', titulo: 'Información de la plataforma' },
  { id: 'SITUACION_CONDOMINIO', titulo: 'Situación del condominio' },
  { id: 'OTRO', titulo: 'Otro' }
]

const ETIQUETA_TIPO = Object.fromEntries(TIPOS.map((t) => [t.id, t.titulo]))

function formatearFecha(iso) {
  return new Date(iso).toLocaleDateString('es', { day: 'numeric', month: 'short', year: 'numeric' })
}

export default function Comentarios() {
  const [tipo, setTipo] = useState(null)
  const [tema, setTema] = useState(null)
  const [contenido, setContenido] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState('')
  const [enviado, setEnviado] = useState(false)
  const [misAportes, setMisAportes] = useState(null)

  useEffect(() => {
    let activo = true
    fetchMisComentarios()
      .then((lista) => { if (activo) setMisAportes(lista) })
      .catch(() => { if (activo) setMisAportes([]) })
    return () => { activo = false }
  }, [])

  const enviar = async (e) => {
    e.preventDefault()
    setError('')
    if (!tipo) { setError('Selecciona qué quieres compartir.'); return }
    const texto = contenido.trim()
    if (!texto) { setError('Escribe tu comentario antes de enviar.'); return }
    setEnviando(true)
    try {
      await enviarComentario({ tipo, temaDeclarado: tema, contenido: texto })
      setEnviado(true)
      setContenido('')
      setTipo(null)
      setTema(null)
      fetchMisComentarios().then(setMisAportes).catch(() => {})
    } catch (err) {
      setError(err.handledMessage || 'No pudimos registrar tu comentario. Intenta de nuevo.')
    } finally {
      setEnviando(false)
    }
  }

  const cercaDelLimite = contenido.length >= MAX_CARACTERES - 50

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <header>
        <h1 className="flex items-center gap-2 font-display text-2xl font-semibold text-leaf-900 sm:text-3xl">
          <MessageSquare size={26} className="text-leaf-600" aria-hidden="true" />
          Tu opinión ayuda a mejorar Senderos
        </h1>
        <p className="mt-2 text-sm leading-relaxed text-stone-600">
          Comparte una inquietud, una sugerencia, una percepción o una situación
          relacionada con el manejo de residuos.
        </p>
      </header>

      {enviado ? (
        <section
          aria-labelledby="gracias-titulo"
          className="animate-fade-up rounded-3xl border border-leaf-200 bg-leaf-50 p-6 text-center"
        >
          <CheckCircle2 size={40} className="mx-auto text-leaf-600" aria-hidden="true" />
          <h2 id="gracias-titulo" className="mt-3 font-display text-xl font-semibold text-leaf-900">
            ¡Gracias por participar!
          </h2>
          <p className="mx-auto mt-2 max-w-md text-sm leading-relaxed text-stone-600">
            Tu comentario fue registrado correctamente y será tenido en cuenta para
            el análisis del proyecto.
          </p>
          <button
            onClick={() => setEnviado(false)}
            className="mt-5 inline-flex items-center gap-2 rounded-xl border border-leaf-300 bg-white px-4 py-2.5 text-sm font-semibold text-leaf-800 transition-colors hover:bg-leaf-50"
          >
            <Send size={15} aria-hidden="true" /> Enviar otro comentario
          </button>
        </section>
      ) : (
        <form onSubmit={enviar} className="space-y-6 rounded-3xl border border-stone-200 bg-white p-5 shadow-sm sm:p-6" noValidate>
          <fieldset>
            <legend className="text-sm font-semibold text-stone-800">¿Qué quieres compartir?</legend>
            <div className="mt-3 grid gap-2.5 sm:grid-cols-2">
              {TIPOS.map(({ id, titulo, Icono }) => {
                const activo = tipo === id
                return (
                  <button
                    key={id}
                    type="button"
                    onClick={() => setTipo(activo ? null : id)}
                    aria-pressed={activo}
                    className={`flex items-center gap-3 rounded-2xl border p-3.5 text-left text-sm transition-all active:scale-[.98] ${
                      activo
                        ? 'border-leaf-600 bg-leaf-50 font-medium text-leaf-900 ring-2 ring-leaf-500/30'
                        : 'border-stone-200 bg-white text-stone-700 hover:border-leaf-300'
                    }`}
                  >
                    <Icono size={18} className={`shrink-0 ${activo ? 'text-leaf-700' : 'text-stone-400'}`} aria-hidden="true" />
                    {titulo}
                  </button>
                )
              })}
            </div>
          </fieldset>

          <div>
            <p className="text-sm font-semibold text-stone-800">¿Sobre qué tema? <span className="font-normal text-stone-400">(opcional)</span></p>
            <div className="mt-2.5 flex flex-wrap gap-2" role="group" aria-label="Tema declarado">
              {TEMAS.map((t) => {
                const activo = tema === t.id
                return (
                  <button
                    key={t.id}
                    type="button"
                    onClick={() => setTema(activo ? null : t.id)}
                    aria-pressed={activo}
                    className={`rounded-full border px-3.5 py-2 text-sm transition-all active:scale-[.98] ${
                      activo
                        ? 'border-leaf-600 bg-leaf-100 font-medium text-leaf-900'
                        : 'border-stone-200 bg-white text-stone-600 hover:border-leaf-300'
                    }`}
                  >
                    {t.titulo}
                  </button>
                )
              })}
            </div>
          </div>

          <div>
            <label htmlFor="comentario-contenido" className="text-sm font-semibold text-stone-800">
              Cuéntanos qué está pasando
            </label>
            <textarea
              id="comentario-contenido"
              rows={5}
              maxLength={MAX_CARACTERES}
              value={contenido}
              onChange={(e) => setContenido(e.target.value)}
              placeholder="Escribe aquí tu inquietud, sugerencia o situación…"
              className="mt-1.5 w-full resize-y rounded-2xl border border-stone-300 bg-white px-4 py-3 text-sm text-stone-800 placeholder:text-stone-400 focus:outline-none focus:ring-2 focus:ring-leaf-600"
              aria-describedby="comentario-contador"
            />
            <p
              id="comentario-contador"
              className={`mt-1 text-right text-xs tabular-nums ${cercaDelLimite ? 'font-semibold text-amber-600' : 'text-stone-400'}`}
            >
              {contenido.length} / {MAX_CARACTERES}
            </p>
          </div>

          {error && <Aviso tipo="error">{error}</Aviso>}

          <div className="rounded-2xl border border-stone-200 bg-stone-50 p-4">
            <p className="flex items-start gap-2.5 text-sm leading-relaxed text-stone-600">
              <Lock size={16} className="mt-0.5 shrink-0 text-leaf-700" aria-hidden="true" />
              <span>
                <span className="font-semibold text-stone-800">Privacidad:</span> los comentarios
                serán utilizados exclusivamente con fines académicos e investigativos. Los
                resultados serán tratados de forma anónima y agregada.
              </span>
            </p>
          </div>

          <BotonCargando
            type="submit"
            cargando={enviando}
            disabled={enviando || !tipo || !contenido.trim()}
            className="w-full"
          >
            <Send size={15} aria-hidden="true" />
            Enviar comentario
          </BotonCargando>
        </form>
      )}

      {/* MIS APORTES: solo los del propio residente */}
      <section aria-labelledby="mis-aportes-titulo" className="space-y-3 pt-2">
        <h2 id="mis-aportes-titulo" className="font-display text-lg font-semibold text-leaf-900">
          Mis aportes
        </h2>
        {misAportes === null ? (
          <PantallaCarga />
        ) : misAportes.length === 0 ? (
          <p className="rounded-2xl border border-dashed border-stone-300 p-4 text-sm text-stone-500">
            Todavía no has enviado aportes. Cuando lo hagas, aparecerán aquí.
          </p>
        ) : (
          <ul className="space-y-2.5">
            {misAportes.map((aporte) => (
              <li key={aporte.id} className="rounded-2xl border border-stone-200 bg-white p-4 shadow-sm">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="rounded-full bg-leaf-100 px-2.5 py-0.5 text-xs font-semibold text-leaf-800">
                    {ETIQUETA_TIPO[aporte.tipo] || aporte.tipo}
                  </span>
                  <span className="text-xs text-stone-400">{formatearFecha(aporte.creadoEn)}</span>
                </div>
                <p className="mt-2 text-sm leading-relaxed text-stone-700 break-words">{aporte.contenido}</p>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
