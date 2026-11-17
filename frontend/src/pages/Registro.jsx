import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Mail, User, Home } from 'lucide-react'
import { registroVivienda, login } from '../api/client.js'
import { BLOQUES, numerosDeBloque } from '../data/viviendas.js'
import { CampoTexto, CampoPassword, Casilla, BotonCargando, Aviso } from '../components/ui.jsx'

// Política NIST adaptada (A-3): mínimo 10 y sin contraseñas comunes;
// la lista de denegación la aplica el backend, el mensaje concreto
// (detalles.password) se muestra en el catch del envío.
const MIN_PASSWORD = 10

/** Indicador simple de fortaleza: longitud + variedad de caracteres. */
function fortaleza(password) {
  let puntos = 0
  if (password.length >= MIN_PASSWORD) puntos++
  if (password.length >= 12) puntos++
  if (/[a-z]/.test(password) && /[A-Z]/.test(password)) puntos++
  if (/\d/.test(password) || /[^A-Za-z0-9]/.test(password)) puntos++
  return Math.min(puntos, 3) // 0 débil · 1 media · 2-3 fuerte
}

const NIVELES = [
  { etiqueta: 'Débil', color: 'bg-red-400', ancho: 'w-1/3' },
  { etiqueta: 'Aceptable', color: 'bg-amber-400', ancho: 'w-2/3' },
  { etiqueta: 'Fuerte', color: 'bg-leaf-500', ancho: 'w-full' },
  { etiqueta: 'Fuerte', color: 'bg-leaf-500', ancho: 'w-full' }
]

