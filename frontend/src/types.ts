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
  tipoId: number
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

export type Rol = 'RECEPCIONISTA' | 'ADMINISTRADOR' | 'ADMIN_GENERAL'

export interface Usuario {
  id: number
  nombre: string
  rol: Rol
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

export interface Salud {
  estado: string
  baseDatos: string
  versionServidor: string
}

export interface ReservaForm {
  huespedId: number
  habitacionId: number
  fechaEntrada: string
  fechaSalida: string
}

export interface LoginResponse {
  token: string
  expiraEnSegundos: number
  usuario: Usuario
}

export interface TipoHabitacion {
  id: number
  nombre: string
  descripcion: string | null
  capacidad: number
}

export interface Tarifa {
  id: number
  tipoHabitacionId: number
  tipo: string
  valor: number
  fechaInicio: string
  fechaFin: string
  activa: boolean
}

export interface UsuarioAdmin {
  id: number
  nombre: string
  correo: string
  rol: Rol
  activo: boolean
}

export interface Comprobante {
  numeroComprobante: string
  fechaGeneracion: string
  reservaId: number
  estadoReserva: EstadoReserva
  huesped: string
  documento: string
  habitacion: string
  tipoHabitacion: string
  fechaEntrada: string
  fechaSalida: string
  noches: number
  tarifaNoche: number
  valorTotal: number
}

export interface ReporteOcupacion {
  desde: string
  hasta: string
  dias: number
  totalHabitaciones: number
  habitacionesEnMantenimiento: number
  nochesDisponibles: number
  nochesOcupadas: number
  porcentajeOcupacion: number
  habitacionesConReservas: number
  habitacionesSinReservas: number
  reservasPorEstado: Record<string, number>
  porTipo: Array<{
    tipo: string
    habitaciones: number
    nochesDisponibles: number
    nochesOcupadas: number
    porcentajeOcupacion: number
  }>
}

export interface FiltroReservas {
  estado?: EstadoReserva | ''
  desde?: string
  hasta?: string
}

export interface EstadoSistema {
  fechaHora: string
  usuariosActivosPorRol: Record<string, number>
  usuariosInactivos: number
  habitaciones: number
  proveedorIa: string
  modeloConfigurado: boolean
  simuladorHabilitado: boolean
  whatsappConfigurado: boolean
  versionAviso: string
  autorizacionesVigentes: number
  autorizacionesRevocadas: number
  jwtSecretoDefinido: boolean
}

export const ETIQUETA_ROL: Record<Rol, string> = {
  RECEPCIONISTA: 'Recepcionista',
  ADMINISTRADOR: 'Administrador del hotel',
  ADMIN_GENERAL: 'Administrador general',
}

export interface TipoPublico {
  nombre: string
  descripcion: string | null
  capacidad: number
  precioNoche: number | null
  habitaciones: number
}

export interface RegistroAuditoria {
  id: number
  fecha: string
  correo: string | null
  rol: string | null
  accion: string
  entidad: string | null
  entidadId: string | null
  detalle: string | null
  ip: string | null
}
