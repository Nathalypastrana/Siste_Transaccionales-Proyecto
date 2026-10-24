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
