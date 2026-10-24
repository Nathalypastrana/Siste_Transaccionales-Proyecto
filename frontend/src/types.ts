export type EstadoHabitacion = 'DISPONIBLE' | 'RESERVADA' | 'OCUPADA' | 'MANTENIMIENTO'
export type EstadoReserva = 'PENDIENTE' | 'CONFIRMADA' | 'CANCELADA' | 'FINALIZADA'

export interface Habitacion {
  id: number
  numero: string
  piso: number
  capacidad: number
  estado: EstadoHabitacion
  tipo: string
  precioNoche: number | null
}

export interface Huesped {
  id: number
  tipoDocumento: string
  numeroDocumento: string
  nombres: string
  apellidos: string
  telefono: string | null
  correo: string | null
}

export interface HuespedForm {
  tipoDocumento: string
  numeroDocumento: string
  nombres: string
  apellidos: string
  telefono: string
  correo: string
}

export interface Usuario {
  id: number
  nombre: string
  rol: 'RECEPCIONISTA' | 'ADMINISTRADOR'
}

export interface Reserva {
  id: number
  huespedId: number
  huespedNombre: string
  habitacionId: number
  habitacionNumero: string
  fechaEntrada: string
  fechaSalida: string
  estado: EstadoReserva
  valorTotal: number
}

export interface ReservaForm {
  huespedId: number
  habitacionId: number
  usuarioId: number
  fechaEntrada: string
  fechaSalida: string
}
