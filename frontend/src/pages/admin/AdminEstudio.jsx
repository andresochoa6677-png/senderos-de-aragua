import { useEffect, useState } from 'react'
import { fetchAdminEstudio, habilitarPosprueba, deshabilitarPosprueba } from '../../api/client.js'
import { BotonCargando, Aviso } from '../../components/ui.jsx'
import PantallaCarga from '../../components/PantallaCarga.jsx'

const fechaHora = (instant) =>
  instant ? new Date(instant).toLocaleString('es-VE', { dateStyle: 'long', timeStyle: 'short' }) : '—'

export default function AdminEstudio() {
  const [estado, setEstado] = useState(null)
  const [error, setError] = useState('')
  const [procesando, setProcesando] = useState(false)

  const cargar = async () => {
    try { setEstado(await fetchAdminEstudio()) } catch (err) {
      setError(err.handledMessage || 'No pudimos cargar la configuración del estudio.')
    }
  }

  useEffect(() => { cargar() }, [])

  const alternar = async () => {
    if (!window.confirm(estado.pospruebaHabilitada
      ? '¿Confirmas deshabilitar la encuesta final? Los residentes dejarán de verla.'
      : '¿Confirmas habilitar la encuesta final? Quedará disponible de inmediato para todos.')) return
    setError('')
    setProcesando(true)
    try {
      if (estado.pospruebaHabilitada) await deshabilitarPosprueba()
      else await habilitarPosprueba()
      await cargar()
    } catch (err) {
      setError(err.handledMessage || 'No pudimos cambiar el estado de la encuesta final.')
    } finally { setProcesando(false) }
  }

  if (!estado && !error) return <PantallaCarga />

  const habilitada = estado?.pospruebaHabilitada

  return (
    <section className="animate-fade-up max-w-2xl">
      <h1 className="font-display text-2xl font-semibold text-leaf-900">Configuración del estudio</h1>
      <p className="mt-1 text-sm text-stone-500">
        La encuesta final la controlas tú: está deshabilitada por defecto y solo queda disponible
        cuando la habilitas. No hay cronómetro ni ventana de días; puedes deshabilitarla cuando quieras.
      </p>

      {error && <div className="mt-4"><Aviso tipo="error">{error}</Aviso></div>}

      {estado && (
        <div className="mt-6 rounded-3xl bg-white p-6 shadow-card sm:p-8">
          <div className="flex items-center justify-between gap-3">
            <h2 className="font-display text-lg font-semibold text-leaf-900">Encuesta final (posprueba)</h2>
            <span className={`rounded-full px-3 py-1 text-xs font-semibold ${habilitada ? 'bg-leaf-50 text-leaf-700' : 'bg-amber-50 text-amber-700'}`}>
              {habilitada ? 'Habilitada' : 'Deshabilitada'}
            </span>
          </div>

          <dl className="mt-4 space-y-3 text-sm">
            <div className="flex justify-between gap-3 rounded-2xl bg-stone-50 px-4 py-3">
              <dt className="text-stone-500">Estado actual</dt>
              <dd className="font-semibold text-leaf-900">
                {habilitada
                  ? 'Disponible para todas las viviendas con preprueba respondida'
                  : 'Los residentes ven el mensaje de que aún no está disponible'}
              </dd>
            </div>
            <div className="flex justify-between gap-3 rounded-2xl bg-stone-50 px-4 py-3">
              <dt className="text-stone-500">Última habilitación (trazabilidad)</dt>
              <dd className="font-semibold text-leaf-900">{fechaHora(estado.pospruebaHabilitadaEn)}</dd>
            </div>
          </dl>

          <div className="mt-5">
            <BotonCargando type="button" onClick={alternar} cargando={procesando}
              className={habilitada ? undefined : 'bg-leaf-600 hover:bg-leaf-700'}>
              {habilitada ? 'Deshabilitar encuesta final' : 'Habilitar encuesta final'}
            </BotonCargando>
            <p className="mt-2 text-xs text-stone-400">
              Se te pedirá confirmación antes de cambiar el estado.
            </p>
          </div>
        </div>
      )}
    </section>
  )
}
