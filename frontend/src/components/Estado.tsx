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
