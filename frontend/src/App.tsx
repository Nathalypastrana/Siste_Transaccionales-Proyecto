import { Link, NavLink, Navigate, Route, Routes } from 'react-router-dom'
import { formatoRestante, useAuth } from './auth'
import EstadoConexion from './components/EstadoConexion'
import Migas from './components/Migas'
import { Cargando } from './components/Estado'
import Administracion from './pages/Administracion'
import Consultor from './pages/Consultor'
import Habitaciones from './pages/Habitaciones'
import Huespedes from './pages/Huespedes'
import Inicio from './pages/Inicio'
import Login from './pages/Login'
import OlvideClave from './pages/OlvideClave'
import RestablecerClave from './pages/RestablecerClave'
import Auditoria from './pages/Auditoria'
import AvisoSesion from './components/AvisoSesion'
import Publica from './pages/Publica'
import NuevaReserva from './pages/NuevaReserva'
import Reportes from './pages/Reportes'
import { ETIQUETA_ROL } from './types'
import type { Rol } from './types'
import Reservas from './pages/Reservas'
import LogoHotel from './components/LogoHotel'

const RECEP: Rol[] = ['RECEPCIONISTA', 'ADMINISTRADOR']
const ADMINS: Rol[] = ['ADMINISTRADOR', 'ADMIN_GENERAL']
const TODOS: Rol[] = ['RECEPCIONISTA', 'ADMINISTRADOR', 'ADMIN_GENERAL']

/** Cada pantalla indica qué roles pueden entrar; el menú y las rutas usan esta misma tabla. */
const enlaces: Array<{ to: string; texto: string; fin?: boolean; roles: Rol[] }> = [
  { to: '/', texto: 'Inicio', fin: true, roles: TODOS },
  { to: '/reservas/nueva', texto: 'Nueva reserva', roles: RECEP },
  { to: '/reservas', texto: 'Reservas', fin: true, roles: TODOS },
  { to: '/habitaciones', texto: 'Habitaciones', roles: TODOS },
  { to: '/huespedes', texto: 'Huéspedes', roles: RECEP },
  { to: '/consultor', texto: 'Consultor virtual', roles: RECEP },
  { to: '/administracion', texto: 'Administración', roles: ADMINS },
  { to: '/reportes', texto: 'Reportes', roles: ADMINS },
  { to: '/auditoria', texto: 'Auditoría', roles: ['ADMIN_GENERAL'] },
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

function Protegida({ roles, children }: { roles: Rol[]; children: React.ReactNode }) {
  const { usuario } = useAuth()
  if (!usuario || !roles.includes(usuario.rol)) {
    return (
      <section className="panel">
        <h1>Sin permisos</h1>
        <p className="vacio">Tu rol no tiene acceso a esta pantalla.</p>
        <Link to="/" className="boton">
          Volver al inicio
        </Link>
      </section>
    )
  }
  return <>{children}</>
}

export default function App() {
  const { usuario, cargando, cerrarSesion, segundosRestantes } = useAuth()

  if (cargando) return <Cargando texto="Verificando sesión…" />
  if (!usuario) {
    // Sin sesión: página pública del hotel y acceso del personal.
    return (
      <Routes>
        <Route path="/" element={<Publica />} />
        <Route path="/login" element={<Login />} />
        <Route path="/olvide" element={<OlvideClave />} />
        <Route path="/restablecer" element={<RestablecerClave />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    )
  }

  return (
    <div className="layout">
      <aside className="sidebar">
        <Link to="/" className="marca" aria-label="Ir al inicio">
          <LogoHotel tamaño={44} />
          <div>
            <strong>Hotel Reservas</strong>
            <small>Sistema transaccional</small>
          </div>
        </Link>
        <nav>
          {enlaces
            .filter((e) => e.roles.includes(usuario.rol))
            .map((e) => (
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
        <div className="sesion">
          <strong>{usuario.nombre}</strong>
          <small>{ETIQUETA_ROL[usuario.rol]}</small>
          {segundosRestantes !== null && (
            <small className={segundosRestantes <= 600 ? 'sesion-poca' : ''}>
              Sesión: {formatoRestante(segundosRestantes)}
            </small>
          )}
          <button className="boton-secundario" onClick={cerrarSesion}>
            Cerrar sesión
          </button>
        </div>
        <EstadoConexion />
      </aside>

      <AvisoSesion />
      <main className="contenido">
        <Migas />
        <Routes>
          <Route path="/" element={<Inicio />} />
          <Route path="/reservas/nueva" element={<Protegida roles={RECEP}><NuevaReserva /></Protegida>} />
          <Route path="/reservas" element={<Reservas />} />
          <Route path="/habitaciones" element={<Habitaciones />} />
          <Route path="/huespedes" element={<Protegida roles={RECEP}><Huespedes /></Protegida>} />
          <Route path="/consultor" element={<Protegida roles={RECEP}><Consultor /></Protegida>} />
          <Route path="/administracion" element={<Protegida roles={ADMINS}><Administracion /></Protegida>} />
          <Route path="/reportes" element={<Protegida roles={ADMINS}><Reportes /></Protegida>} />
          <Route path="/auditoria" element={<Protegida roles={['ADMIN_GENERAL']}><Auditoria /></Protegida>} />
          <Route path="/login" element={<Navigate to="/" replace />} />
          <Route path="*" element={<NoEncontrada />} />
        </Routes>
      </main>
    </div>
  )
}
