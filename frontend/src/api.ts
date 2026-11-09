import type {
  Comprobante,
  FiltroReservas,
  Habitacion,
  Huesped,
  HuespedForm,
  LoginResponse,
  ReporteOcupacion,
  Reserva,
  ReservaForm,
  Salud,
  Tarifa,
  TipoHabitacion,
  Usuario,
  UsuarioAdmin,
  EstadoSistema,
  TipoPublico,
  RegistroAuditoria,
} from './types'

const BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'
const CLAVE_TOKEN = 'hotel.token'

export class ApiError extends Error {
  status: number
  constructor(message: string, status: number) {
    super(message)
    this.status = status
  }
}

/* ---- Sesión: el token vive solo en esta pestaña (sessionStorage) ---- */
let token: string | null = null
try {
  token = sessionStorage.getItem(CLAVE_TOKEN)
} catch {
  /* almacenamiento no disponible */
}
let alNoAutorizado: (() => void) | null = null

export function guardarToken(t: string | null) {
  token = t
  try {
    if (t) sessionStorage.setItem(CLAVE_TOKEN, t)
    else sessionStorage.removeItem(CLAVE_TOKEN)
  } catch {
    /* almacenamiento no disponible */
  }
}
export const hayToken = () => token !== null
/** Momento (ms desde 1970) en que vence el token actual, leído de su propio contenido; null si no se puede leer. */
export function expiracionToken(): number | null {
  if (!token) return null
  try {
    const carga = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const { exp } = JSON.parse(atob(carga)) as { exp?: number }
    return typeof exp === 'number' ? exp * 1000 : null
  } catch {
    return null
  }
}
/** La interfaz registra aquí qué hacer cuando el servidor responde 401 (sesión vencida). */
export function alRecibirNoAutorizado(fn: () => void) {
  alNoAutorizado = fn
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`

  let res: Response
  try {
    res = await fetch(BASE + path, { ...init, headers })
  } catch {
    throw new ApiError(
      'No se pudo conectar con el servidor. Verifica que el backend esté corriendo en ' + BASE,
      0,
    )
  }

  if (!res.ok) {
    let mensaje = `Error ${res.status}`
    try {
      const problema = await res.json()
      if (problema.detail) mensaje = problema.detail
    } catch {
      /* respuesta sin cuerpo JSON */
    }
    if (res.status === 401 && path !== '/api/auth/login') alNoAutorizado?.()
    throw new ApiError(mensaje, res.status)
  }
  return res.json() as Promise<T>
}

const json = (metodo: string, data?: unknown): RequestInit => ({
  method: metodo,
  body: data === undefined ? undefined : JSON.stringify(data),
})

export const api = {
  salud: () => request<Salud>('/api/salud'),

  login: (correo: string, password: string) =>
    request<LoginResponse>('/api/auth/login', json('POST', { correo, password })),
  yo: () => request<Usuario>('/api/auth/yo'),

  habitaciones: () => request<Habitacion[]>('/api/habitaciones'),
  disponibles: (entrada: string, salida: string) =>
    request<Habitacion[]>(`/api/habitaciones/disponibles?entrada=${entrada}&salida=${salida}`),
  guardarHabitacion: (id: number | null, data: unknown) =>
    request<Habitacion>(id ? `/api/habitaciones/${id}` : '/api/habitaciones', json(id ? 'PUT' : 'POST', data)),

  tipos: () => request<TipoHabitacion[]>('/api/tipos-habitacion'),
  guardarTipo: (id: number | null, data: unknown) =>
    request<TipoHabitacion>(
      id ? `/api/tipos-habitacion/${id}` : '/api/tipos-habitacion',
      json(id ? 'PUT' : 'POST', data),
    ),

  tarifas: () => request<Tarifa[]>('/api/tarifas'),
  guardarTarifa: (id: number | null, data: unknown) =>
    request<Tarifa>(id ? `/api/tarifas/${id}` : '/api/tarifas', json(id ? 'PUT' : 'POST', data)),
  estadoTarifa: (id: number, activa: boolean) =>
    request<Tarifa>(`/api/tarifas/${id}/estado?activa=${activa}`, json('PATCH')),

  habitacionesPublicas: () => request<TipoPublico[]>('/api/publico/habitaciones'),
  cambiarEstadoHabitacion: (id: number, estado: string) =>
    request<Habitacion>(`/api/habitaciones/${id}/estado`, json('PATCH', { estado })),
  renovarSesion: () => request<LoginResponse>('/api/auth/renovar', json('POST')),
  olvideClave: (correo: string) =>
    request<{ mensaje: string }>('/api/auth/olvide', json('POST', { correo })),
  restablecerClave: (token: string, password: string) =>
    request<{ mensaje: string }>('/api/auth/restablecer', json('POST', { token, password })),
  auditoria: (f: { desde?: string; hasta?: string; accion?: string; texto?: string; limite?: number }) => {
    const q = new URLSearchParams()
    if (f.desde) q.set('desde', f.desde)
    if (f.hasta) q.set('hasta', f.hasta)
    if (f.accion) q.set('accion', f.accion)
    if (f.texto) q.set('texto', f.texto)
    if (f.limite) q.set('limite', String(f.limite))
    const texto = q.toString()
    return request<RegistroAuditoria[]>('/api/auditoria' + (texto ? `?${texto}` : ''))
  },
  estadoSistema: () => request<EstadoSistema>('/api/sistema/estado'),
  usuarios: () => request<UsuarioAdmin[]>('/api/usuarios'),
  guardarUsuario: (id: number | null, data: unknown) =>
    request<UsuarioAdmin>(id ? `/api/usuarios/${id}` : '/api/usuarios', json(id ? 'PUT' : 'POST', data)),

  huespedes: () => request<Huesped[]>('/api/huespedes'),
  crearHuesped: (data: HuespedForm) => request<Huesped>('/api/huespedes', json('POST', data)),

  simularAsistente: (telefono: string, mensaje: string) =>
    request<{ respuesta: string }>('/api/asistente/simular', json('POST', { telefono, mensaje })),

  reservas: (f: FiltroReservas = {}) => {
    const q = new URLSearchParams()
    if (f.estado) q.set('estado', f.estado)
    if (f.desde) q.set('desde', f.desde)
    if (f.hasta) q.set('hasta', f.hasta)
    const texto = q.toString()
    return request<Reserva[]>('/api/reservas' + (texto ? `?${texto}` : ''))
  },
  crearReserva: (data: ReservaForm) => request<Reserva>('/api/reservas', json('POST', data)),
  modificarReserva: (
    id: number,
    data: { habitacionId?: number; fechaEntrada?: string; fechaSalida?: string },
  ) => request<Reserva>(`/api/reservas/${id}`, json('PATCH', data)),
  cancelarReserva: (id: number) => request<Reserva>(`/api/reservas/${id}/cancelar`, json('PATCH')),
  comprobante: (id: number) => request<Comprobante>(`/api/reservas/${id}/comprobante`),
  generarComprobante: (id: number) =>
    request<Comprobante>(`/api/reservas/${id}/comprobante`, json('POST')),

  reporteOcupacion: (desde: string, hasta: string, tipoId?: number) =>
    request<ReporteOcupacion>(
      `/api/reportes/ocupacion?desde=${desde}&hasta=${hasta}` + (tipoId ? `&tipoId=${tipoId}` : ''),
    ),
}
