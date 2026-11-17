export default function PantallaCarga({ mensaje = 'Cargando…' }) {
  return (
    <div className="min-h-screen grid place-items-center bg-stone-50" role="status" aria-live="polite">
      <div className="flex flex-col items-center gap-3">
        <div className="h-10 w-10 rounded-full border-4 border-leaf-200 border-t-leaf-600 animate-spin" />
        <p className="text-sm text-stone-500">{mensaje}</p>
      </div>
    </div>
  )
}
