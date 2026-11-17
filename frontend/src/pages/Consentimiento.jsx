import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { fetchConsentimientoTexto, aceptarConsentimiento, rechazarConsentimiento, logout } from '../api/client.js'
import { BotonCargando, Aviso } from '../components/ui.jsx'
import PantallaCarga from '../components/PantallaCarga.jsx'

export default function Consentimiento() {
  const navigate = useNavigate()
  const { refreshOnboarding } = useAuth()
  const [documento, setDocumento] = useState(null)
  const [errorCarga, setErrorCarga] = useState('')
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)
  const [rechazando, setRechazando] = useState(false)

  useEffect(() => {
    let activo = true
    ;(async () => {
      try {
        const data = await fetchConsentimientoTexto()
        if (activo) setDocumento(data)
      } catch (err) {
        if (activo) setErrorCarga(err.handledMessage || 'No pudimos cargar el consentimiento.')
      }
    })()
    return () => { activo = false }
  }, [])

  const aceptar = async () => {
    setError('')
    setCargando(true)
    try {
      await aceptarConsentimiento()
      await refreshOnboarding()
      navigate('/onboarding/encuesta-inicial', { replace: true })
    } catch (err) {
      setError(err.handledMessage || 'No pudimos registrar tu consentimiento. Intenta de nuevo.')
    } finally {
      setCargando(false)
    }
  }

  const rechazar = async () => {
    setError('')
    setRechazando(true)
    try {
      // El rechazo queda registrado con trazabilidad; luego se cierra la sesión.
      await rechazarConsentimiento()
    } catch {
      /* aunque el registro del rechazo falle, la sesión se cierra igual */
    } finally {
      await logout()
      navigate('/login', { replace: true })
    }
  }

  if (errorCarga) {
    return (
      <section className="max-w-2xl mx-auto">
        <Aviso tipo="error">{errorCarga}</Aviso>
      </section>
    )
  }

  if (!documento) return <PantallaCarga />

  return (
    <section className="max-w-2xl mx-auto">
      <div className="bg-white rounded-3xl shadow-card p-6 sm:p-8">
        <div className="flex items-center justify-between gap-3">
          <p className="text-xs font-semibold uppercase tracking-wide text-leaf-700">Paso 1 de 2</p>
          <span className="rounded-full bg-leaf-50 px-2.5 py-0.5 text-[11px] font-semibold text-leaf-700">
            Versión {documento.version}
          </span>
        </div>
        <h1 className="mt-2 font-display text-2xl font-semibold text-leaf-900">Consentimiento informado</h1>
        <p className="mt-1 text-sm text-stone-500">
          Lee el documento completo (puedes desplazarte dentro del recuadro) y decide abajo.
        </p>

        <div
          className="mt-5 max-h-[420px] overflow-y-auto rounded-2xl border border-stone-200 bg-stone-50 p-4 sm:p-5"
          aria-label="Texto del consentimiento informado"
        >
          <div className="space-y-3 text-sm leading-relaxed text-stone-700 whitespace-pre-wrap">
            {documento.texto}
          </div>
        </div>

        {error && <div className="mt-4"><Aviso tipo="error">{error}</Aviso></div>}

        <div className="mt-6 grid gap-3 sm:grid-cols-2">
          <BotonCargando
            type="button"
            onClick={aceptar}
            cargando={cargando}
            disabled={cargando || rechazando}
            className="w-full"
          >
            {cargando ? 'Registrando…' : 'He leído y acepto'}
          </BotonCargando>
          <button
            type="button"
            onClick={rechazar}
            disabled={cargando || rechazando}
            className="w-full rounded-2xl border border-stone-300 bg-white px-4 py-3 text-sm font-medium text-stone-600 transition-colors hover:border-red-300 hover:text-red-600 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {rechazando ? 'Cerrando sesión…' : 'No acepto'}
          </button>
        </div>

        <p className="mt-4 text-center text-[11px] text-stone-400">
          Trazabilidad digital: se registrará la versión {documento.version}, la fecha/hora, tu
          vivienda y la IP. Hash del documento:&nbsp;
          <span className="font-mono break-all">{documento.hashTexto.slice(0, 16)}…</span>
        </p>
      </div>
    </section>
  )
}
