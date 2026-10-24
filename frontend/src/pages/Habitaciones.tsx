import { useState } from 'react'
import { api } from '../api'
import { Alerta, Cargando, Insignia } from '../components/Estado'
import { useCarga } from '../hooks'
import type { EstadoHabitacion } from '../types'
import { moneda } from '../utils'

const ESTADOS: Array<EstadoHabitacion | 'TODAS'> = [
  'TODAS',
  'DISPONIBLE',
  'RESERVADA',
  'OCUPADA',
  'MANTENIMIENTO',
]

export default function Habitaciones() {
  const { datos, error, cargando } = useCarga(api.habitaciones)
  const [filtro, setFiltro] = useState<EstadoHabitacion | 'TODAS'>('TODAS')

  if (cargando) return <Cargando />
  if (error || !datos) return <Alerta tipo="error">{error}</Alerta>

  const visibles = filtro === 'TODAS' ? datos : datos.filter((h) => h.estado === filtro)

  return (
    <>
      <header className="encabezado">
        <h1>Habitaciones</h1>
        <select value={filtro} onChange={(e) => setFiltro(e.target.value as typeof filtro)}>
          {ESTADOS.map((e) => (
            <option key={e} value={e}>
              {e === 'TODAS' ? 'Todos los estados' : e}
            </option>
          ))}
        </select>
      </header>

      {visibles.length === 0 ? (
        <p className="vacio">No hay habitaciones con ese estado.</p>
      ) : (
        <section className="rejilla">
          {visibles.map((h) => (
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
    </>
  )
}
