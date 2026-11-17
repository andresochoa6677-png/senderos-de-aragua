import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { Home, KeyRound, Users } from 'lucide-react'

/**
 * Confirmación posterior al registro. La cuenta nace activa (sin paso de
 * verificación por correo): esta pantalla solo se alcanza si el inicio de
 * sesión automático no llegó a establecerse — el residente continúa
 * iniciando sesión normalmente.
 */
export default function RegistroExitoso() {
  const navigate = useNavigate()
  const location = useLocation()
  const vivienda = location.state?.vivienda
  const email = location.state?.email

  if (!vivienda) {
    return <Navigate to="/registro" replace />
  }

  return (
    <section className="animate-fade-up text-center">
      <div className="mx-auto grid h-16 w-16 place-items-center rounded-full bg-leaf-100 animate-scale-in">
        <Home className="h-8 w-8 text-leaf-700" aria-hidden="true" />
      </div>

      <h1 className="mt-5 font-display text-2xl font-semibold tracking-tight text-leaf-900 sm:text-[1.7rem]">
        ¡Tu cuenta quedó creada!
      </h1>
      <p className="mt-1.5 text-sm leading-relaxed text-stone-500">
        Solo falta iniciar sesión para empezar.
      </p>

      <div className="mt-6 flex items-center gap-4 rounded-2xl border border-leaf-200 bg-leaf-50/70 px-5 py-4 text-left">
        <span className="grid h-11 w-11 shrink-0 place-items-center rounded-xl bg-leaf-600 text-white shadow-sm">
          <Home size={22} aria-hidden="true" />
        </span>
        <div>
          <p className="text-xs font-medium uppercase tracking-wider text-stone-500">Vivienda registrada</p>
          <p className="font-display text-xl font-semibold tracking-tight text-leaf-900">{vivienda}</p>
        </div>
      </div>

      <div className="mt-5 text-left">
        <div className="rounded-2xl bg-stone-50 px-4 py-3 text-sm text-stone-600">
          Inicia sesión con el correo <span className="font-semibold break-words">{email}</span> y la
          contraseña que creaste. Guárdalos: también sirven para recuperar tu cuenta si olvidas la contraseña.
        </div>
      </div>

      <div className="mt-6">
        <button
          onClick={() => navigate('/login', { state: { registrado: true, email } })}
          className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-leaf-600 px-4 py-3 text-sm font-semibold text-white shadow-card transition-colors hover:bg-leaf-700"
        >
          <KeyRound size={16} aria-hidden="true" />
          Iniciar sesión
        </button>
      </div>

      <p className="mt-5 flex items-center justify-center gap-1.5 text-xs text-stone-400">
        <Users size={13} aria-hidden="true" />
        Recuerda: las cuentas de esta plataforma son una por vivienda.
      </p>
    </section>
  )
}
