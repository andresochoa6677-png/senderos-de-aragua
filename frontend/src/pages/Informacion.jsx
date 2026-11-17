import { useMemo, useState } from 'react'
import {
  ArrowRight, CheckCircle2, Clock, Home, Info, Leaf, MailX, Recycle,
  Search, ShieldAlert, Sparkles, XCircle
} from 'lucide-react'
import {
  CATEGORIAS, HORARIOS, IMAGENES, NORMATIVA, RESIDUOS, buscarResiduos, normalizar
} from '../data/informacion.js'
import { Aviso } from '../components/ui.jsx'

const CHIP_CATEGORIA = {
  blanco: 'bg-stone-100 text-stone-800 border border-stone-300',
  negro: 'bg-stone-800 text-white',
  verde: 'bg-leaf-600 text-white',
  especial: 'bg-purple-700 text-white'
}

const ETIQUETA_CATEGORIA = {
  blanco: 'Caneca blanca · Aprovechables',
  negro: 'Caneca negra · No aprovechables',
  verde: 'Caneca verde · Orgánicos',
  especial: 'Manejo especial'
}

function Imagen({ src, alt, className = '' }) {
  return (
    <img
      src={src}
      alt={alt}
      loading="lazy"
      width={1200}
      height={800}
      className={`h-auto w-full rounded-2xl object-cover ${className}`}
    />
  )
}

function Seccion({ id, icono: Icono, titulo, children }) {
  return (
    <section id={id} aria-labelledby={`${id}-titulo`} className="scroll-mt-24 space-y-4">
      <h2 id={`${id}-titulo`} className="flex items-center gap-2 font-display text-xl font-semibold text-leaf-900 sm:text-2xl">
        <Icono size={22} className="text-leaf-600" aria-hidden="true" />
        {titulo}
      </h2>
      {children}
    </section>
  )
}

function TarjetaCategoria({ categoria }) {
  return (
    <article className="flex h-full flex-col gap-3 rounded-3xl border border-stone-200 bg-white p-5 shadow-sm">
      <img
        src={categoria.imagen}
        alt={`${categoria.tipo} — ejemplos de residuos`}
        loading="lazy"
        width={1200}
        height={800}
        className="h-32 w-full rounded-2xl object-cover"
      />
      <div>
        <span className={`inline-block rounded-full px-3 py-1 text-xs font-bold uppercase tracking-wide ${CHIP_CATEGORIA[categoria.color]}`}>
          {categoria.nombre}
        </span>
        <h3 className="mt-1.5 font-semibold text-stone-800">{categoria.tipo}</h3>
      </div>
      <p className="text-sm leading-relaxed text-stone-600">{categoria.queEs}</p>
      <ul className="space-y-1 text-sm text-stone-600">
        {categoria.ejemplos.map((e) => (
          <li key={e} className="flex items-start gap-1.5">
            <CheckCircle2 size={14} className="mt-1 shrink-0 text-leaf-600" aria-hidden="true" /> {e}
          </li>
        ))}
      </ul>
      <details className="rounded-xl bg-stone-50 p-3 text-sm text-stone-600">
        <summary className="cursor-pointer font-medium text-stone-700">Errores frecuentes y qué hacer</summary>
        <ul className="mt-2 space-y-1.5">
          {categoria.errores.map((e) => (
            <li key={e} className="flex items-start gap-1.5">
              <XCircle size={14} className="mt-0.5 shrink-0 text-red-500" aria-hidden="true" /> {e}
            </li>
          ))}
        </ul>
        <p className="mt-2 flex items-start gap-1.5 font-medium text-leaf-800">
          <Sparkles size={14} className="mt-0.5 shrink-0" aria-hidden="true" /> {categoria.queHacer}
        </p>
      </details>
    </article>
  )
}

