import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Home, ClipboardCheck, ClipboardList, MessageSquare, ToggleRight, ToggleLeft, Settings } from 'lucide-react'
import { fetchAdminEstudio, habilitarPosprueba, deshabilitarPosprueba, exportarCsv } from '../../api/client.js'
import { BotonCargando, Aviso } from '../../components/ui.jsx'
import PantallaCarga from '../../components/PantallaCarga.jsx'

const fechaHora = (instant) =>
  instant ? new Date(instant).toLocaleString('es-VE', { dateStyle: 'long', timeStyle: 'short' }) : '—'

function Tarjeta({ icono: Icon, etiqueta, valor }) {
  return (
    <div className="rounded-2xl border border-stone-200 bg-white p-4 shadow-card">
      <span className="inline-flex h-9 w-9 items-center justify-center rounded-xl bg-leaf-100 text-leaf-700">
        <Icon size={18} aria-hidden="true" />
      </span>
      <p className="mt-3 text-2xl font-semibold tabular-nums text-leaf-900">{valor}</p>
      <p className="text-xs font-medium text-stone-500">{etiqueta}</p>
    </div>
  )
}

export default function AdminPanelInicio() {
  const navigate = useNavigate()
  const [estado, setEstado] = useState(null)
  const [error, setError] = useState('')
  const [confirmando, setConfirmando] = useState(null) // 'habilitar' | 'deshabilitar'
  const [procesando, setProcesando] = useState(false)

  const cargar = async () => {
    try {
      setEstado(await fetchAdminEstudio())
    } catch (err) {
      setError(err.handledMessage || 'No pudimos cargar el estado del estudio.')
    }
  }

  useEffect(() => { cargar() }, [])

  const alternar = async () => {
    setError('')
    setProcesando(true)
    try {
      if (confirmando === 'habilitar') await habilitarPosprueba()
      else await deshabilitarPosprueba()
      setConfirmando(null)
      await cargar()
    } catch (err) {
      setError(err.handledMessage || 'No pudimos cambiar el estado de la encuesta final.')
      setConfirmando(null)
    } finally {
      setProcesando(false)
    }
  }

  if (!estado && !error) return <PantallaCarga />

  const habilitada = estado?.pospruebaHabilitada

  return (
    <section className="animate-fade-up">
      <h1 className="font-display text-2xl font-semibold text-leaf-900">Panel de administración</h1>
      <p className="mt-1 text-sm text-stone-500">
        Seguimiento del estudio y control de la encuesta final.
      </p>

      {error && <div className="mt-4"><Aviso tipo="error">{error}</Aviso></div>}

      {estado && (
        <>
          <div className="mt-6 rounded-3xl bg-white p-6 shadow-card sm:p-8">
            <div className="flex items-center justify-between gap-3">
              <h2 className="font-display text-lg font-semibold text-leaf-900">Encuesta final (posprueba)</h2>
              <span className={`rounded-full px-3 py-1 text-xs font-semibold ${habilitada ? 'bg-leaf-50 text-leaf-700' : 'bg-amber-50 text-amber-700'}`}>
                {habilitada ? 'Habilitada' : 'No habilitada'}
              </span>
            </div>

            {habilitada ? (
              <div className="mt-4 grid gap-3 sm:grid-cols-2">
                <div className="rounded-2xl bg-stone-50 p-4">
                  <p className="text-xs font-medium text-stone-500">Habilitada desde</p>
                  <p className="mt-1 text-sm font-semibold text-leaf-900">{fechaHora(estado.pospruebaHabilitadaEn)}</p>
                </div>
                <div className="rounded-2xl bg-stone-50 p-4">
                  <p className="text-xs font-medium text-stone-500">Estado</p>
                  <p className="mt-1 text-sm font-semibold text-leaf-900">
                    Disponible para todas las viviendas con preprueba respondida.
                  </p>
                </div>
              </div>
            ) : (
              <p className="mt-3 text-sm text-stone-600">
                La encuesta final está deshabilitada: los residentes ven el mensaje de que aún no
                está disponible. Al habilitarla queda disponible de inmediato para todos.
              </p>
            )}

            <div className="mt-5 rounded-2xl border border-leaf-200 bg-leaf-50/60 p-4">
              {!confirmando ? (
                habilitada ? (
                  <button
                    type="button"
                    onClick={() => setConfirmando('deshabilitar')}
                    className="inline-flex items-center gap-2 rounded-2xl border border-stone-300 bg-white px-5 py-3 text-sm font-semibold text-stone-700 transition-colors hover:border-red-300 hover:text-red-600"
                  >
                    <ToggleLeft size={16} aria-hidden="true" />
                    Deshabilitar encuesta final
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={() => setConfirmando('habilitar')}
                    className="inline-flex items-center gap-2 rounded-2xl bg-leaf-600 px-5 py-3 text-sm font-semibold text-white shadow-card transition-colors hover:bg-leaf-700"
                  >
                    <ToggleRight size={16} aria-hidden="true" />
                    Habilitar encuesta final
                  </button>
                )
              ) : (
                <div>
                  <p className="text-sm font-medium text-leaf-900">
                    {confirmando === 'habilitar'
                      ? '¿Confirmas habilitar la encuesta final? Quedará disponible de inmediato para todos los residentes.'
                      : '¿Confirmas deshabilitar la encuesta final? Los residentes volverán a ver el mensaje de no disponible.'}
                  </p>
                  <div className="mt-3 flex gap-2">
                    <BotonCargando type="button" onClick={alternar} cargando={procesando}>
                      {confirmando === 'habilitar' ? 'Sí, habilitar ahora' : 'Sí, deshabilitar ahora'}
                    </BotonCargando>
                    <button
                      type="button"
                      onClick={() => setConfirmando(null)}
                      className="rounded-2xl border border-stone-300 px-4 py-2.5 text-sm font-medium text-stone-600 hover:border-stone-400"
                    >
                      Cancelar
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>

          <h2 className="mt-8 font-display text-lg font-semibold text-leaf-900">Participación</h2>
          <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            <Tarjeta icono={Home} etiqueta="Viviendas registradas" valor={estado.viviendasRegistradas} />
            <Tarjeta icono={ClipboardCheck} etiqueta="Con consentimiento" valor={estado.conConsentimiento} />
            <Tarjeta icono={ClipboardList} etiqueta="Con preprueba" valor={estado.conPreprueba} />
            <Tarjeta icono={ClipboardCheck} etiqueta="Con postprueba" valor={estado.conPostprueba} />
            <Tarjeta icono={MessageSquare} etiqueta="Comentarios recibidos" valor={estado.comentarios} />
          </div>

          <h2 className="mt-8 font-display text-lg font-semibold text-leaf-900">Exportaciones CSV</h2>
          <p className="mt-1 text-xs text-stone-500">
            Archivos con la fecha del día en el nombre, listos para Excel/Sheets.
          </p>
          <div className="mt-3 grid gap-3 sm:grid-cols-2">
            <button type="button" onClick={() => exportarCsv('preprueba')}
              className="rounded-2xl border border-stone-200 bg-white px-4 py-3 text-sm font-medium text-leaf-900 hover:border-leaf-400">
              ⬇ Preprueba por vivienda (18 ítems + puntaje)
            </button>
            <button type="button" onClick={() => exportarCsv('postprueba')}
              className="rounded-2xl border border-stone-200 bg-white px-4 py-3 text-sm font-medium text-leaf-900 hover:border-leaf-400">
              ⬇ Postprueba por vivienda (22 ítems + puntaje)
            </button>
            <button type="button" onClick={() => exportarCsv('uso')}
              className="rounded-2xl border border-stone-200 bg-white px-4 py-3 text-sm font-medium text-leaf-900 hover:border-leaf-400">
              ⬇ Métricas de uso por vivienda
            </button>
            <button type="button" onClick={() => exportarCsv('comentarios')}
              className="rounded-2xl border border-stone-200 bg-white px-4 py-3 text-sm font-medium text-leaf-900 hover:border-leaf-400">
              ⬇ Comentarios con categoría temática
            </button>
          </div>

          <div className="mt-6 grid gap-3 sm:grid-cols-3">
            <button type="button" onClick={() => navigate('/admin/viviendas')}
              className="inline-flex items-center justify-center gap-2 rounded-2xl border border-stone-200 bg-white px-4 py-3 text-sm font-medium text-leaf-900 hover:border-leaf-400">
              <Home size={16} aria-hidden="true" /> Ver viviendas
            </button>
            <button type="button" onClick={() => navigate('/admin/comentarios')}
              className="inline-flex items-center justify-center gap-2 rounded-2xl border border-stone-200 bg-white px-4 py-3 text-sm font-medium text-leaf-900 hover:border-leaf-400">
              <MessageSquare size={16} aria-hidden="true" /> Ver comentarios
            </button>
            <button type="button" onClick={() => navigate('/admin/estudio')}
              className="inline-flex items-center justify-center gap-2 rounded-2xl border border-stone-200 bg-white px-4 py-3 text-sm font-medium text-leaf-900 hover:border-leaf-400">
              <Settings size={16} aria-hidden="true" /> Configuración del estudio
            </button>
          </div>
        </>
      )}
    </section>
  )
}
