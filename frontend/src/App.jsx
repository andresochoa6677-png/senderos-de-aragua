import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './context/AuthContext.jsx'
import AuthLayout from './layouts/AuthLayout.jsx'
import ResidentLayout from './layouts/ResidentLayout.jsx'
import PantallaCarga from './components/PantallaCarga.jsx'
import Login from './pages/Login.jsx'
import Registro from './pages/Registro.jsx'
import RegistroExitoso from './pages/RegistroExitoso.jsx'
import RecuperarPassword from './pages/RecuperarPassword.jsx'
import RestablecerPassword from './pages/RestablecerPassword.jsx'
import Consentimiento from './pages/Consentimiento.jsx'
import EncuestaInicial from './pages/EncuestaInicial.jsx'
import Dashboard from './pages/Dashboard.jsx'
import { ListaLecciones, DetalleLeccion } from './pages/Aprendizaje.jsx'
import { ListaEjercicios, DetalleEjercicios } from './pages/Ejercicios.jsx'
import Progreso from './pages/Progreso.jsx'
import { PracticaInicio, PracticaModo } from './pages/Practica.jsx'
import VerificarCorreo from './pages/VerificarCorreo.jsx'
import Informacion from './pages/Informacion.jsx'
import EncuestaFinal from './pages/EncuestaFinal.jsx'
import AdminLayout from './layouts/AdminLayout.jsx'
import AdminPanelInicio from './pages/admin/PanelInicio.jsx'
import AdminViviendas from './pages/admin/AdminViviendas.jsx'
import AdminComentarios from './pages/admin/AdminComentarios.jsx'
import AdminSesiones from './pages/admin/AdminSesiones.jsx'
import AdminEstudio from './pages/admin/AdminEstudio.jsx'
import Comentarios from './pages/Comentarios.jsx'

function RequireAuth({ children }) {
  const { user, loading } = useAuth()
  if (loading) return <PantallaCarga />
  if (!user) return <Navigate to="/login" replace />
  return children
}

function RequireAdmin({ children }) {
  const { user, loading } = useAuth()
  if (loading) return <PantallaCarga />
  if (!user) return <Navigate to="/login" replace />
  // Solo el administrador entra; un residente va a su dashboard.
  if (user.rol !== 'ADMIN') return <Navigate to="/dashboard" replace />
  return children
}

function RequireOnboarding({ children }) {
  const { user, loading, estadoOnboarding } = useAuth()
  if (loading) return <PantallaCarga />
  if (!user) return <Navigate to="/login" replace />
  // El administrador no pasa por onboarding: su acceso es el panel.
  if (user.rol === 'ADMIN') return <Navigate to="/admin" replace />
  const siguientePaso = estadoOnboarding?.siguientePaso
  if (!siguientePaso || siguientePaso === 'DASHBOARD') return children
  if (siguientePaso === 'CONSENTIMIENTO') return <Navigate to="/onboarding/consentimiento" replace />
  if (siguientePaso === 'ENCUESTA_INICIAL') return <Navigate to="/onboarding/encuesta-inicial" replace />
  return children
}

export default function App() {
  return (
    <Routes>
      <Route element={<AuthLayout />}>
        <Route path="/login" element={<Login />} />
        <Route path="/registro" element={<Registro />} />
        <Route path="/registro/exitoso" element={<RegistroExitoso />} />
        <Route path="/verificar" element={<VerificarCorreo />} />
        <Route path="/recuperar" element={<RecuperarPassword />} />
        <Route path="/restablecer" element={<RestablecerPassword />} />
      </Route>

      {/* La guarda envuelve al LAYOUT (no solo a cada ruta): así nada del
          panel —ni siquiera la barra de navegación— se muestra mientras
          /api/auth/me resuelve la sesión. Sin esto, el layout parpadea
          vacío antes de redirigir al login. */}
      <Route element={<RequireAuth><ResidentLayout /></RequireAuth>}>
        <Route path="/onboarding/consentimiento" element={
          <RequireAuth><Consentimiento /></RequireAuth>
        } />
        <Route path="/onboarding/encuesta-inicial" element={
          <RequireAuth><EncuestaInicial /></RequireAuth>
        } />
        <Route path="/dashboard" element={
          <RequireOnboarding><Dashboard /></RequireOnboarding>
        } />
        <Route path="/aprendizaje" element={<RequireOnboarding><ListaLecciones /></RequireOnboarding>} />
        <Route path="/aprendizaje/:id" element={<RequireOnboarding><DetalleLeccion /></RequireOnboarding>} />
        <Route path="/ejercicios" element={<RequireOnboarding><ListaEjercicios /></RequireOnboarding>} />
        <Route path="/ejercicios/:id" element={<RequireOnboarding><DetalleEjercicios /></RequireOnboarding>} />
        <Route path="/clasificacion" element={<RequireOnboarding><PracticaInicio /></RequireOnboarding>} />
        <Route path="/clasificacion/:modo" element={<RequireOnboarding><PracticaModo /></RequireOnboarding>} />
        <Route path="/informacion" element={<RequireOnboarding><Informacion /></RequireOnboarding>} />
        <Route path="/comentarios" element={<RequireOnboarding><Comentarios /></RequireOnboarding>} />
        <Route path="/encuesta-final" element={<RequireOnboarding><EncuestaFinal /></RequireOnboarding>} />
        <Route path="/progreso" element={<RequireOnboarding><Progreso /></RequireOnboarding>} />
      </Route>

      <Route element={<RequireAdmin><AdminLayout /></RequireAdmin>}>
        <Route path="/admin" element={<RequireAdmin><AdminPanelInicio /></RequireAdmin>} />
        <Route path="/admin/viviendas" element={<RequireAdmin><AdminViviendas /></RequireAdmin>} />
        <Route path="/admin/comentarios" element={<RequireAdmin><AdminComentarios /></RequireAdmin>} />
        <Route path="/admin/sesiones" element={<RequireAdmin><AdminSesiones /></RequireAdmin>} />
        <Route path="/admin/estudio" element={<RequireAdmin><AdminEstudio /></RequireAdmin>} />
      </Route>

      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}
