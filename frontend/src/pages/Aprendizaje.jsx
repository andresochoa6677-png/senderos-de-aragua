import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  BookOpen, CheckCircle2, CircleDashed, Globe, Recycle, Droplets, Leaf, AlertTriangle,
  Users, Lightbulb, Sparkles, Dumbbell, Lock, Play, ArrowRight, Trophy
} from 'lucide-react'
import { fetchLecciones, fetchLeccion } from '../api/client.js'
import { Aviso } from '../components/ui.jsx'
import PantallaCarga from '../components/PantallaCarga.jsx'

const ICONOS = { Globe, Recycle, Droplets, Leaf, AlertTriangle, Users, BookOpen }

const COLORES = {
  leaf: 'bg-leaf-100 text-leaf-700',
  earth: 'bg-amber-100 text-amber-700',
  sky: 'bg-sky-100 text-sky-700',
  amber: 'bg-amber-100 text-amber-700'
}

function iconoLeccion(nombre, size = 22) {
  const Icono = ICONOS[nombre] || BookOpen
  return <Icono size={size} aria-hidden="true" />
}

/** Apariencia del estado del recorrido obligatorio por leccion. */
const ESTADOS = {
  COMPLETADA: {
    etiqueta: 'Completada',
    chip: 'bg-leaf-100 text-leaf-700',
    Icono: CheckCircle2
  },
  ACTUAL: {
    etiqueta: 'Lección actual',
    chip: 'bg-leaf-600 text-white',
    Icono: Play
  },
  BLOQUEADA: {
    etiqueta: 'Bloqueada',
    chip: 'bg-stone-100 text-stone-500',
    Icono: Lock
  }
}

