import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  BookOpen, CheckCircle2, CircleDashed, Globe, Recycle, Droplets, Leaf, AlertTriangle,
  Users, Lock, Play, ArrowRight, Trophy
} from 'lucide-react'
import { fetchProgreso } from '../api/client.js'
import { Aviso } from '../components/ui.jsx'
import PantallaCarga from '../components/PantallaCarga.jsx'

const ICONOS = { Globe, Recycle, Droplets, Leaf, AlertTriangle, Users, BookOpen }

function iconoLeccion(nombre, size = 18) {
  const Icono = ICONOS[nombre] || BookOpen
  return <Icono size={size} aria-hidden="true" />
}

function formatearFecha(iso) {
  if (!iso) return '—'
  const fecha = new Date(iso)
  return fecha.toLocaleDateString('es', { day: 'numeric', month: 'short' })
}

const ESTADO_ITEM = {
  COMPLETADA: { etiqueta: 'Completada', Icono: CheckCircle2, clases: 'text-leaf-600' },
  ACTUAL: { etiqueta: 'Lección actual', Icono: Play, clases: 'text-leaf-600' },
  BLOQUEADA: { etiqueta: 'Bloqueada', Icono: Lock, clases: 'text-stone-300' }
}

export default function Progreso() {
  const [progreso, setProgreso] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let activo = true
    ;(async () => {
      try {
        const data = await fetchProgreso()
        if (activo) setProgreso(data)
      } catch (err) {
        if (activo) setError(err.handledMessage || 'No pudimos cargar tu progreso.')
      }
    })()
    return () => { activo = false }
  }, [])

  if (error) {
    return <section className="max-w-3xl mx-auto"><Aviso tipo="error">{error}</Aviso></section>
  }
  if (!progreso) return <PantallaCarga />

  const { totalLecciones, completadas, porcentaje, ultimaActividad, leccionActualId, lecciones } = progreso
  const actual = lecciones.find((l) => l.leccionId === leccionActualId)
  const recorridoCompleto = totalLecciones > 0 && completadas === totalLecciones

  return (
    <div className="space-y-6">
      <header>
        <h1 className="font-display text-3xl font-semibold text-leaf-900">Progreso</h1>
        <p className="mt-2 max-w-2xl text-sm leading-relaxed text-stone-600">
          Avance de tu vivienda en el recorrido educativo del condominio.
        </p>
      </header>

      <section aria-labelledby="prog-resumen" className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-3xl bg-leaf-700 p-5 text-white shadow-card">
          <p className="text-xs font-semibold uppercase tracking-wide text-leaf-200">Avance</p>
          <p className="mt-2 font-display text-4xl font-semibold">{porcentaje}%</p>
          <p className="mt-1 text-sm text-leaf-100">{completadas} de {totalLecciones} lecciones</p>
        </div>
        <div className="rounded-3xl border border-stone-200 bg-white p-5 shadow-sm">
          <p className="text-xs font-semibold uppercase tracking-wide text-stone-500">Completadas</p>
          <p className="mt-2 font-display text-4xl font-semibold text-stone-800">{completadas}</p>
        </div>
        <div className="rounded-3xl border border-stone-200 bg-white p-5 shadow-sm">
          <p className="text-xs font-semibold uppercase tracking-wide text-stone-500">Última actividad</p>
          <p className="mt-2 font-display text-lg font-semibold text-stone-800">{formatearFecha(ultimaActividad)}</p>
        </div>
      </section>

      <section aria-labelledby="prog-barra">
        <div className="h-3 w-full overflow-hidden rounded-full bg-stone-200" role="progressbar" aria-valuenow={porcentaje} aria-valuemin="0" aria-valuemax="100" aria-labelledby="prog-barra">
          <div className="h-full rounded-full bg-leaf-600 transition-all" style={{ width: `${porcentaje}%` }} />
        </div>
        <p id="prog-barra" className="sr-only">{porcentaje}% del contenido completado</p>
      </section>

      {/* Llamada a la accion del recorrido */}
      {recorridoCompleto ? (
        <section className="flex items-center gap-3 rounded-3xl bg-leaf-700 p-5 text-white shadow-card">
          <Trophy size={24} aria-hidden="true" />
          <div>
            <p className="font-display text-lg font-semibold">¡Recorrido educativo completado!</p>
            <p className="text-sm text-leaf-100">Tu vivienda finalizó las {totalLecciones} lecciones.</p>
          </div>
        </section>
      ) : actual ? (
        <Link
          to={`/aprendizaje/${actual.leccionId}`}
          className="flex items-center justify-between gap-4 rounded-3xl border border-leaf-300 bg-leaf-50 p-5 transition-all hover:-translate-y-px hover:shadow-card focus:outline-none focus-visible:ring-2 focus-visible:ring-leaf-600"
        >
          <span className="flex items-center gap-4">
            <span className="grid h-11 w-11 shrink-0 place-items-center rounded-2xl bg-leaf-600 text-white">
              <Play size={20} aria-hidden="true" />
            </span>
            <span>
              <span className="block text-xs font-semibold uppercase tracking-wide text-leaf-700">Continúa aquí</span>
              <span className="block font-semibold text-leaf-900">
                Lección {actual.orden}: {actual.titulo}
              </span>
            </span>
          </span>
          <ArrowRight size={20} className="shrink-0 text-leaf-700" aria-hidden="true" />
        </Link>
      ) : null}

      <section aria-labelledby="prog-lista">
        <h2 id="prog-lista" className="text-sm font-semibold uppercase tracking-wide text-stone-500">Lecciones</h2>
        <ul className="mt-3 space-y-2">
          {lecciones.map((l) => {
            const info = ESTADO_ITEM[l.estado] || ESTADO_ITEM.BLOQUEADA
            return (
              <li
                key={l.leccionId}
                className={`flex items-center gap-4 rounded-2xl border px-4 py-3 ${
                  l.estado === 'ACTUAL' ? 'border-leaf-400 bg-leaf-50/60' : 'border-stone-200 bg-white'
                }`}
              >
                <span className={`grid h-9 w-9 shrink-0 place-items-center rounded-xl ${
                  l.estado === 'COMPLETADA' ? 'bg-leaf-100 text-leaf-700'
                    : l.estado === 'ACTUAL' ? 'bg-leaf-600 text-white'
                      : 'bg-stone-100 text-stone-400'
                }`}>
                  {iconoLeccion(l.icono)}
                </span>
                <span className="min-w-0 flex-1">
                  <span className={`block text-sm font-semibold ${l.estado === 'BLOQUEADA' ? 'text-stone-400' : 'text-stone-800'}`}>
                    {l.orden}. {l.titulo}
                  </span>
                  <span className="block text-xs text-stone-500">
                    {l.estado === 'COMPLETADA' && `Completada el ${formatearFecha(l.completadoEn)}`}
                    {l.estado === 'ACTUAL' && (l.iniciadoEn ? `En curso desde el ${formatearFecha(l.iniciadoEn)}` : 'Lista para comenzar')}
                    {l.estado === 'BLOQUEADA' && 'Completa la lección anterior para desbloquear'}
                  </span>
                </span>
                <info.Icono size={20} className={info.clases} aria-label={info.etiqueta} />
              </li>
            )
          })}
        </ul>
      </section>
    </div>
  )
}
