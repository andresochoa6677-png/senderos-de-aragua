import { useState } from 'react'
import { Eye, EyeOff, Loader2 } from 'lucide-react'

/**
 * Boton primario con estado de carga. Variante "secundario" para
 * acciones de menor peso (p. ej. "Crear cuenta" desde el login).
 */
export function BotonCargando({ cargando, variante = 'primario', children, className = '', ...props }) {
  const variantes = {
    primario: 'bg-leaf-600 text-white hover:bg-leaf-700 focus-visible:ring-leaf-500 shadow-sm',
    secundario: 'bg-white text-leaf-800 border border-leaf-300 hover:bg-leaf-50 focus-visible:ring-leaf-500'
  }
  return (
    <button
      {...props}
      disabled={cargando || props.disabled}
      className={`inline-flex items-center justify-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold transition-all
        hover:-translate-y-px active:translate-y-0 active:scale-[.98]
        disabled:translate-y-0 disabled:opacity-60 disabled:cursor-not-allowed
        ${variantes[variante] || variantes.primario} ${className}`}
    >
      {cargando && <Loader2 size={16} className="animate-spin" aria-hidden="true" />}
      {children}
    </button>
  )
}

export function CampoTexto({ etiqueta, error, hint, id, icono, className = '', ...props }) {
  return (
    <div className={className}>
      {etiqueta && <label htmlFor={id} className="field-label">{etiqueta}</label>}
      <div className="relative">
        {icono && (
          <span className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3.5 text-stone-400 [&>svg]:h-[18px] [&>svg]:w-[18px]">
            {icono}
          </span>
        )}
        <input
          id={id}
          {...props}
          aria-invalid={error ? 'true' : undefined}
          aria-describedby={error ? `${id}-error` : hint ? `${id}-hint` : undefined}
          className={`field-input ${icono ? 'pl-10' : ''} ${error ? 'field-input-error' : ''}`}
        />
      </div>
      {hint && !error && <p id={`${id}-hint`} className="field-hint">{hint}</p>}
      {error && <p id={`${id}-error`} className="field-error-text" role="alert">{error}</p>}
    </div>
  )
}

/** Campo de contraseña con alternancia mostrar/ocultar. */
export function CampoPassword({ etiqueta, error, hint, id, className = '', ...props }) {
  const [visible, setVisible] = useState(false)
  return (
    <div className={className}>
      {etiqueta && <label htmlFor={id} className="field-label">{etiqueta}</label>}
      <div className="relative">
        <input
          id={id}
          type={visible ? 'text' : 'password'}
          {...props}
          aria-invalid={error ? 'true' : undefined}
          aria-describedby={error ? `${id}-error` : hint ? `${id}-hint` : undefined}
          className={`field-input pr-11 ${error ? 'field-input-error' : ''}`}
        />
        <button
          type="button"
          onClick={() => setVisible((v) => !v)}
          aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
          aria-pressed={visible}
          className="absolute inset-y-0 right-0 flex items-center px-3.5 text-stone-400 hover:text-leaf-700 transition-colors"
        >
          {visible ? <EyeOff size={18} aria-hidden="true" /> : <Eye size={18} aria-hidden="true" />}
        </button>
      </div>
      {hint && !error && <p id={`${id}-hint`} className="field-hint">{hint}</p>}
      {error && <p id={`${id}-error`} className="field-error-text" role="alert">{error}</p>}
    </div>
  )
}

/** Casilla de verificacion con estilo propio, para el consentimiento. */
export function Casilla({ id, checked, onChange, error, children }) {
  return (
    <div>
      <label htmlFor={id} className="flex cursor-pointer items-start gap-3 text-sm text-stone-600">
        <span className="relative mt-0.5 grid place-items-center">
          <input
            id={id}
            type="checkbox"
            checked={checked}
            onChange={onChange}
            aria-invalid={error ? 'true' : undefined}
            className="peer h-5 w-5 appearance-none rounded-md border border-leaf-300 bg-white
              checked:border-leaf-600 checked:bg-leaf-600 transition-colors cursor-pointer"
          />
          <svg
            viewBox="0 0 20 20"
            fill="none"
            stroke="white"
            strokeWidth="3"
            strokeLinecap="round"
            strokeLinejoin="round"
            className="pointer-events-none absolute h-3.5 w-3.5 scale-50 opacity-0 transition-all peer-checked:scale-100 peer-checked:opacity-100"
            aria-hidden="true"
          >
            <path d="M4 10.5l4 4 8-9" />
          </svg>
        </span>
        <span>{children}</span>
      </label>
      {error && <p className="field-error-text mt-1.5" role="alert">{error}</p>}
    </div>
  )
}

export function Aviso({ tipo = 'info', children }) {
  const estilos = {
    info: 'bg-leaf-50 border-leaf-200 text-leaf-800',
    error: 'bg-red-50 border-red-200 text-red-700',
    exito: 'bg-emerald-50 border-emerald-200 text-emerald-800'
  }
  return (
    <div role={tipo === 'error' ? 'alert' : 'status'} className={`rounded-xl border px-4 py-3 text-sm ${estilos[tipo]}`}>
      {children}
    </div>
  )
}
