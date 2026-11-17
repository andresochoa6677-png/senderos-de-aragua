import { useEffect, useState } from 'react'
import { fetchAdminViviendas } from '../../api/client.js'
import { Aviso } from '../../components/ui.jsx'
import PantallaCarga from '../../components/PantallaCarga.jsx'

const fecha = (instant) =>
  instant ? new Date(instant).toLocaleDateString('es-VE', { day: 'numeric', month: 'short', year: 'numeric' }) : '—'

const SiNo = ({ valor }) => (
  <span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${valor ? 'bg-leaf-50 text-leaf-700' : 'bg-stone-100 text-stone-500'}`}>
    {valor ? 'Sí' : 'No'}
  </span>
)

export default function AdminViviendas() {
  const [viviendas, setViviendas] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    fetchAdminViviendas().then(setViviendas).catch((err) => setError(err.handledMessage || 'No pudimos cargar las viviendas.'))
  }, [])

  if (!viviendas && !error) return <PantallaCarga />

  return (
    <section className="animate-fade-up">
      <h1 className="font-display text-2xl font-semibold text-leaf-900">Viviendas</h1>
      <p className="mt-1 text-sm text-stone-500">
        Estado de participación por vivienda: registro, consentimiento, preprueba y postprueba.
      </p>

      {error && <div className="mt-4"><Aviso tipo="error">{error}</Aviso></div>}

      {viviendas && (
        <div className="mt-5 overflow-x-auto rounded-3xl bg-white shadow-card">
          <table className="w-full min-w-[720px] text-left text-sm">
            <thead className="border-b border-stone-100 text-xs uppercase tracking-wide text-stone-400">
              <tr>
                <th className="px-4 py-3">Vivienda</th>
                <th className="px-4 py-3">Registro</th>
                <th className="px-4 py-3">Consentimiento</th>
                <th className="px-4 py-3">Preprueba</th>
                <th className="px-4 py-3">Puntaje pre</th>
                <th className="px-4 py-3">Postprueba</th>
                <th className="px-4 py-3">Puntaje post</th>
                <th className="px-4 py-3">Final disponible</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-stone-100">
              {viviendas.length === 0 && (
                <tr><td colSpan={8} className="px-4 py-6 text-center text-stone-400">Aún no hay viviendas registradas.</td></tr>
              )}
              {viviendas.map((v) => (
                <tr key={v.codigo} className="text-stone-700">
                  <td className="px-4 py-3 font-semibold text-leaf-900">{v.codigo}</td>
                  <td className="px-4 py-3">{fecha(v.fecha_registro)}</td>
                  <td className="px-4 py-3"><SiNo valor={v.con_consentimiento} /></td>
                  <td className="px-4 py-3">{fecha(v.fecha_preprueba)}</td>
                  <td className="px-4 py-3 tabular-nums">{v.puntaje_pre ?? 0}/10</td>
                  <td className="px-4 py-3"><SiNo valor={v.con_postprueba} /></td>
                  <td className="px-4 py-3 tabular-nums">{v.puntaje_post ?? 0}/10</td>
                  <td className="px-4 py-3"><SiNo valor={v.disponible_final} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
