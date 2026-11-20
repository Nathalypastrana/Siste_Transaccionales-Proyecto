import { useMemo, useState } from 'react'
import { api } from '../api'
import { Cargando, ErrorCarga, Insignia } from '../components/Estado'
import { useCarga } from '../hooks'
import { Paginacion, usePaginacion } from '../components/Paginacion'
import { ETIQUETA_ESTADO } from '../types'
import type { EstadoHabitacion } from '../types'
import { moneda, normalizar } from '../utils'

const ESTADOS: Array<EstadoHabitacion | 'TODAS'> = [
  'TODAS',
  'DISPONIBLE',
  'RESERVADA',
  'OCUPADA',
  'MANTENIMIENTO',
]

export default function Habitaciones() {
  const { datos, error, cargando, recargar } = useCarga(api.habitaciones)
  const [filtro, setFiltro] = useState<EstadoHabitacion | 'TODAS'>('TODAS')
  const [texto, setTexto] = useState('')
  const [tipo, setTipo] = useState('TODOS')
  const [piso, setPiso] = useState('TODOS')
  const [personas, setPersonas] = useState('')

  const visibles = useMemo(() => {
    if (!datos) return []
    const q = normalizar(texto)
    const minimo = Number(personas) || 0
    return datos.filter((h) =>
      (filtro === 'TODAS' || h.estado === filtro) &&
      (tipo === 'TODOS' || h.tipo === tipo) &&
      (piso === 'TODOS' || String(h.piso) === piso) &&
      h.capacidad >= minimo &&
      (!q || normalizar(`${h.numero} ${h.tipo}`).includes(q)))
  }, [datos, filtro, texto, tipo, piso, personas])
  const pag = usePaginacion(visibles, 12)

  if (cargando) return <Cargando />
  if (error || !datos) return <ErrorCarga mensaje={error} onReintentar={recargar} />

  const tipos = [...new Set(datos.map((h) => h.tipo))].sort()
  const pisos = [...new Set(datos.map((h) => h.piso))].sort((a, b) => a - b)
  const hayFiltros = filtro !== 'TODAS' || texto !== '' || tipo !== 'TODOS' || piso !== 'TODOS' || personas !== ''

  return (
    <>
      <header className="encabezado">
        <h1>Habitaciones</h1>
      </header>

      <div className="filtros">
        <input className="buscador" type="search" placeholder="Buscar por número o tipo"
          value={texto} onChange={(e) => setTexto(e.target.value)} />
        <label>
          Estado
          <select value={filtro} onChange={(e) => setFiltro(e.target.value as typeof filtro)}>
            {ESTADOS.map((e) => (
              <option key={e} value={e}>
                {e === 'TODAS' ? 'Todos los estados' : ETIQUETA_ESTADO[e]}
              </option>
            ))}
          </select>
        </label>
        <label>
          Tipo
          <select value={tipo} onChange={(e) => setTipo(e.target.value)}>
            <option value="TODOS">Todos</option>
            {tipos.map((t) => <option key={t} value={t}>{t}</option>)}
          </select>
        </label>
        <label>
          Piso
          <select value={piso} onChange={(e) => setPiso(e.target.value)}>
            <option value="TODOS">Todos</option>
            {pisos.map((p) => <option key={p} value={String(p)}>{p}</option>)}
          </select>
        </label>
        <label>
          Personas (mín.)
          <input type="number" min={1} max={20} value={personas} onChange={(e) => setPersonas(e.target.value)} style={{ width: 90 }} />
        </label>
        {hayFiltros && (
          <button type="button" className="boton-secundario"
            onClick={() => { setFiltro('TODAS'); setTexto(''); setTipo('TODOS'); setPiso('TODOS'); setPersonas('') }}>
            Limpiar filtros
          </button>
        )}
      </div>
      <p className="ayuda">{visibles.length} de {datos.length} habitaciones coinciden con los filtros.</p>

      {visibles.length === 0 ? (
        <p className="vacio">Ninguna habitación coincide con los filtros.</p>
      ) : (
        <section className="rejilla">
          {pag.visibles.map((h) => (
            <article key={h.id} className="tarjeta-hab">
              <div className="tarjeta-hab-cabecera">
                <h3>Hab. {h.numero}</h3>
                <Insignia estado={h.estado} />
              </div>
              <p>{h.tipo}</p>
              <ul>
                <li>Piso {h.piso}</li>
                <li>Capacidad: {h.capacidad}</li>
              </ul>
              <strong className="precio">{moneda(h.precioNoche)} / noche</strong>
            </article>
          ))}
        </section>
      )}
      <Paginacion p={pag} nombre="habitaciones" opciones={[6, 12, 24, 48]} />
    </>
  )
}
