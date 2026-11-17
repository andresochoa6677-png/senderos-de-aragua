import { Recycle, GraduationCap, Home, Users, BarChart3 } from 'lucide-react'
import { Link, Outlet } from 'react-router-dom'

const rasgos = [
  {
    icono: GraduationCap,
    titulo: 'Aprende a separar',
    texto: 'Lecciones breves con imágenes y ejercicios prácticos.'
  },
  {
    icono: BarChart3,
    titulo: 'Mide tu progreso',
    texto: 'Tu vivienda avanza a su propio ritmo, lección a lección.'
  },
  {
    icono: Users,
    titulo: 'Comunidad que participa',
    texto: 'Encuestas y metas pensadas para todo el condominio.'
  }
]

export default function AuthLayout() {
  return (
    <div className="relative min-h-screen overflow-hidden bg-gradient-to-br from-leaf-50 via-white to-earth-50">
      {/* Halos decorativos de fondo */}
      <div aria-hidden="true" className="pointer-events-none absolute -right-24 -top-24 h-96 w-96 rounded-full bg-leaf-200/40 blur-3xl animate-float-slow" />
      <div aria-hidden="true" className="pointer-events-none absolute -bottom-32 -left-20 h-96 w-96 rounded-full bg-earth-200/40 blur-3xl" />

      <div className="relative flex min-h-screen flex-col">
        {/* Encabezado compacto (móvil) */}
        <header className="p-5 sm:p-6 lg:hidden">
          <Marca />
        </header>

        <main className="flex flex-1 items-center justify-center px-4 pb-10 sm:px-6 lg:px-8">
          <div className="grid w-full max-w-5xl overflow-hidden rounded-3xl bg-white shadow-card animate-fade-up lg:grid-cols-[1.05fr_1fr]">
            {/* Panel de marca (desktop) */}
            <aside className="relative hidden flex-col justify-between overflow-hidden bg-gradient-to-br from-leaf-700 via-leaf-800 to-leaf-950 p-10 text-white lg:flex">
              <Recycle
                aria-hidden="true"
                size={260}
                strokeWidth={0.6}
                className="pointer-events-none absolute -bottom-14 -right-14 text-leaf-600/30"
              />
              <div className="relative">
                <span className="inline-flex items-center gap-2.5">
                  <span className="grid h-10 w-10 place-items-center rounded-2xl bg-white/15 ring-1 ring-white/25 backdrop-blur-sm">
                    <Recycle size={22} aria-hidden="true" />
                  </span>
                  <span className="font-display text-lg font-semibold tracking-tight">Senderos de Aragua</span>
                </span>
              </div>

              <div className="relative max-w-sm">
                <h2 className="font-display text-3xl font-semibold leading-tight tracking-tight">
                  Tecnología para una comunidad{' '}
                  <span className="text-leaf-300">más sostenible</span>
                </h2>
                <p className="mt-3 text-sm leading-relaxed text-leaf-100/80">
                  La plataforma educativa de gestión de residuos sólidos del
                  condominio. Una cuenta por vivienda, un propósito común.
                </p>

                <ul className="mt-8 space-y-5">
                  {rasgos.map(({ icono: Icono, titulo, texto }) => (
                    <li key={titulo} className="flex items-start gap-3.5">
                      <span className="mt-0.5 grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-white/10 ring-1 ring-white/20">
                        <Icono size={18} aria-hidden="true" />
                      </span>
                      <span>
                        <span className="block text-sm font-semibold">{titulo}</span>
                        <span className="mt-0.5 block text-xs leading-relaxed text-leaf-100/70">{texto}</span>
                      </span>
                    </li>
                  ))}
                </ul>
              </div>

              <p className="relative flex items-center gap-2 text-xs text-leaf-100/60">
                <Home size={14} aria-hidden="true" />
                Tu vivienda, tu cuenta · Educación ambiental aplicada
              </p>
            </aside>

            {/* Formulario */}
            <section className="p-6 sm:p-10">
              <div className="mb-6 hidden justify-center lg:flex">
                <span className="inline-flex items-center gap-2 rounded-full border border-leaf-200 bg-leaf-50 px-3.5 py-1.5 text-xs font-medium text-leaf-800">
                  <Recycle size={14} aria-hidden="true" />
                  Gestión de residuos sólidos
                </span>
              </div>
              <Outlet />
            </section>
          </div>
        </main>

        <footer className="relative p-4 text-center text-xs text-stone-500">
          Senderos de Aragua · Educación y gestión de residuos sólidos
        </footer>
      </div>
    </div>
  )
}

/** Logotipo de la marca usado en el encabezado móvil. */
function Marca() {
  return (
    <Link to="/login" className="inline-flex items-center gap-2.5 group" aria-label="Senderos de Aragua - Inicio">
      <span className="grid h-10 w-10 place-items-center rounded-2xl bg-leaf-600 text-white shadow-card transition-colors group-hover:bg-leaf-700">
        <Recycle size={22} aria-hidden="true" />
      </span>
      <span className="font-display text-lg font-semibold text-leaf-900">Senderos de Aragua</span>
    </Link>
  )
}
