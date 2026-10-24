import { NavLink, Route, Routes } from 'react-router-dom'
import Inicio from './pages/Inicio'
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
]

export default function App() {
  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="marca">
          <span className="marca-icono">H</span>
          <div>
            <strong>Hotel Reservas</strong>
            <small>Sistema transaccional</small>
          </div>
        </div>
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
      </aside>

      <main className="contenido">
        <Routes>
          <Route path="/" element={<Inicio />} />
          <Route path="/reservas/nueva" element={<NuevaReserva />} />
          <Route path="/reservas" element={<Reservas />} />
          <Route path="/habitaciones" element={<Habitaciones />} />
          <Route path="/huespedes" element={<Huespedes />} />
          <Route path="*" element={<p>Página no encontrada.</p>} />
        </Routes>
      </main>
    </div>
  )
}
