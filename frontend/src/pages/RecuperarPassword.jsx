import { useState } from 'react'
import { Link } from 'react-router-dom'
import { solicitarReset } from '../api/client.js'
import { CampoTexto, BotonCargando, Aviso } from '../components/ui.jsx'

export default function RecuperarPassword() {
  const [email, setEmail] = useState('')
  const [error, setError] = useState('')
  const [exito, setExito] = useState(null)
  const [cargando, setCargando] = useState(false)

  const enviar = async (e) => {
    e.preventDefault()
    setError('')
    setExito(null)
    if (!email.trim()) {
      setError('Ingresa tu correo para continuar.')
      return
    }
    setCargando(true)
    try {
      await solicitarReset(email.trim().toLowerCase())
      setExito({ mensaje: 'Si el correo está registrado, recibirás instrucciones.' })
    } catch (err) {
      setError(err.handledMessage || 'No pudimos procesar la solicitud. Intenta de nuevo.')
    } finally {
      setCargando(false)
    }
  }

  return (
    <section className="animate-fade-up">
      <h1 className="font-display text-2xl font-semibold text-leaf-900">Recuperar contraseña</h1>
      <p className="mt-1 text-sm text-stone-500">
        Ingresa el correo de tu vivienda y te enviaremos instrucciones para crear una nueva contraseña.
      </p>

      {error && <div className="mt-4"><Aviso tipo="error">{error}</Aviso></div>}
      {exito && (
        <div className="mt-4 space-y-2">
          <Aviso tipo="exito">{exito.mensaje}</Aviso>
        </div>
      )}

      <form onSubmit={enviar} className="mt-6 space-y-4" noValidate>
        <CampoTexto
          id="rec-email"
          etiqueta="Correo electrónico"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <BotonCargando type="submit" cargando={cargando} className="w-full">
          {cargando ? 'Enviando…' : 'Enviar instrucciones'}
        </BotonCargando>
      </form>

      <p className="mt-6 text-sm text-stone-500 text-center">
        <Link to="/login" className="font-medium text-leaf-700 underline underline-offset-2 hover:text-leaf-800">
          ← Volver a iniciar sesión
        </Link>
      </p>
    </section>
  )
}
