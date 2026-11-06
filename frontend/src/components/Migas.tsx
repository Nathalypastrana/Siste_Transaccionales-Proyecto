import { Link, useLocation } from 'react-router-dom'

const TITULOS: Record<string, string> = {
  '/reservas': 'Reservas',
  '/reservas/nueva': 'Nueva reserva',
  '/habitaciones': 'Habitaciones',
  '/huespedes': 'Huéspedes',
  '/consultor': 'Consultor virtual',
}

/** Ruta de navegación (Inicio / Sección / Pantalla) para volver con un clic. */
export default function Migas() {
  const { pathname } = useLocation()
  if (pathname === '/') return null

  return (
    <nav className="migas" aria-label="Ruta de navegación">
      <Link to="/">Inicio</Link>
      {pathname === '/reservas/nueva' && (
        <>
          <span>/</span>
          <Link to="/reservas">Reservas</Link>
        </>
      )}
      <span>/</span>
      <strong>{TITULOS[pathname] ?? 'Página no encontrada'}</strong>
    </nav>
  )
}
