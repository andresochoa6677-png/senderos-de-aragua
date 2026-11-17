import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { restablecerPassword } from '../api/client.js'
import { CampoTexto, BotonCargando, Aviso } from '../components/ui.jsx'

// Política NIST adaptada (A-3): mínimo 10; la lista de contraseñas comunes
// la aplica el backend y su mensaje llega en detalles.nuevaPassword.
const MIN_PASSWORD = 10

export default function RestablecerPassword() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const token = params.get('token') || ''
  const [password, setPassword] = useState('')
  const [confirmacion, setConfirmacion] = useState('')
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)
  const [listo, setListo] = useState(false)

  const enviar = async (e) => {
    e.preventDefault()
    setError('')
    if (password.length < MIN_PASSWORD) {
      setError(`La contraseña debe tener al menos ${MIN_PASSWORD} caracteres.`)
      return
    }
    if (password !== confirmacion) {
      setError('Las contraseñas no coinciden.')
      return
    }
    setCargando(true)
    try {
      await restablecerPassword(token, password)
      setListo(true)
      setTimeout(() => navigate('/login', { replace: true }), 3000)
    } catch (err) {
      const detallePassword = err.response?.data?.detalles?.nuevaPassword
      setError(detallePassword || err.handledMessage || 'No pudimos actualizar la contraseña. El enlace puede haber expirado.')
    } finally {
      setCargando(false)
    }
  }

  if (!token) {
    return (
      <section className="animate-fade-up text-center">
        <h1 className="font-display text-2xl font-semibold text-leaf-900">Enlace no válido</h1>
        <p className="mt-2 text-sm text-stone-500">
          El enlace de recuperación no incluye un token. Solicita uno nuevo desde la página de recuperación.
        </p>
        <p className="mt-6 text-sm">
          <Link to="/recuperar" className="font-medium text-leaf-700 underline underline-offset-2 hover:text-leaf-800">
            Solicitar nuevo enlace
          </Link>
        </p>
      </section>
    )
  }

  return (
    <section className="animate-fade-up">
      <h1 className="font-display text-2xl font-semibold text-leaf-900">Nueva contraseña</h1>
      <p className="mt-1 text-sm text-stone-500">Crea una contraseña nueva para tu cuenta.</p>

      {error && <div className="mt-4"><Aviso tipo="error">{error}</Aviso></div>}
      {listo && (
        <div className="mt-4">
          <Aviso tipo="exito">Contraseña actualizada. Te llevamos al inicio de sesión…</Aviso>
        </div>
      )}

      {!listo && (
        <form onSubmit={enviar} className="mt-6 space-y-4" noValidate>
          <CampoTexto
            id="nueva-password"
            etiqueta="Nueva contraseña"
            type="password"
            autoComplete="new-password"
            hint={`Mínimo ${MIN_PASSWORD} caracteres.`}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
          <CampoTexto
            id="nueva-confirm"
            etiqueta="Confirmar nueva contraseña"
            type="password"
            autoComplete="new-password"
            value={confirmacion}
            onChange={(e) => setConfirmacion(e.target.value)}
            required
          />
          <BotonCargando type="submit" cargando={cargando} className="w-full">
            {cargando ? 'Guardando…' : 'Guardar contraseña'}
          </BotonCargando>
        </form>
      )}

      <p className="mt-6 text-sm text-stone-500 text-center">
        <Link to="/login" className="font-medium text-leaf-700 underline underline-offset-2 hover:text-leaf-800">
          ← Volver a iniciar sesión
        </Link>
      </p>
    </section>
  )
}
