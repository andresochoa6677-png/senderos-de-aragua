import { useEffect, useRef } from 'react'
import { NavLink, Outlet, useNavigate, useLocation } from 'react-router-dom'
import { Recycle, LayoutDashboard, BookOpen, Dumbbell, Trash2, Info, MessageSquare, ClipboardList, BarChart3, LogOut } from 'lucide-react'
import { useAuth } from '../context/AuthContext.jsx'

const NAV = [
  { to: '/dashboard', label: 'Inicio', icon: LayoutDashboard, end: true },
  { to: '/aprendizaje', label: 'Aprendizaje', icon: BookOpen },
  { to: '/ejercicios', label: 'Ejercicios', icon: Dumbbell },
  { to: '/clasificacion', label: 'Clasificación', icon: Trash2 },
  { to: '/progreso', label: 'Progreso', icon: BarChart3 },
  { to: '/informacion', label: 'Información', icon: Info },
  { to: '/comentarios', label: 'Comentarios', icon: MessageSquare },
  { to: '/encuesta-final', label: 'Encuesta final', icon: ClipboardList }
]

const claseNavLink = ({ isActive }) =>
  `flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-colors ${
    isActive ? 'bg-leaf-600 text-white shadow-card' : 'text-leaf-900/80 hover:bg-leaf-100'
  }`

export default function ResidentLayout() {
  const { user, cerrarSesion } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const navListaRef = useRef(null)

  // En móvil, centra automáticamente la sección activa de la barra de
  // navegación horizontal al cambiar de página (el usuario no debe tener
  // que adivinar dónde está el ítem resaltado).
  useEffect(() => {
    const lista = navListaRef.current
    if (!lista || window.innerWidth >= 1024) return
    const activo = lista.querySelector('[aria-current="page"]')
    if (activo) {
      lista.scrollLeft = activo.offsetLeft - (lista.clientWidth - activo.clientWidth) / 2
    }
  }, [location.pathname])

  const salir = async () => {
    await cerrarSesion()
    navigate('/login')
  }

  return (
    <div className="min-h-screen bg-stone-50 flex flex-col">
      <header className="sticky top-0 z-20 bg-white/90 backdrop-blur border-b border-leaf-100">
        <div className="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between gap-3">
          <div className="flex items-center gap-2.5 min-w-0">
            <span className="h-9 w-9 rounded-xl bg-leaf-600 text-white grid place-items-center shrink-0">
              <Recycle size={20} aria-hidden="true" />
            </span>
            <div className="min-w-0">
              <p className="font-display font-semibold text-leaf-900 leading-tight truncate">Senderos de Aragua</p>
              <p className="text-xs text-stone-500 truncate">
                {user ? `Vivienda ${user.viviendaCodigo || '—'}` : ''}
              </p>
            </div>
          </div>
          <button
            onClick={salir}
            className="inline-flex items-center gap-2 px-3 py-2 rounded-xl text-sm font-medium text-leaf-900/80 hover:bg-leaf-100 transition-colors"
          >
            <LogOut size={16} aria-hidden="true" />
            <span className="hidden sm:inline">Salir</span>
          </button>
        </div>
      </header>

      <div className="flex-1 max-w-6xl w-full mx-auto px-4 py-6 lg:grid lg:grid-cols-[240px_1fr] lg:gap-8">
        <nav aria-label="Navegación principal" className="relative lg:sticky lg:top-24 mb-4 lg:mb-0">
          {/* Indicador visual de que hay más secciones hacia la derecha (solo móvil) */}
          <span
            aria-hidden="true"
            className="pointer-events-none absolute inset-y-0 right-0 z-10 hidden w-8 bg-gradient-to-l from-stone-50 to-transparent sm:block lg:hidden"
          />
          <ul
            ref={navListaRef}
            className="flex snap-x snap-mandatory gap-2 overflow-x-auto pb-1 lg:flex-col lg:pb-0 [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
          >
            {NAV.map(({ to, label, icon: Icon, end }) => (
              <li key={to} className="shrink-0 snap-start">
                <NavLink to={to} end={end} className={claseNavLink}>
                  <Icon size={18} aria-hidden="true" />
                  <span className="whitespace-nowrap">{label}</span>
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
        <main id="contenido" className="min-w-0">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
