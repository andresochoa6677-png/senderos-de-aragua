import { useEffect, useState } from 'react'
import { fetchAdminSesiones } from '../../api/client.js'
import { Aviso } from '../../components/ui.jsx'
import PantallaCarga from '../../components/PantallaCarga.jsx'

export default function AdminSesiones() {
  const [sesiones, setSesiones] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    fetchAdminSesiones().then(setSesiones).catch((err) => setError(err.handledMessage || 'No pudimos cargar las sesiones.'))
  }, [])

  if (!sesiones && !error) return <PantallaCarga />

  return (
    <section className="animate-fade-up">
      <h1 className="font-display text-2xl font-semibold text-leaf-900">Sesiones activas</h1>
      <p className="mt-1 text-sm text-stone-500">
        Sesiones HTTP reales por cuenta (máximo una activa por usuario; la demostración no aparece).
      </p>

      {error && <div className="mt-4"><Aviso tipo="error">{error}</Aviso></div>}

      {sesiones && (
        <div className="mt-5 overflow-hidden rounded-3xl bg-white shadow-card">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-stone-100 text-xs uppercase tracking-wide text-stone-400">
              <tr>
                <th className="px-4 py-3">Correo</th>
                <th className="px-4 py-3">Sesiones activas</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-stone-100">
              {sesiones.map((s) => (
                <tr key={s.email} className="text-stone-700">
                  <td className="px-4 py-3 font-medium">{s.email}</td>
                  <td className="px-4 py-3 tabular-nums">{s.sesiones}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
