import { Link } from 'react-router-dom'
import { api } from '../api'
import { Cargando, ErrorCarga } from '../components/Estado'
import { useCarga } from '../hooks'
import { fechaCorta, hoy, moneda } from '../utils'
import { useAuth } from '../auth'
import Sistema from './Sistema'

function PanelRecepcion() {
  const { datos, error, cargando, recargar } = useCarga(async () => {
    const [habitaciones, reservas, huespedes] = await Promise.all([
      api.habitaciones(),
      api.reservas(),
      api.huespedes(),
    ])
    return { habitaciones, reservas, huespedes }
  })

  if (cargando) return <Cargando />
  if (error || !datos) return <ErrorCarga mensaje={error} onReintentar={recargar} />

  const { habitaciones, reservas, huespedes } = datos
  const disponibles = habitaciones.filter((h) => h.estado === 'DISPONIBLE').length
  const activas = reservas.filter((r) => r.estado === 'CONFIRMADA' || r.estado === 'PENDIENTE')
  const ultimas = reservas.slice(0, 5)
  const dia = hoy()
  const llegadas = reservas.filter((r) => r.fechaEntrada === dia && (r.estado === 'CONFIRMADA' || r.estado === 'PENDIENTE'))
  const salidas = reservas.filter((r) => r.fechaSalida === dia && r.estado === 'CONFIRMADA')

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
        <h2>Hoy en recepción</h2>
        <p><strong>Llegadas ({llegadas.length}):</strong>{' '}
          {llegadas.length === 0 ? 'ninguna' : llegadas.map((r) => `${r.huespedNombre} (hab. ${r.habitacionNumero})`).join(', ')}</p>
        <p><strong>Salidas ({salidas.length}):</strong>{' '}
          {salidas.length === 0 ? 'ninguna' : salidas.map((r) => `${r.huespedNombre} (hab. ${r.habitacionNumero})`).join(', ')}</p>
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

function PanelHotel() {
  const { datos, error, cargando, recargar } = useCarga(async () => {
    const d = new Date()
    const desde = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`
    const sig = new Date(d.getFullYear(), d.getMonth() + 1, 1)
    const hasta = `${sig.getFullYear()}-${String(sig.getMonth() + 1).padStart(2, '0')}-01`
    const [reporte, habitaciones] = await Promise.all([api.reporteOcupacion(desde, hasta), api.habitaciones()])
    return { reporte, habitaciones }
  })
  if (cargando) return <Cargando />
  if (error || !datos) return <ErrorCarga mensaje={error} onReintentar={recargar} />
  const { reporte, habitaciones } = datos
  const cuenta = (e: string) => habitaciones.filter((h) => h.estado === e).length
  return (
    <>
      <header className="encabezado">
        <h1>Panel del administrador del hotel</h1>
        <Link to="/reportes" className="boton">Ver reportes</Link>
      </header>
      <section className="tarjetas">
        <div className="tarjeta-dato"><span>Ocupación del mes</span><strong>{reporte.porcentajeOcupacion}%</strong></div>
        <div className="tarjeta-dato"><span>Disponibles</span><strong>{cuenta('DISPONIBLE')}</strong></div>
        <div className="tarjeta-dato"><span>Reservadas</span><strong>{cuenta('RESERVADA')}</strong></div>
        <div className="tarjeta-dato"><span>Ocupadas</span><strong>{cuenta('OCUPADA')}</strong></div>
        <div className="tarjeta-dato"><span>En mantenimiento</span><strong>{cuenta('MANTENIMIENTO')}</strong></div>
      </section>
      <section className="panel">
        <h2>Reservas del mes por estado</h2>
        <table>
          <thead><tr><th>Estado</th><th>Reservas</th></tr></thead>
          <tbody>
            {Object.entries(reporte.reservasPorEstado).map(([e, n]) => (<tr key={e}><td>{e}</td><td>{n}</td></tr>))}
          </tbody>
        </table>
      </section>
    </>
  )
}

/** La pantalla de inicio cambia según el rol de quien inicia sesión. */
export default function Inicio() {
  const { usuario } = useAuth()
  if (usuario?.rol === 'ADMIN_GENERAL') return <Sistema />
  if (usuario?.rol === 'ADMINISTRADOR') return <PanelHotel />
  return <PanelRecepcion />
}
