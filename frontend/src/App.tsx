import { Link, NavLink, Route, Routes } from 'react-router-dom'
import EstadoConexion from './components/EstadoConexion'
import Migas from './components/Migas'
import Inicio from './pages/Inicio'
import Consultor from './pages/Consultor'
import Habitaciones from './pages/Habitaciones'
import Huespedes from './pages/Huespedes'
import NuevaReserva from './pages/NuevaReserva'
import Reservas from './pages/Reservas'

const enlaces = [
  { to: '/', texto: 'Inicio', fin: true },
  { to: '/reservas/nueva', texto: 'Nueva reserva' },
  { to: '/reservas', texto: 'Reservas', fin: true },
  { to: '/habitaciones', texto: 'Habitaciones' },
  { to: '/huespedes', texto: 'Huéspedes' },
  { to: '/consultor', texto: 'Consultor virtual' },
]

function NoEncontrada() {
  return (
    <section className="panel">
      <h1>Página no encontrada</h1>
      <p className="vacio">La dirección no corresponde a ninguna pantalla del sistema.</p>
      <Link to="/" className="boton">
        Volver al inicio
      </Link>
    </section>
  )
}

export default function App() {
  return (
    <div className="layout">
      <aside className="sidebar">
        <Link to="/" className="marca" aria-label="Ir al inicio">
          <span className="marca-icono">H</span>
          <div>
            <strong>Hotel Reservas</strong>
            <small>Sistema transaccional</small>
          </div>
        </Link>
        <nav>
          {enlaces.map((e) => (
            <NavLink
              key={e.to}
              to={e.to}
              end={e.fin}
              className={({ isActive }) => (isActive ? 'enlace activo' : 'enlace')}
            >
              {e.texto}
            </NavLink>
          ))}
        </nav>
        <EstadoConexion />
      </aside>

      <main className="contenido">
        <Migas />
        <Routes>
          <Route path="/" element={<Inicio />} />
          <Route path="/reservas/nueva" element={<NuevaReserva />} />
          <Route path="/reservas" element={<Reservas />} />
          <Route path="/habitaciones" element={<Habitaciones />} />
          <Route path="/huespedes" element={<Huespedes />} />
          <Route path="/consultor" element={<Consultor />} />
          <Route path="*" element={<NoEncontrada />} />
        </Routes>
      </main>
    </div>
  )
}