export default function Registro() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ nombre: '', email: '', password: '', confirmacion: '', acepta: false, bloque: '', numero: '' })
  const [errores, setErrores] = useState({})
  const [errorApi, setErrorApi] = useState('')
  const [cargando, setCargando] = useState(false)

  const nivel = useMemo(() => fortaleza(form.password), [form.password])
  const nivelActual = NIVELES[nivel]

  const actualizar = (campo) => (e) =>
    setForm((f) => ({ ...f, [campo]: e.target.type === 'checkbox' ? e.target.checked : e.target.value }))

  /** Al cambiar de bloque se reinicia el número: cada bloque tiene su propio listado. */
  const elegirBloque = (e) =>
    setForm((f) => ({ ...f, bloque: e.target.value, numero: '' }))

  const validar = () => {
    const errs = {}
    if (!form.nombre.trim()) errs.nombre = 'Ingresa el nombre de tu familia o el tuyo.'
    else if (form.nombre.trim().length > 80) errs.nombre = 'Máximo 80 caracteres.'
    if (!form.email.trim()) errs.email = 'Ingresa tu correo.'
    else if (!/^\S+@\S+\.\S+$/.test(form.email)) errs.email = 'Ingresa un correo válido.'
    if (form.password.length < MIN_PASSWORD) errs.password = `La contraseña debe tener al menos ${MIN_PASSWORD} caracteres.`
    if (form.password !== form.confirmacion) errs.confirmacion = 'Las contraseñas no coinciden.'
    if (!form.acepta) errs.acepta = 'Debes aceptar el tratamiento de datos para continuar.'
    if (!form.bloque) errs.bloque = 'Escoge el bloque de tu casa.'
    if (!form.numero) errs.numero = 'Escoge el número de tu casa dentro del bloque.'
    return errs
  }

  const enviar = async (e) => {
    e.preventDefault()
    setErrorApi('')
    const errs = validar()
    setErrores(errs)
    if (Object.keys(errs).length > 0) return
    setCargando(true)
    try {
      const cuenta = await registroVivienda({
        nombre: form.nombre.trim(),
        email: form.email.trim().toLowerCase(),
        password: form.password,
        aceptaTratamientoDatos: form.acepta,
        bloque: form.bloque,
        numero: Number(form.numero)
      })
      // La cuenta nace activa: iniciamos sesión de inmediato para que el
      // residente continúe con el consentimiento sin paso extra.
      try {
        await login(form.email.trim().toLowerCase(), form.password, true)
        navigate('/onboarding/consentimiento', { replace: true })
      } catch {
        // Fallback: sesión no establecida (p. ej. límite de intentos) —
        // el residente continúa desde la pantalla de inicio de sesión.
        navigate('/registro/exitoso', {
          state: {
            vivienda: cuenta.usuario?.viviendaCodigo,
            email: form.email.trim().toLowerCase()
          }
        })
      }
    } catch (err) {
      const detallePassword = err.response?.data?.detalles?.password
      setErrorApi(detallePassword || err.handledMessage || 'No pudimos crear la cuenta. Intenta de nuevo.')
      setCargando(false)
    }
  }

  return (
    <section className="animate-fade-up">
      <h1 className="font-display text-2xl font-semibold tracking-tight text-leaf-900 sm:text-[1.7rem]">
        Crea la cuenta de tu vivienda
      </h1>
      <p className="mt-1.5 text-sm leading-relaxed text-stone-500">
        Escoge el bloque y el número de tu casa según el listado oficial
        del condominio (por ejemplo A-1, G-5 o Ñ-2).
      </p>

      {errorApi && <div className="mt-5"><Aviso tipo="error">{errorApi}</Aviso></div>}

      <div className="mt-6 flex items-start gap-3 rounded-2xl border border-leaf-100 bg-leaf-50/60 p-4">
        <Home size={18} className="mt-0.5 shrink-0 text-leaf-600" aria-hidden="true" />
        <p className="text-sm leading-relaxed text-stone-600">
          <span className="font-medium text-leaf-900">Una cuenta por vivienda.</span>{' '}
          Registras la cuenta de tu hogar; luego, cada integrante puede usarla para aprender.
        </p>
      </div>

      <form onSubmit={enviar} className="mt-6 space-y-5" noValidate>
        <CampoTexto
          id="reg-nombre"
          etiqueta="Nombre completo"
          type="text"
          autoComplete="name"
          icono={<User />}
          value={form.nombre}
          onChange={actualizar('nombre')}
          error={errores.nombre}
          placeholder="Ej.: Familia Pérez"
          required
        />
        <CampoTexto
          id="reg-email"
          etiqueta="Correo electrónico"
          type="email"
          autoComplete="email"
          icono={<Mail />}
          value={form.email}
          onChange={actualizar('email')}
          error={errores.email}
          placeholder="tu@correo.com"
          required
        />
        <div>
          <CampoPassword
            id="reg-password"
            etiqueta="Contraseña"
            autoComplete="new-password"
            value={form.password}
            onChange={actualizar('password')}
            error={errores.password}
            placeholder="Mínimo 10 caracteres"
            required
          />
          {form.password.length > 0 && !errores.password && (
            <div className="mt-2 flex items-center gap-2.5" aria-live="polite">
              <span className="h-1.5 w-28 overflow-hidden rounded-full bg-stone-200">
                <span
                  className={`block h-full rounded-full transition-all duration-300 ${nivelActual.color} ${nivelActual.ancho}`}
                />
              </span>
              <span className="text-xs font-medium text-stone-500">{nivelActual.etiqueta}</span>
            </div>
          )}
        </div>
        <CampoPassword
          id="reg-confirm"
          etiqueta="Confirmar contraseña"
          autoComplete="new-password"
          value={form.confirmacion}
          onChange={actualizar('confirmacion')}
          error={errores.confirmacion}
          placeholder="Repite tu contraseña"
          required
        />
        <fieldset className="space-y-3">
          <legend className="text-sm font-medium text-stone-700">
            Tu vivienda <span className="text-red-600" aria-hidden="true">*</span>
          </legend>
          <div className="grid gap-3 sm:grid-cols-2">
            <div>
              <label htmlFor="reg-bloque" className="mb-1.5 block text-xs font-medium text-stone-500">
                Bloque o manzana
              </label>
              <select
                id="reg-bloque"
                value={form.bloque}
                onChange={elegirBloque}
                aria-invalid={!!errores.bloque}
                className={`w-full rounded-2xl border bg-white px-4 py-3 text-sm text-stone-800 transition-colors focus:outline-none focus:ring-2 focus:ring-leaf-600 ${
                  errores.bloque ? 'border-red-400' : 'border-stone-300 focus:border-leaf-600'
                }`}
                required
              >
                <option value="" disabled>Escoge el bloque…</option>
                {BLOQUES.map(({ bloque }) => (
                  <option key={bloque} value={bloque}>{bloque}</option>
                ))}
              </select>
              {errores.bloque && <p className="mt-1 text-xs text-red-600">{errores.bloque}</p>}
            </div>
            <div>
              <label htmlFor="reg-numero" className="mb-1.5 block text-xs font-medium text-stone-500">
                Número de la casa
              </label>
              <select
                id="reg-numero"
                value={form.numero}
                onChange={actualizar('numero')}
                disabled={!form.bloque}
                aria-invalid={!!errores.numero}
                className={`w-full rounded-2xl border bg-white px-4 py-3 text-sm text-stone-800 transition-colors focus:outline-none focus:ring-2 focus:ring-leaf-600 disabled:cursor-not-allowed disabled:bg-stone-100 disabled:text-stone-400 ${
                  errores.numero ? 'border-red-400' : 'border-stone-300 focus:border-leaf-600'
                }`}
                required
              >
                <option value="" disabled>
                  {form.bloque ? 'Escoge el número…' : 'Primero escoge el bloque'}
                </option>
                {numerosDeBloque(form.bloque).map((n) => (
                  <option key={n} value={n}>{n}</option>
                ))}
              </select>
              {errores.numero && <p className="mt-1 text-xs text-red-600">{errores.numero}</p>}
            </div>
          </div>
          {form.bloque && form.numero && (
            <p className="rounded-2xl bg-leaf-50 px-4 py-2.5 text-sm text-leaf-900" aria-live="polite">
              Tu vivienda será <span className="font-semibold">{form.bloque}-{form.numero}</span>.
              Si ya tiene cuenta, el sistema te lo indicará.
            </p>
          )}
        </fieldset>
        <Casilla id="reg-acepta" checked={form.acepta} onChange={actualizar('acepta')} error={errores.acepta}>
          Acepto el tratamiento de mis datos personales para participar en el
          programa educativo del condominio, conforme a su política de privacidad.
        </Casilla>
        <BotonCargando type="submit" cargando={cargando} className="w-full">
          {cargando ? 'Creando tu cuenta…' : 'Crear cuenta'}
        </BotonCargando>
      </form>

      <p className="mt-6 text-center text-sm text-stone-500">
        ¿Tu vivienda ya tiene cuenta?{' '}
        <Link
          to="/login"
          className="font-medium text-leaf-700 underline underline-offset-2 hover:text-leaf-800"
        >
          Inicia sesión
        </Link>
      </p>
    </section>
  )
}
