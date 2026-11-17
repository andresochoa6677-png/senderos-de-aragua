import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { Recycle, LayoutDashboard, Home, MessageSquare, Activity, Settings, LogOut } from 'lucide-react'
import { useAuth } from '../context/AuthContext.jsx'

const NAV = [
  { to: '/admin', label: 'Panel', icon: LayoutDashboard, end: true },
  { to: '/admin/viviendas', label: 'Viviendas', icon: Home },
  { to: '/admin/comentarios', label: 'Comentarios', icon: MessageSquare },
  { to: '/admin/sesiones', label: 'Sesiones', icon: Activity },
  { to: '/admin/estudio', label: 'Estudio', icon: Settings }
]

const claseNavLink = ({ isActive }) =>
  `flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-colors ${
    isActive ? 'bg-leaf-600 text-white shadow-card' : 'text-leaf-900/80 hover:bg-leaf-100'
  }`

export default function AdminLayout() {
  const { cerrarSesion } = useAuth()
  const navigate = useNavigate()

  const salir = async () => {
    await cerrarSesion()
    navigate('/login')
  }

  return (
    <div className="min-h-screen bg-stone-50 flex flex-col">
      <header className="sticky top-0 z-20 bg-white/90 backdrop-blur border-b border-leaf-100">
        <div className="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between gap-3">
          <div className="flex items-center gap-2.5 min-w-0">
            <span className="h-9 w-9 rounded-xl bg-leaf-800 text-white grid place-items-center shrink-0">
              <Recycle size={20} aria-hidden="true" />
            </span>
            <div className="min-w-0">
              <p className="font-display font-semibold text-leaf-900 leading-tight truncate">Senderos de Aragua</p>
              <p className="text-xs text-stone-500 truncate">Panel de administración</p>
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
        <nav aria-label="Navegación de administración" className="mb-4 lg:mb-0 lg:sticky lg:top-24">
          <ul className="flex gap-2 overflow-x-auto pb-1 lg:flex-col lg:pb-0 [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
            {NAV.map(({ to, label, icon: Icon, end }) => (
              <li key={to} className="shrink-0">
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
