/** Fecha de hoy (zona horaria local) en formato AAAA-MM-DD. */
export function hoy(): string {
  const d = new Date()
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset())
  return d.toISOString().slice(0, 10)
}

/** Número de noches entre dos fechas AAAA-MM-DD. */
export function noches(entrada: string, salida: string): number {
  const ms = new Date(salida + 'T00:00:00').getTime() - new Date(entrada + 'T00:00:00').getTime()
  return Math.round(ms / 86_400_000)
}

export function moneda(valor: number | null | undefined): string {
  if (valor == null) return '—'
  return new Intl.NumberFormat('es-CO', {
    style: 'currency',
    currency: 'COP',
    maximumFractionDigits: 0,
  }).format(valor)
}

export function fechaCorta(iso: string): string {
  const [y, m, d] = iso.split('-')
  return `${d}/${m}/${y}`
}

/** Suma días a una fecha AAAA-MM-DD. */
export function sumarDias(iso: string, dias: number): string {
  const d = new Date(iso + 'T00:00:00')
  d.setDate(d.getDate() + dias)
  const mes = String(d.getMonth() + 1).padStart(2, '0')
  const dia = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${mes}-${dia}`
}

/** Texto en minúsculas, sin tildes y sin espacios repetidos: sirve para buscar y comparar nombres. */
export function normalizar(texto: string | null | undefined): string {
  return (texto ?? '').normalize('NFD').replace(/\p{M}/gu, '').trim().replace(/\s+/g, ' ').toLowerCase()
}

/** Últimos 10 dígitos de un teléfono (ignora indicativo, espacios y guiones). */
export function ultimos10(telefono: string | null | undefined): string {
  const d = (telefono ?? '').replace(/\D/g, '')
  return d.length > 10 ? d.slice(-10) : d
}