function TarjetaResiduo({ residuo }) {
  const especial = residuo.categoria === 'especial'
  return (
    <article className="space-y-2 rounded-2xl border border-stone-200 bg-white p-4 shadow-sm">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="font-semibold text-stone-800">{residuo.nombre}</h3>
        <span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${CHIP_CATEGORIA[residuo.categoria]}`}>
          {ETIQUETA_CATEGORIA[residuo.categoria]}
        </span>
      </div>
      <p className="text-sm leading-relaxed text-stone-600">{residuo.descripcion}</p>
      {residuo.preparacion && (
        <p className="flex items-start gap-1.5 text-sm text-leaf-800">
          <CheckCircle2 size={15} className="mt-0.5 shrink-0" aria-hidden="true" />
          <span><span className="font-medium">Preparación:</span> {residuo.preparacion}</span>
        </p>
      )}
      {especial && (
        <p className="flex items-start gap-1.5 text-sm font-medium text-purple-800">
          <ShieldAlert size={15} className="mt-0.5 shrink-0" aria-hidden="true" />
          NO lo mezcles con las canecas comunes: requiere manejo especial.
        </p>
      )}
    </article>
  )
}

export default function Informacion() {
  const [consulta, setConsulta] = useState('')
  const resultados = useMemo(() => buscarResiduos(consulta), [consulta])
  const buscando = normalizar(consulta).length > 0

  const secciones = [
    { id: 'donde-va', titulo: '¿Dónde va?' },
    { id: 'tres-colores', titulo: 'Los tres colores' },
    { id: 'especiales', titulo: 'Residuos especiales' },
    { id: 'preparacion', titulo: 'Preparación' },
    { id: 'senderos', titulo: 'En Senderos' },
    { id: 'horarios', titulo: 'Horarios' },
    { id: 'normativa', titulo: 'Normativa' }
  ]

  return (
    <div className="space-y-8">
      <header>
        <h1 className="flex items-center gap-2 font-display text-3xl font-semibold text-leaf-900">
          <Info size={26} className="text-leaf-600" aria-hidden="true" />
          Información
        </h1>
        <p className="mt-2 max-w-2xl text-sm leading-relaxed text-stone-600">
          Resuelve tus dudas sobre el manejo de residuos en Senderos.
        </p>
        <figure className="mt-4 overflow-hidden rounded-3xl border border-leaf-100 sm:max-h-56 sm:overflow-hidden">
          <img
            src={IMAGENES.portada}
            alt="Punto de residuos del condominio con las tres canecas de colores"
            width={1200}
            height={800}
            className="h-40 w-full object-cover sm:h-56"
          />
        </figure>
      </header>

      {/* Accesos rápidos a secciones */}
      <nav aria-label="Secciones de información" className="flex flex-wrap gap-2">
        {secciones.map((s) => (
          <a
            key={s.id}
            href={`#${s.id}`}
            className="rounded-full border border-stone-200 bg-white px-3.5 py-1.5 text-xs font-semibold text-stone-600 transition-colors hover:border-leaf-400 hover:text-leaf-800 focus:outline-none focus-visible:ring-2 focus-visible:ring-leaf-600"
          >
            {s.titulo}
          </a>
        ))}
      </nav>

      {/* ¿DÓNDE VA? — buscador */}
      <Seccion id="donde-va" icono={Search} titulo="¿Dónde va?">
        <form onSubmit={(e) => e.preventDefault()} role="search" className="relative">
          <label htmlFor="buscar-residuo" className="sr-only">¿Qué residuo buscas?</label>
          <Search size={18} aria-hidden="true"
            className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-stone-400" />
          <input
            id="buscar-residuo"
            type="search"
            value={consulta}
            onChange={(e) => setConsulta(e.target.value)}
            placeholder="¿Qué residuo buscas?"
            autoComplete="off"
            className="field-input pl-10"
          />
        </form>

        <div aria-live="polite">
          {buscando && resultados.length > 0 && (
            <p className="text-xs font-medium text-stone-500">
              {resultados.length} resultado{resultados.length !== 1 ? 's' : ''} para "{consulta.trim()}"
            </p>
          )}

          {buscando && resultados.length > 0 && (
            <ul className="mt-3 space-y-3">
              {resultados.map((r) => (
                <li key={r.id}><TarjetaResiduo residuo={r} /></li>
              ))}
            </ul>
          )}

          {buscando && resultados.length === 0 && (
            <Aviso tipo="info">
              <span className="flex items-start gap-2.5">
                <Search size={18} className="mt-0.5 shrink-0" aria-hidden="true" />
                <span>
                  No encontramos ese residuo. Prueba con otro nombre (por ejemplo:
                  botella, lata, cáscara, pañal o pila).
                </span>
              </span>
            </Aviso>
          )}

          {!buscando && (
            <p className="mt-3 text-sm text-stone-500">
              Escribe el nombre de un residuo (por ejemplo: botella, lata, cáscara, pañal,
              pila) y te decimos dónde va y cómo prepararlo.
            </p>
          )}
        </div>
      </Seccion>

      {/* LA REGLA DE LOS TRES COLORES */}
      <Seccion id="tres-colores" icono={Recycle} titulo="La regla de los tres colores">
        <img
          src={IMAGENES.tresCategorias}
          alt="Las tres canecas del condominio: blanca para aprovechables, negra para no aprovechables y verde para orgánicos"
          loading="lazy"
          width={1200}
          height={800}
          className="h-auto w-full rounded-3xl border border-leaf-100 object-cover"
        />
        <div className="grid gap-4 lg:grid-cols-3">
          {CATEGORIAS.map((c) => <TarjetaCategoria key={c.id} categoria={c} />)}
        </div>
      </Seccion>

      {/* RESIDUOS ESPECIALES */}
      <Seccion id="especiales" icono={ShieldAlert} titulo="Residuos especiales">
        <div className="overflow-hidden rounded-3xl border border-purple-200 bg-purple-50/50 p-5">
          <img
            src={IMAGENES.especiales}
            alt="Residuos especiales como pilas, bombillos y pequeños aparatos electrónicos"
            loading="lazy"
            width={1200}
            height={800}
            className="mx-auto h-auto w-full max-w-xl rounded-2xl object-cover"
          />
          <p className="mt-4 flex items-start gap-2 text-sm leading-relaxed text-purple-900">
            <XCircle size={18} className="mt-0.5 shrink-0" aria-hidden="true" />
            <span><span className="font-semibold">NO los mezcles con los residuos comunes.</span> Estos
            residuos requieren un manejo o punto de disposición especial; no van en las tres
            canecas convencionales.</span>
          </p>
          <ul className="mt-3 flex flex-wrap gap-2">
            {RESIDUOS.filter((r) => r.categoria === 'especial').map((r) => (
              <li key={r.id} className="flex items-center gap-1.5 rounded-full border border-purple-200 bg-white px-3 py-1.5 text-sm text-purple-900">
                <MailX size={13} aria-hidden="true" /> {r.nombre}
              </li>
            ))}
          </ul>
          <p className="mt-3 text-sm text-stone-600">
            Consúltalos en el buscador de arriba para ver la indicación de cada uno.
          </p>
        </div>
      </Seccion>

      {/* PREPARACIÓN */}
      <Seccion id="preparacion" icono={Sparkles} titulo="¿Cómo preparar los residuos?">
        <div className="grid gap-4 sm:grid-cols-2">
          <Imagen src={IMAGENES.preparar} alt="Pasos para preparar un envase: vaciar y enjuagar" />
          <Imagen src={IMAGENES.antesDespues} alt="Comparación de un punto de residuos desordenado y uno organizado" />
        </div>
        <div className="space-y-3 rounded-3xl border border-stone-200 bg-white p-5 shadow-sm">
          <p className="text-sm leading-relaxed text-stone-600">
            Los envases aprovechables deben llegar <span className="font-medium text-stone-800">vacíos,
            limpios y sin goteos</span>. Así se evita que contaminen al resto de los reciclables
            y que el punto de residuos genere malos olores.
          </p>
          <ol className="grid gap-2 sm:grid-cols-2">
            {['Vacía todo el contenido', 'Enjuaga cuando el envase tuvo líquidos o comida',
              'Déjalo escurrir bien', 'Depósitalo en la caneca blanca'].map((paso, i) => (
              <li key={paso} className="flex items-center gap-2.5 rounded-xl border border-stone-200 bg-white p-3 text-sm">
                <span className="grid h-6 w-6 shrink-0 place-items-center rounded-lg bg-leaf-100 text-xs font-bold text-leaf-700">
                  {i + 1}
                </span>
                {paso}
              </li>
            ))}
          </ol>
          <p className="flex items-start gap-2 rounded-2xl bg-leaf-50 p-3 text-sm text-leaf-900">
            <Leaf size={16} className="mt-0.5 shrink-0" aria-hidden="true" />
            No es necesario lavar todo con agua abundante: un enjuague rápido y dejar
            escurrir es suficiente. La idea es reducir la contaminación de los
            aprovechables sin desperdiciar agua.
          </p>
        </div>
      </Seccion>

      {/* ¿CÓMO HACERLO EN SENDEROS? */}
      <Seccion id="senderos" icono={Home} titulo="¿Cómo hacerlo en Senderos?">
        <div className="grid gap-4 sm:grid-cols-2">
          <Imagen src={IMAGENES.puntoSenderos} alt="Punto de disposición del condominio con las tres canecas identificadas" />
          <Imagen src={IMAGENES.rutaCorrecta} alt="Residente llevando sus bolsas separadas al punto de disposición" />
        </div>
        <div className="rounded-3xl border border-stone-200 bg-white p-5 shadow-sm">
          <ol className="space-y-3">
            {[
              'Separa los residuos en tu vivienda: blanco, negro y verde.',
              'Prepara los aprovechables: vacíos, limpios y secos.',
              'Lleva tus bolsas al punto de disposición del condominio.',
              'Identifica la caneca correcta por su color.',
              'Deposita cada residuo en su lugar.',
              'Deja el área limpia y ordenada.'
            ].map((paso, i) => (
              <li key={paso} className="flex items-start gap-3">
                <span className="grid h-7 w-7 shrink-0 place-items-center rounded-full bg-leaf-600 text-xs font-bold text-white">
                  {i + 1}
                </span>
                <span className="text-sm leading-relaxed text-stone-700">{paso}</span>
              </li>
            ))}
          </ol>
        </div>
        <div className="rounded-3xl border border-amber-200 bg-amber-50 p-5">
          <h3 className="font-display text-base font-semibold text-amber-900">
            ¿Por qué importa empezar en casa?
          </h3>
          <p className="mt-1.5 text-sm leading-relaxed text-amber-900">
            Cuando los residuos llegan mezclados al punto aparecen los problemas que ya
            conocemos: bolsas mal separadas, contaminación entre materiales, suciedad,
            malos olores, desorden y molestias por fauna. La separación comienza en tu
            vivienda, no cuando llegas al punto de residuos.
          </p>
        </div>
      </Seccion>

      {/* HORARIOS */}
      <Seccion id="horarios" icono={Clock} titulo="Horarios">
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="rounded-3xl border border-stone-200 bg-white p-5 shadow-sm">
            <p className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wide text-stone-500">
              <Clock size={14} aria-hidden="true" /> Disposición de residuos
            </p>
            <p className="mt-2 font-display text-xl font-semibold text-leaf-900">{HORARIOS.dias}</p>
            <p className="text-stone-600">{HORARIOS.horario}</p>
            <p className="mt-2 text-xs text-stone-500">{HORARIOS.nota}</p>
          </div>
          <Imagen src={IMAGENES.horarios} alt="Horario de disposición de residuos del condominio" />
        </div>
      </Seccion>

      {/* NORMATIVA */}
      <Seccion id="normativa" icono={Info} titulo="Normativa">
        <img
          src={IMAGENES.normativa}
          alt="Marco normativo de gestión de residuos aplicable al proyecto"
          loading="lazy"
          width={1200}
          height={800}
          className="mb-4 h-auto w-full rounded-3xl border border-leaf-100 object-cover"
        />
        <ul className="grid gap-4 sm:grid-cols-2">
          {NORMATIVA.map((ref) => (
            <li key={ref.nombre} className="flex h-full flex-col gap-2 rounded-3xl border border-stone-200 bg-white p-5 shadow-sm">
              <h3 className="font-display text-base font-semibold text-leaf-900">{ref.nombre}</h3>
              <p className="text-sm leading-relaxed text-stone-600">{ref.relacion}</p>
              <p className="mt-auto flex items-start gap-1.5 text-sm text-leaf-800">
                <ArrowRight size={14} className="mt-1 shrink-0" aria-hidden="true" />
                {ref.relevancia}
              </p>
            </li>
          ))}
        </ul>
        <p className="text-xs text-stone-400">
          Resumen informativo del proyecto. Consulta los documentos oficiales para el texto completo de cada norma.
        </p>
      </Seccion>
    </div>
  )
}
