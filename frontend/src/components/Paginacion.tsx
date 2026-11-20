import { useEffect, useMemo, useState } from 'react'

export interface EstadoPaginacion<T> {
  visibles: T[]
  pagina: number
  totalPaginas: number
  porPagina: number
  total: number
  irA: (pagina: number) => void
  cambiarPorPagina: (n: number) => void
}

/** Divide una lista en páginas. Vuelve a la primera página cuando cambia la cantidad de elementos (por ejemplo, al filtrar). */
export function usePaginacion<T>(lista: T[], porPaginaInicial = 10): EstadoPaginacion<T> {
  const [pagina, setPagina] = useState(1)
  const [porPagina, setPorPagina] = useState(porPaginaInicial)
  const total = lista.length
  const totalPaginas = Math.max(1, Math.ceil(total / porPagina))
  const actual = Math.min(pagina, totalPaginas)

  useEffect(() => {
    setPagina(1)
  }, [total])

  const visibles = useMemo(
    () => lista.slice((actual - 1) * porPagina, actual * porPagina),
    [lista, actual, porPagina],
  )

  return {
    visibles,
    pagina: actual,
    totalPaginas,
    porPagina,
    total,
    irA: (p) => setPagina(Math.min(Math.max(1, p), totalPaginas)),
    cambiarPorPagina: (n) => {
      setPorPagina(n)
      setPagina(1)
    },
  }
}

interface Props<T> {
  p: EstadoPaginacion<T>
  opciones?: number[]
  nombre?: string
}

/** Controles de paginación: contador, registros por página y navegación. */
export function Paginacion<T>({ p, opciones = [5, 10, 15, 25, 50, 100], nombre = 'registros' }: Props<T>) {
  if (p.total === 0) return null
  const desde = (p.pagina - 1) * p.porPagina + 1
  const hasta = Math.min(p.pagina * p.porPagina, p.total)
  return (
    <div className="paginacion">
      <span className="ayuda">{desde}–{hasta} de {p.total} {nombre}</span>
      <label className="ayuda">
        Por página{' '}
        <select value={p.porPagina} onChange={(e) => p.cambiarPorPagina(Number(e.target.value))}>
          {opciones.map((n) => <option key={n} value={n}>{n}</option>)}
        </select>
      </label>
      <div className="paginacion-botones">
        <button type="button" className="boton-secundario" disabled={p.pagina === 1} onClick={() => p.irA(1)} aria-label="Primera página">«</button>
        <button type="button" className="boton-secundario" disabled={p.pagina === 1} onClick={() => p.irA(p.pagina - 1)}>‹ Anterior</button>
        <span>Página {p.pagina} de {p.totalPaginas}</span>
        <button type="button" className="boton-secundario" disabled={p.pagina === p.totalPaginas} onClick={() => p.irA(p.pagina + 1)}>Siguiente ›</button>
        <button type="button" className="boton-secundario" disabled={p.pagina === p.totalPaginas} onClick={() => p.irA(p.totalPaginas)} aria-label="Última página">»</button>
      </div>
    </div>
  )
}
