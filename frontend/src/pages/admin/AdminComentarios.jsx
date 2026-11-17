import { useEffect, useMemo, useState } from 'react'
import { fetchAdminComentarios, actualizarCategoriaComentario } from '../../api/client.js'
import { Aviso } from '../../components/ui.jsx'
import PantallaCarga from '../../components/PantallaCarga.jsx'

const fecha = (instant) =>
  instant ? new Date(instant).toLocaleString('es-VE', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '—'

const CATEGORIAS = [
  'inquietudes-horarios', 'sugerencias-mejora', 'reconocimiento-informacion',
  'reportes-situacion', 'otro'
]

export default function AdminComentarios() {
  const [comentarios, setComentarios] = useState(null)
  const [guardando, setGuardando] = useState(null)
  const [error, setError] = useState('')
  const [tipo, setTipo] = useState('TODOS')
  const [tema, setTema] = useState('TODOS')
  const [busqueda, setBusqueda] = useState('')

  useEffect(() => {
    fetchAdminComentarios().then(setComentarios).catch((err) => setError(err.handledMessage || 'No pudimos cargar los comentarios.'))
  }, [])

  const cambiarCategoria = async (id, categoria) => {
    setGuardando(id)
    try {
      await actualizarCategoriaComentario(id, categoria || null)
      setComentarios((lista) => lista.map((c) => (c.id === id ? { ...c, categoriaTematica: categoria || null } : c)))
    } catch (err) {
      setError(err.handledMessage || 'No pudimos guardar la categoría.')
    } finally {
      setGuardando(null)
    }
  }

  const tipos = useMemo(() => [...new Set((comentarios ?? []).map((c) => c.tipo))], [comentarios])
  const temas = useMemo(() => [...new Set((comentarios ?? []).map((c) => c.temaDeclarado).filter(Boolean))], [comentarios])

  const filtrados = useMemo(() => (comentarios ?? []).filter((c) => {
    if (tipo !== 'TODOS' && c.tipo !== tipo) return false
    if (tema !== 'TODOS' && c.temaDeclarado !== tema) return false
    if (busqueda && !`${c.contenido} ${c.viviendaCodigo ?? ''}`.toLowerCase().includes(busqueda.toLowerCase())) return false
    return true
  }), [comentarios, tipo, tema, busqueda])

  if (!comentarios && !error) return <PantallaCarga />

  return (
    <section className="animate-fade-up">
      <h1 className="font-display text-2xl font-semibold text-leaf-900">Comentarios</h1>
      <p className="mt-1 text-sm text-stone-500">
        Participación completa de las viviendas ({comentarios?.length ?? 0} en total). Datos para el análisis cualitativo.
      </p>

      {error && <div className="mt-4"><Aviso tipo="error">{error}</Aviso></div>}

      <div className="mt-5 flex flex-wrap gap-2">
        <select value={tipo} onChange={(e) => setTipo(e.target.value)}
          className="rounded-xl border border-stone-300 bg-white px-3 py-2 text-sm text-stone-700 focus:outline-none focus:ring-2 focus:ring-leaf-600">
          <option value="TODOS">Todos los tipos</option>
          {tipos.map((t) => <option key={t} value={t}>{t}</option>)}
        </select>
        <select value={tema} onChange={(e) => setTema(e.target.value)}
          className="rounded-xl border border-stone-300 bg-white px-3 py-2 text-sm text-stone-700 focus:outline-none focus:ring-2 focus:ring-leaf-600">
          <option value="TODOS">Todos los temas</option>
          {temas.map((t) => <option key={t} value={t}>{t}</option>)}
        </select>
        <input
          type="search"
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
          placeholder="Buscar por texto o vivienda…"
          className="min-w-[220px] flex-1 rounded-xl border border-stone-300 bg-white px-3 py-2 text-sm text-stone-700 focus:outline-none focus:ring-2 focus:ring-leaf-600"
        />
      </div>

      {filtrados && (
        <div className="mt-5 space-y-3">
          {filtrados.length === 0 && (
            <div className="rounded-2xl bg-white p-6 text-center text-sm text-stone-400 shadow-card">
              No hay comentarios que coincidan con los filtros.
            </div>
          )}
          {filtrados.map((c) => (
            <article key={c.id} className="rounded-2xl bg-white p-4 shadow-card">
              <div className="flex flex-wrap items-center gap-2">
                <span className="rounded-full bg-leaf-50 px-2.5 py-0.5 text-xs font-semibold text-leaf-700">{c.tipo}</span>
                {c.temaDeclarado && (
                  <span className="rounded-full bg-stone-100 px-2.5 py-0.5 text-xs font-medium text-stone-600">{c.temaDeclarado}</span>
                )}
                <span className="text-xs font-semibold text-stone-500">Vivienda {c.viviendaCodigo ?? '—'}</span>
                <span className="ml-auto text-xs text-stone-400">{fecha(c.creadoEn)}</span>
              </div>
              <p className="mt-2 text-sm leading-relaxed text-stone-700">{c.contenido}</p>
              <div className="mt-2 flex flex-wrap items-center gap-2">
                <label className="text-xs text-stone-400" htmlFor={`cat-${c.id}`}>Categoría temática:</label>
                <select
                  id={`cat-${c.id}`}
                  value={c.categoriaTematica || ''}
                  onChange={(e) => cambiarCategoria(c.id, e.target.value)}
                  disabled={guardando === c.id}
                  className="rounded-lg border border-stone-300 bg-white px-2 py-1 text-xs text-stone-700 focus:outline-none focus:ring-2 focus:ring-leaf-600"
                >
                  <option value="">sin categoría</option>
                  {CATEGORIAS.map((cat) => <option key={cat} value={cat}>{cat}</option>)}
                </select>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}
