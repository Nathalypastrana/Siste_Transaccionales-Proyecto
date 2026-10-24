import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api, ApiError } from '../api'
import { Alerta, Cargando, Insignia } from '../components/Estado'
import { useCarga } from '../hooks'
import { fechaCorta, moneda } from '../utils'

export default function Reservas() {
  const { datos, error, cargando, recargar } = useCarga(api.reservas)
  const [busqueda, setBusqueda] = useState('')
  const [mensaje, setMensaje] = useState<{ tipo: 'error' | 'exito'; texto: string } | null>(null)

  async function cancelar(id: number) {
    if (!window.confirm(`¿Cancelar la reserva #${id}?`)) return
    setMensaje(null)
    try {
      await api.cancelarReserva(id)
      setMensaje({ tipo: 'exito', texto: `Reserva #${id} cancelada.` })
      recargar()
    } catch (e) {
      setMensaje({ tipo: 'error', texto: e instanceof ApiError ? e.message : 'Error inesperado' })
    }
  }

  if (cargando) return <Cargando />
  if (error || !datos) return <Alerta tipo="error">{error}</Alerta>

  const q = busqueda.trim().toLowerCase()
  const visibles = q
    ? datos.filter(
        (r) =>
          r.huespedNombre.toLowerCase().includes(q) ||
          r.habitacionNumero.toLowerCase().includes(q) ||
          String(r.id) === q,
      )
    : datos

  return (
    <>
      <header className="encabezado">
        <h1>Reservas</h1>
        <Link to="/reservas/nueva" className="boton">
          + Nueva reserva
        </Link>
      </header>

      {mensaje && <Alerta tipo={mensaje.tipo}>{mensaje.texto}</Alerta>}

      <section className="panel">
        <input
          className="buscador"
          placeholder="Buscar por huésped, habitación o número de reserva"
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
        />
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
                      <button className="boton-peligro" onClick={() => cancelar(r.id)}>
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
    </>
  )
}
