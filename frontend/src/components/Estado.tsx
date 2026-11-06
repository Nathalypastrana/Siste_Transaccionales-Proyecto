import type { EstadoHabitacion, EstadoReserva } from '../types'

export function Insignia({ estado }: { estado: EstadoHabitacion | EstadoReserva }) {
  return <span className={`insignia insignia-${estado.toLowerCase()}`}>{estado}</span>
}

export function Cargando({ texto = 'Cargando…' }: { texto?: string }) {
  return <p className="cargando">{texto}</p>
}

export function Alerta({
  tipo,
  children,
}: {
  tipo: 'error' | 'exito'
  children: React.ReactNode
}) {
  return (
    <div className={`alerta alerta-${tipo}`} role={tipo === 'error' ? 'alert' : 'status'}>
      {children}
    </div>
  )
}

/** Error al cargar datos (por ejemplo, sin conexión) con botón para reintentar. */
export function ErrorCarga({
  mensaje,
  onReintentar,
}: {
  mensaje: string | null
  onReintentar: () => void
}) {
  return (
    <div className="alerta alerta-error error-carga" role="alert">
      <span>{mensaje ?? 'No se pudieron cargar los datos.'}</span>
      <button className="boton-secundario" onClick={onReintentar}>
        Reintentar
      </button>
    </div>
  )
}
