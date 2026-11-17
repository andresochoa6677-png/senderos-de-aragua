import { Link } from 'react-router-dom'
import { BookOpen, Dumbbell, Trash2, Info, MessageSquare, ClipboardList } from 'lucide-react'
import { useAuth } from '../context/AuthContext.jsx'

const ACCESOS = [
  { to: '/aprendizaje', titulo: 'Aprendizaje', desc: 'Recorrido guiado: completa los ejercicios para avanzar', Icono: BookOpen },
  { to: '/ejercicios', titulo: 'Ejercicios', desc: 'Practica lo aprendido', Icono: Dumbbell },
  { to: '/clasificacion', titulo: 'Clasificación de residuos', desc: 'Consulta qué es reciclable', Icono: Trash2 },
  { to: '/informacion', titulo: 'Información', desc: 'Consulta rápida: colores, preparación y horarios', Icono: Info },
  { to: '/comentarios', titulo: 'Comentarios', desc: 'Comparte tu opinión', Icono: MessageSquare },
  { to: '/encuesta-final', titulo: 'Encuesta final', desc: 'Ayúdanos a medir el impacto', Icono: ClipboardList },
]

export default function Dashboard() {
  const { user } = useAuth()

  return (
    <div className="space-y-8">
      <header>
        <p className="text-sm text-stone-500">¡Hola{user?.nombre ? `, ${user.nombre}` : ''}!</p>
        <h1 className="mt-1 font-display text-3xl font-semibold text-leaf-900">
          Tu vivienda{user?.viviendaCodigo ? ` ${user.viviendaCodigo}` : ''} ya participa
        </h1>
        <p className="mt-2 max-w-2xl text-sm text-stone-600">
          Completaste el registro, el consentimiento y la encuesta inicial. Ahora puedes aprender,
          practicar y consultar cómo separar mejor los residuos del condominio.
        </p>
      </header>

      <section aria-labelledby="dash-accesos">
        <h2 id="dash-accesos" className="text-sm font-semibold uppercase tracking-wide text-stone-500">
          Secciones
        </h2>
        <ul className="mt-3 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {ACCESOS.map(({ to, titulo, desc, Icono }) => (
            <li key={to}>
              <Link
                to={to}
                className="group flex h-full flex-col gap-3 rounded-3xl border border-stone-200 bg-white p-5 shadow-sm transition-all hover:border-leaf-500 hover:shadow-card focus:outline-none focus-visible:ring-2 focus-visible:ring-leaf-600"
              >
                <span className="grid h-11 w-11 place-items-center rounded-2xl bg-leaf-100 text-leaf-700 transition-colors group-hover:bg-leaf-600 group-hover:text-white">
                  <Icono size={22} aria-hidden="true" />
                </span>
                <span>
                  <span className="block font-semibold text-stone-800">{titulo}</span>
                  <span className="mt-0.5 block text-sm text-stone-500">{desc}</span>
                </span>
              </Link>
            </li>
          ))}
        </ul>
      </section>

      <section className="rounded-3xl bg-leaf-700 p-6 text-white sm:p-8" aria-labelledby="dash-tip">
        <h2 id="dash-tip" className="font-display text-xl font-semibold">Tip del día</h2>
        <p className="mt-2 max-w-2xl text-sm text-leaf-50">
          Enjuaga los envases antes de reciclarlos: un envase limpio vale más y evita olores y plagas
          en el punto de acopio.
        </p>
      </section>
    </div>
  )
}
