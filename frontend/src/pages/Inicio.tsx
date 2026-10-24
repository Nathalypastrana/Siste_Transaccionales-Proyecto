import { Link } from 'react-router-dom'
import { api } from '../api'
import { Alerta, Cargando } from '../components/Estado'
import { useCarga } from '../hooks'
import { fechaCorta, moneda } from '../utils'

export default function Inicio() {
  const { datos, error, cargando } = useCarga(async () => {
    const [habitaciones, reservas, huespedes] = await Promise.all([
      api.habitaciones(),
      api.reservas(),
      api.huespedes(),
    ])
    return { habitaciones, reservas, huespedes }
  })

  if (cargando) return <Cargando />
  if (error || !datos) return <Alerta tipo="error">{error}</Alerta>

  const { habitaciones, reservas, huespedes } = datos
  const disponibles = habitaciones.filter((h) => h.estado === 'DISPONIBLE').length
  const activas = reservas.filter((r) => r.estado === 'CONFIRMADA' || r.estado === 'PENDIENTE')
  const ultimas = reservas.slice(0, 5)

  return (
    <>
      <header className="encabezado">
        <h1>Panel de reservas</h1>
        <Link to="/reservas/nueva" className="boton">
          + Nueva reserva
        </Link>
      </header>

      <section className="tarjetas">
        <div className="tarjeta-dato">
          <span>Habitaciones</span>
          <strong>{habitaciones.length}</strong>
        </div>
        <div className="tarjeta-dato">
          <span>Disponibles hoy</span>
          <strong>{disponibles}</strong>
        </div>
        <div className="tarjeta-dato">
          <span>Reservas activas</span>
          <strong>{activas.length}</strong>
        </div>
        <div className="tarjeta-dato">
          <span>Huéspedes</span>
          <strong>{huespedes.length}</strong>
        </div>
      </section>

      <section className="panel">
        <h2>Últimas reservas</h2>
        {ultimas.length === 0 ? (
          <p className="vacio">Aún no hay reservas registradas.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>#</th>
                <th>Huésped</th>
                <th>Hab.</th>
                <th>Entrada</th>
                <th>Salida</th>
                <th>Total</th>
              </tr>
            </thead>
            <tbody>
              {ultimas.map((r) => (
                <tr key={r.id}>
                  <td>{r.id}</td>
                  <td>{r.huespedNombre}</td>
                  <td>{r.habitacionNumero}</td>
                  <td>{fechaCorta(r.fechaEntrada)}</td>
                  <td>{fechaCorta(r.fechaSalida)}</td>
                  <td>{moneda(r.valorTotal)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        <Link to="/reservas" className="enlace-texto">
          Ver todas las reservas →
        </Link>
      </section>
    </>
  )
}
