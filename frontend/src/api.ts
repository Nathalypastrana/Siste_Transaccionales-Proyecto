import type {
  Habitacion,
  Huesped,
  HuespedForm,
  Reserva,
  ReservaForm,
  Usuario,
} from './types'

const BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

export class ApiError extends Error {
  status: number
  constructor(message: string, status: number) {
    super(message)
    this.status = status
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let res: Response
  try {
    res = await fetch(BASE + path, {
      headers: { 'Content-Type': 'application/json' },
      ...init,
    })
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
    throw new ApiError(mensaje, res.status)
  }
  return res.json() as Promise<T>
}

export const api = {
  habitaciones: () => request<Habitacion[]>('/api/habitaciones'),
  disponibles: (entrada: string, salida: string) =>
    request<Habitacion[]>(`/api/habitaciones/disponibles?entrada=${entrada}&salida=${salida}`),

  huespedes: () => request<Huesped[]>('/api/huespedes'),
  crearHuesped: (data: HuespedForm) =>
    request<Huesped>('/api/huespedes', { method: 'POST', body: JSON.stringify(data) }),

  usuarios: () => request<Usuario[]>('/api/usuarios'),

  reservas: () => request<Reserva[]>('/api/reservas'),
  crearReserva: (data: ReservaForm) =>
    request<Reserva>('/api/reservas', { method: 'POST', body: JSON.stringify(data) }),
  cancelarReserva: (id: number) =>
    request<Reserva>(`/api/reservas/${id}/cancelar`, { method: 'PATCH' }),
}