export function ListaLecciones() {
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

  const completadas = lecciones.filter((l) => l.estado === 'COMPLETADA').length
  const total = lecciones.length
  const porcentaje = total === 0 ? 0 : Math.round((completadas * 100) / total)
  const leccionActual = lecciones.find((l) => l.estado === 'ACTUAL')

  return (
    <div className="space-y-6">
      <header>
        <h1 className="font-display text-3xl font-semibold text-leaf-900">Aprendizaje</h1>
        <p className="mt-2 max-w-2xl text-sm leading-relaxed text-stone-600">
          Recorrido guiado, lección por lección. Completa los ejercicios de cada
          lección para desbloquear la siguiente.
        </p>
      </header>

      {/* Avance general del recorrido */}
      <section aria-labelledby="aprend-avance" className="rounded-3xl border border-leaf-100 bg-leaf-50/70 p-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <p id="aprend-avance" className="text-sm font-semibold text-leaf-900">
            {completadas} de {total} lecciones completadas
          </p>
          {leccionActual ? (
            <p className="flex items-center gap-1.5 text-sm text-leaf-800">
              <Play size={14} aria-hidden="true" />
              Actual: <span className="font-semibold">Lección {leccionActual.orden}</span>
            </p>
          ) : (
            <p className="flex items-center gap-1.5 text-sm font-semibold text-leaf-700">
              <Trophy size={14} aria-hidden="true" /> ¡Recorrido completo!
            </p>
          )}
        </div>
        <div
          className="mt-3 h-2.5 w-full overflow-hidden rounded-full bg-leaf-200/60"
          role="progressbar" aria-valuenow={porcentaje} aria-valuemin="0" aria-valuemax="100"
        >
          <div
            className="h-full rounded-full bg-leaf-600 transition-all duration-500"
            style={{ width: `${porcentaje}%` }}
          />
        </div>
      </section>

      {/* Recorrido secuencial */}
      <ol className="relative space-y-4">
        {lecciones.map((l, indice) => {
          const esUltima = indice === lecciones.length - 1
          const info = ESTADOS[l.estado] || ESTADOS.BLOQUEADA
          const bloqueada = l.estado === 'BLOQUEADA'

          const tarjeta = (
            <span className={`group flex h-full items-stretch gap-4 rounded-3xl border bg-white p-4 transition-all sm:p-5 ${
              l.estado === 'ACTUAL'
                ? 'border-leaf-500 shadow-card ring-2 ring-leaf-500/30'
                : bloqueada
                  ? 'border-stone-200 opacity-70'
                  : 'border-stone-200 shadow-sm hover:border-leaf-400 hover:shadow-card'
            }`}>
              {l.imagen && (
                <span className={`relative hidden h-24 w-32 shrink-0 overflow-hidden rounded-2xl bg-leaf-50 sm:block ${bloqueada ? 'grayscale' : ''}`}>
                  <img
                    src={l.imagen}
                    alt=""
                    loading="lazy"
                    width={1024}
                    height={640}
                    className={`h-full w-full object-cover ${bloqueada ? '' : 'transition-transform duration-300 group-hover:scale-105'}`}
                  />
                  {bloqueada && (
                    <span className="absolute inset-0 grid place-items-center bg-stone-900/40 text-white">
                      <Lock size={20} aria-hidden="true" />
                    </span>
                  )}
                </span>
              )}
              <span className="flex min-w-0 flex-1 flex-col">
                <span className="flex flex-wrap items-center gap-2">
                  <span className="text-xs font-semibold uppercase tracking-wide text-stone-400">
                    Lección {l.orden}
                  </span>
                  <span className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold ${info.chip}`}>
                    <info.Icono size={12} aria-hidden="true" /> {info.etiqueta}
                  </span>
                  {typeof l.ejerciciosTotales === 'number' && !bloqueada && (
                    <span className="inline-flex items-center gap-1 rounded-full bg-stone-100 px-2.5 py-0.5 text-xs font-medium text-stone-600">
                      <Dumbbell size={12} aria-hidden="true" />
                      Ejercicios {l.ejerciciosCorrectos}/{l.ejerciciosTotales}
                    </span>
                  )}
                </span>
                <span className={`mt-1.5 block font-semibold ${bloqueada ? 'text-stone-500' : 'text-stone-800 group-hover:text-leaf-800'}`}>
                  {l.titulo}
                </span>
                <span className="mt-1 block text-sm leading-relaxed text-stone-500">{l.resumen}</span>

                {l.estado === 'ACTUAL' && (
                  <span className="mt-3 inline-flex w-fit items-center gap-1.5 rounded-xl bg-leaf-600 px-3.5 py-2 text-sm font-semibold text-white">
                    {l.ejerciciosCorrectos > 0 ? 'Continuar el recorrido' : 'Comenzar la lección'}
                    <ArrowRight size={15} aria-hidden="true" />
                  </span>
                )}
                {l.estado === 'COMPLETADA' && (
                  <span className="mt-3 inline-flex w-fit items-center gap-1.5 text-sm font-medium text-leaf-700">
                    Repasar la lección <ArrowRight size={14} aria-hidden="true" />
                  </span>
                )}
                {bloqueada && (
                  <span className="mt-3 block text-xs font-medium text-stone-400">
                    Completa los ejercicios de la lección anterior para desbloquearla.
                  </span>
                )}
              </span>
            </span>
          )

          return (
            <li key={l.id} className="relative flex gap-4">
              {/* Columna de estado del paso */}
              <span className="flex flex-col items-center pt-1" aria-hidden="true">
                <span className={`grid h-9 w-9 shrink-0 place-items-center rounded-full ring-4 ring-white ${
                  l.estado === 'COMPLETADA' ? 'bg-leaf-600 text-white'
                    : l.estado === 'ACTUAL' ? 'bg-leaf-100 text-leaf-700 ring-leaf-200'
                      : 'bg-stone-100 text-stone-400'
                }`}>
                  {l.estado === 'COMPLETADA' ? <CheckCircle2 size={18} />
                    : l.estado === 'ACTUAL' ? <Play size={16} />
                      : <CircleDashed size={18} />}
                </span>
                {!esUltima && <span className="mt-1 w-px flex-1 bg-stone-200" />}
              </span>

              <span className="min-w-0 flex-1">
                {bloqueada ? (
                  <span className="block cursor-not-allowed">{tarjeta}</span>
                ) : (
                  <Link
                    to={l.estado === 'ACTUAL' && l.ejerciciosCorrectos >= l.ejerciciosTotales && l.ejerciciosTotales > 0
                      ? `/ejercicios/${l.id}`
                      : `/aprendizaje/${l.id}`}
                    className="block h-full focus:outline-none focus-visible:ring-2 focus-visible:ring-leaf-600 focus-visible:ring-offset-2 rounded-3xl"
                  >
                    {tarjeta}
                  </Link>
                )}
              </span>
            </li>
          )
        })}
      </ol>
    </div>
  )
}

/** Render mínimo de markdown (encabezados, listas, citas, negritas). */
function Markdown({ texto }) {
  const lineas = texto.split('\n')
  const bloques = []
  let lista = []

  const cerrarLista = () => {
    if (lista.length > 0) {
      bloques.push(<ul key={`ul-${bloques.length}`} className="my-3 space-y-1.5 pl-5">{lista}</ul>)
      lista = []
    }
  }

  const enLinea = (s) => {
    const partes = s.split(/(\*\*[^*]+\*\*)/g)
    return partes.map((p, i) =>
      p.startsWith('**') && p.endsWith('**')
        ? <strong key={i}>{p.slice(2, -2)}</strong>
        : <span key={i}>{p}</span>
    )
  }

  lineas.forEach((linea, i) => {
    const t = linea.trim()
    if (!t) { cerrarLista(); return }
    if (t.startsWith('- ')) {
      lista.push(<li key={`li-${i}`} className="text-sm text-stone-700">{enLinea(t.slice(2))}</li>)
      return
    }
    cerrarLista()
    if (t.startsWith('### ')) bloques.push(<h4 key={i} className="mt-5 font-display font-semibold text-stone-800">{enLinea(t.slice(4))}</h4>)
    else if (t.startsWith('## ')) bloques.push(<h3 key={i} className="mt-6 font-display text-lg font-semibold text-stone-800">{enLinea(t.slice(3))}</h3>)
    else if (t.startsWith('# ')) bloques.push(<h2 key={i} className="mt-2 font-display text-xl font-semibold text-leaf-900">{enLinea(t.slice(2))}</h2>)
    else if (t.startsWith('> ')) bloques.push(<blockquote key={i} className="my-4 rounded-2xl border-l-4 border-leaf-500 bg-leaf-50 px-4 py-3 text-sm text-leaf-900">{enLinea(t.slice(2))}</blockquote>)
    else bloques.push(<p key={i} className="mt-3 text-sm leading-relaxed text-stone-700">{enLinea(t)}</p>)
  })
  cerrarLista()

  return <div>{bloques}</div>
}

export function DetalleLeccion() {
  const { id } = useParams()
  const [leccion, setLeccion] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let activo = true
    setLeccion(null)
    setError('')
    ;(async () => {
      try {
        const data = await fetchLeccion(id)
        if (activo) setLeccion(data)
      } catch (err) {
        if (activo) setError(err.handledMessage || 'No pudimos cargar la lección.')
      }
    })()
    return () => { activo = false }
  }, [id])

  if (error) {
    return (
      <section className="max-w-2xl mx-auto space-y-4">
        <Aviso tipo="error">{error}</Aviso>
        <Link to="/aprendizaje" className="text-sm font-medium text-leaf-700 hover:underline">← Volver a lecciones</Link>
      </section>
    )
  }
  if (!leccion) return <PantallaCarga />

  const terminada = Boolean(leccion.completadoEn)

  return (
    <section className="max-w-2xl mx-auto">
      <Link to="/aprendizaje" className="text-sm font-medium text-leaf-700 hover:underline">← Volver a lecciones</Link>

      <article className="mt-4 rounded-3xl bg-white p-6 shadow-card sm:p-8">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <p className="text-xs font-semibold uppercase tracking-wide text-leaf-700">Lección {leccion.orden}</p>
          {terminada ? (
            <span className="inline-flex items-center gap-1 rounded-full bg-leaf-100 px-2.5 py-1 text-xs font-semibold text-leaf-700">
              <CheckCircle2 size={13} aria-hidden="true" /> Completada
            </span>
          ) : (
            <span className="inline-flex items-center gap-1 rounded-full bg-leaf-600 px-2.5 py-1 text-xs font-semibold text-white">
              <Play size={12} aria-hidden="true" /> En curso
            </span>
          )}
        </div>
        <h1 className="mt-2 font-display text-2xl font-semibold text-leaf-900">{leccion.titulo}</h1>

        {leccion.imagen && (
          <figure className="mt-5 overflow-hidden rounded-2xl border border-leaf-100 bg-leaf-50">
            <img
              src={leccion.imagen}
              alt={`Recurso visual de la lección: ${leccion.titulo}`}
              loading="lazy"
              width={1024}
              height={640}
              className="h-auto w-full object-cover"
            />
            <figcaption className="sr-only">Recurso visual de apoyo de la lección {leccion.orden}.</figcaption>
          </figure>
        )}

        <div className="mt-6">
          <Markdown texto={leccion.contenido} />
        </div>

        {leccion.ejemplo && (
          <section
            aria-labelledby={`ejemplo-${leccion.id}`}
            className="mt-6 rounded-2xl border border-earth-200 bg-earth-50 p-5"
          >
            <h2 id={`ejemplo-${leccion.id}`} className="flex items-center gap-2 font-display text-sm font-semibold text-earth-800">
              <Lightbulb size={16} aria-hidden="true" /> Ejemplo cotidiano
            </h2>
            <p className="mt-2 text-sm leading-relaxed text-stone-700">{leccion.ejemplo}</p>
          </section>
        )}

        {leccion.ideaClave && (
          <section
            aria-labelledby={`idea-${leccion.id}`}
            className="mt-4 rounded-2xl bg-leaf-700 p-5 text-white"
          >
            <h2 id={`idea-${leccion.id}`} className="flex items-center gap-2 font-display text-sm font-semibold">
              <Sparkles size={16} aria-hidden="true" /> Idea clave
            </h2>
            <p className="mt-2 text-sm leading-relaxed text-leaf-50">{leccion.ideaClave}</p>
          </section>
        )}

        {/* Ahora practiquemos: la actividad obligatoria de la leccion */}
        <section
          aria-labelledby={`practica-${leccion.id}`}
          className={`mt-8 rounded-3xl border-2 p-5 transition-colors ${
            terminada ? 'border-leaf-200 bg-leaf-50/50' : 'border-leaf-400 bg-leaf-50'
          }`}
        >
          <h2 id={`practica-${leccion.id}`} className="flex items-center gap-2 font-display text-lg font-semibold text-leaf-900">
            <Dumbbell size={19} className="text-leaf-600" aria-hidden="true" />
            Ahora practiquemos
          </h2>
          <p className="mt-1.5 text-sm leading-relaxed text-stone-600">
            {terminada
              ? 'Ya superaste la actividad de esta lección. Puedes repetirla para practicar.'
              : 'Pon en práctica lo que aprendiste. Supera la actividad para completar la lección y desbloquear la siguiente.'}
          </p>
          <Link
            to={`/ejercicios/${leccion.id}`}
            className="mt-4 flex w-full items-center justify-center gap-2 rounded-2xl bg-leaf-700 px-4 py-3 text-sm font-semibold text-white transition-all hover:-translate-y-px hover:bg-leaf-800 active:translate-y-0 focus:outline-none focus-visible:ring-2 focus-visible:ring-leaf-600"
          >
            {terminada ? 'Repasar la actividad' : 'Ir a la actividad'}
            <ArrowRight size={15} aria-hidden="true" />
          </Link>
        </section>
      </article>
    </section>
  )
}
