import { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { Mail, ShieldCheck, Send } from 'lucide-react'
import { useAuth } from '../context/AuthContext.jsx'
import { reenviarVerificacion } from '../api/client.js'
import { CampoTexto, CampoPassword, Casilla, BotonCargando, Aviso } from '../components/ui.jsx'

export default function Login() {
  const { iniciarSesion } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState(location.state?.email || '')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)
  const [recordar, setRecordar] = useState(true) // mantener sesión activa
  const [pendiente, setPendiente] = useState(null) // email esperando verificación
  const [avisoReenvio, setAvisoReenvio] = useState('')

  const destino = location.state?.desde || '/dashboard'
  const registrado = Boolean(location.state?.registrado)

  const enviar = async (e) => {
    e.preventDefault()
    setError('')
    setAvisoReenvio('')
    if (!email || !password) {
      setError('Ingresa tu correo y contraseña para continuar.')
      return
    }
    setCargando(true)
    try {
      const usuario = await iniciarSesion(email.trim(), password, recordar)
      navigate(usuario?.rol === 'ADMIN' ? '/admin' : destino, { replace: true })
    } catch (err) {
      if (err.codigo === 'PENDIENTE_VERIFICACION') {
        setPendiente(email.trim())
        setError(err.handledMessage)
      } else {
        setPendiente(null)
        setError(err.handledMessage || 'No pudimos iniciar sesión. Verifica tus datos.')
      }
    } finally {
      setCargando(false)
    }
  }

  const reenviar = async () => {
    if (!pendiente) return
    setAvisoReenvio('')
    try {
      const r = await reenviarVerificacion(pendiente)
      setAvisoReenvio(r.mensaje)
    } catch (err) {
      setAvisoReenvio(err.handledMessage || 'No pudimos reenviar el correo. Espera unos minutos.')
    }
  }

  return (
    <section className="animate-fade-up">
      <h1 className="font-display text-2xl font-semibold tracking-tight text-leaf-900 sm:text-[1.7rem]">
        Bienvenido de nuevo
      </h1>
      <p className="mt-1.5 text-sm leading-relaxed text-stone-500">
        Ingresa con el correo de tu vivienda para continuar.
      </p>

      {registrado && (
        <div className="mt-5">
          <Aviso tipo="exito">¡Cuenta creada correctamente! Verifica tu correo e inicia sesión.</Aviso>
        </div>
      )}

      {pendiente && (
        <div className="mt-5 rounded-2xl border border-amber-200 bg-amber-50 p-4">
          <p className="text-sm font-medium text-amber-800">
            Tu cuenta espera verificación de correo.
          </p>
          <button
            onClick={reenviar}
            className="mt-2.5 inline-flex items-center gap-2 rounded-xl border border-amber-300 bg-white px-3.5 py-2 text-sm font-semibold text-amber-800 hover:bg-amber-100"
          >
            <Send size={14} aria-hidden="true" />
            Reenviar correo de verificación
          </button>
          {avisoReenvio && <p className="mt-2 text-xs text-amber-800">{avisoReenvio}</p>}
        </div>
      )}

      {error && <div className="mt-5"><Aviso tipo="error">{error}</Aviso></div>}

      <form onSubmit={enviar} className="mt-7 space-y-5" noValidate>
        <CampoTexto
          id="login-email"
          etiqueta="Correo electrónico"
          type="email"
          autoComplete="email"
          icono={<Mail />}
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="tu@correo.com"
          required
        />
        <div>
          <CampoPassword
            id="login-password"
            etiqueta="Contraseña"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
            required
          />
          <div className="mt-2 text-right">
            <Link
              to="/recuperar"
              className="text-sm font-medium text-leaf-700 underline-offset-2 hover:text-leaf-800 hover:underline"
            >
              ¿Olvidaste tu contraseña?
            </Link>
          </div>
        </div>
        <Casilla id="login-recordar" checked={recordar} onChange={(e) => setRecordar(e.target.checked)}>
          Mantener la sesión activa en este equipo: al volver, entras directo
          sin escribir tu contraseña.
        </Casilla>
        <BotonCargando type="submit" cargando={cargando} className="w-full">
          {cargando ? 'Ingresando…' : 'Iniciar sesión'}
        </BotonCargando>
      </form>

      <div className="my-7 flex items-center gap-3" aria-hidden="true">
        <span className="h-px flex-1 bg-stone-200" />
        <span className="text-xs font-medium uppercase tracking-wider text-stone-400">o</span>
        <span className="h-px flex-1 bg-stone-200" />
      </div>

      <div className="rounded-2xl border border-leaf-100 bg-leaf-50/60 p-4">
        <p className="flex items-start gap-2.5 text-sm text-stone-600">
          <ShieldCheck size={18} className="mt-0.5 shrink-0 text-leaf-600" aria-hidden="true" />
          <span>
            <span className="font-medium text-leaf-900">Una cuenta por vivienda.</span>{' '}
            Si tu vivienda ya está registrada, inicia sesión con esa cuenta.
          </span>
        </p>
        <Link to="/registro" className="mt-3.5 block">
          <BotonCargando variante="secundario" className="w-full">
            Crear cuenta
          </BotonCargando>
        </Link>
      </div>
    </section>
  )
}
