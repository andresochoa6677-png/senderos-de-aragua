import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ArrowRight, MailX, ShieldCheck } from 'lucide-react'
import { verificarCorreo } from '../api/client.js'
import { Aviso } from '../components/ui.jsx'
import PantallaCarga from '../components/PantallaCarga.jsx'

/**
 * Verificación de correo: el residente llega aquí desde el enlace del
 * mensaje. Estados: verificando, verificado, enlace inválido/expirado.
 */
export default function VerificarCorreo() {
  const [params] = useSearchParams()
  const token = params.get('token')
  const [estado, setEstado] = useState('verificando') // verificando | ok | error
  const [mensaje, setMensaje] = useState('')

  useEffect(() => {
    let activo = true
    if (!token) {
      setEstado('error')
      setMensaje('Falta el token de verificación. Abre el enlace completo que recibiste por correo.')
      return
    }
    verificarCorreo(token)
      .then((r) => { if (activo) { setEstado('ok'); setMensaje(r.mensaje) } })
      .catch((err) => { if (activo) { setEstado('error'); setMensaje(err.handledMessage || 'No pudimos verificar tu correo.') } })
    return () => { activo = false }
  }, [token])

  if (estado === 'verificando') return <PantallaCarga />

  return (
    <section className="animate-fade-up text-center">
      {estado === 'ok' ? (
        <>
          <div className="mx-auto grid h-16 w-16 place-items-center rounded-full bg-leaf-100 animate-scale-in">
            <ShieldCheck className="h-8 w-8 text-leaf-700" aria-hidden="true" />
          </div>
          <h1 className="mt-5 font-display text-2xl font-semibold tracking-tight text-leaf-900 sm:text-[1.7rem]">
            ¡Correo verificado!
          </h1>
          <p className="mt-1.5 text-sm leading-relaxed text-stone-500">{mensaje}</p>
          <div className="mt-7">
            <Link
              to="/login"
              className="inline-flex w-full items-center justify-center gap-2 rounded-2xl bg-leaf-700 px-4 py-3 text-sm font-semibold text-white transition-all hover:-translate-y-px hover:bg-leaf-800"
            >
              Iniciar sesión <ArrowRight size={15} aria-hidden="true" />
            </Link>
          </div>
        </>
      ) : (
        <>
          <div className="mx-auto grid h-16 w-16 place-items-center rounded-full bg-red-100">
            <MailX className="h-8 w-8 text-red-600" aria-hidden="true" />
          </div>
          <h1 className="mt-5 font-display text-2xl font-semibold tracking-tight text-leaf-900 sm:text-[1.7rem]">
            No pudimos verificar tu correo
          </h1>
          <div className="mt-4 text-left">
            <Aviso tipo="error">{mensaje}</Aviso>
          </div>
      <p className="mt-4 text-sm leading-relaxed break-words text-stone-500">
        Si el enlace expiró, inicia sesión con tu correo y solicita un nuevo
        enlace de verificación desde la pantalla de acceso.
      </p>
          <div className="mt-6">
            <Link
              to="/login"
              className="inline-flex w-full items-center justify-center gap-2 rounded-2xl bg-leaf-700 px-4 py-3 text-sm font-semibold text-white transition-all hover:-translate-y-px hover:bg-leaf-800"
            >
              Ir a iniciar sesión
            </Link>
          </div>
        </>
      )}
    </section>
  )
}
