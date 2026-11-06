import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api, ApiError } from '../api'
import Confirmar from '../components/Confirmar'
import { Cargando, ErrorCarga, Insignia } from '../components/Estado'
import { useToast } from '../components/Toast'
import { useCarga } from '../hooks'
import { fechaCorta, moneda } from '../utils'
import type { EstadoReserva } from '../types'

type Filtro = 'TODAS' | EstadoReserva

const FILTROS: { valor: Filtro; texto: string }[] = [
  { valor: 'TODAS', texto: 'Todas' },
  { valor: 'PENDIENTE', texto: 'Pendientes' },
  { valor: 'CONFIRMADA', texto: 'Confirmadas' },
  { valor: 'CANCELADA', texto: 'Canceladas' },
  { valor: 'FINALIZADA', texto: 'Finalizadas' },
]

export default function Reservas() {
  const { datos, error, cargando, recargar } = useCarga(api.reservas)
  const toast = useToast()
  const [busqueda, setBusqueda] = useState('')
  const [filtro, setFiltro] = useState<Filtro>('TODAS')
  const [porCancelar, setPorCancelar] = useState<number | null>(null)

  async function cancelar(id: number) {
    setPorCancelar(null)
    try {
      await api.cancelarReserva(id)
      toast('exito', `La reserva #${id} fue cancelada.`)
      recargar()
    } catch (e) {
      toast('error', e instanceof ApiError ? e.message : 'Error inesperado al cancelar.')
    }
  }

  if (cargando) return <Cargando />
  if (error || !datos) return <ErrorCarga mensaje={error} onReintentar={recargar} />

  const q = busqueda.trim().toLowerCase()
  const contar = (f: Filtro) =>
    f === 'TODAS' ? datos.length : datos.filter((r) => r.estado === f).length
  const visibles = datos
    .filter((r) => filtro === 'TODAS' || r.estado === filtro)
    .filter(
      (r) =>
        !q ||
        r.huespedNombre.toLowerCase().includes(q) ||
        r.habitacionNumero.toLowerCase().includes(q) ||
        String(r.id) === q,
    )

  return (
    <>
      <header className="encabezado">
        <h1>Reservas</h1>
        <Link to="/reservas/nueva" className="boton">
          + Nueva reserva
        </Link>
      </header>

      <section className="panel">
        <input
          className="buscador"
          placeholder="Buscar por huésped, habitación o número de reserva"
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
        />
        <div className="filtros" role="group" aria-label="Filtrar por estado">
          {FILTROS.map((f) => (
            <button
              key={f.valor}
              type="button"
              className={filtro === f.valor ? 'filtro activo' : 'filtro'}
              aria-pressed={filtro === f.valor}
              onClick={() => setFiltro(f.valor)}
            >
              {f.texto} <span className="filtro-conteo">{contar(f.valor)}</span>
            </button>
          ))}
        </div>
        <p className="ayuda">
          Mostrando {visibles.length} de {datos.length} reservas
        </p>
        {visibles.length === 0 ? (
          <p className="vacio">No se encontraron reservas.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>#</th>
                <th>Huésped</th>
                <th>Hab.</th>
                <th>Entrada</th>
                <th>Salida</th>
                <th>Estado</th>
                <th>Total</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {visibles.map((r) => (
                <tr key={r.id}>
                  <td>{r.id}</td>
                  <td>{r.huespedNombre}</td>
                  <td>{r.habitacionNumero}</td>
                  <td>{fechaCorta(r.fechaEntrada)}</td>
                  <td>{fechaCorta(r.fechaSalida)}</td>
                  <td>
                    <Insignia estado={r.estado} />
                  </td>
                  <td>{moneda(r.valorTotal)}</td>
                  <td>
                    {(r.estado === 'CONFIRMADA' || r.estado === 'PENDIENTE') && (
                      <button className="boton-peligro" onClick={() => setPorCancelar(r.id)}>
                        Cancelar
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      {porCancelar !== null && (
        <Confirmar
          titulo="Cancelar reserva"
          mensaje={`¿Seguro que deseas cancelar la reserva #${porCancelar}? La habitación quedará libre en esas fechas.`}
          textoConfirmar="Sí, cancelar reserva"
          onConfirmar={() => cancelar(porCancelar)}
          onCancelar={() => setPorCancelar(null)}
        />
      )}
    </>
  )
}
