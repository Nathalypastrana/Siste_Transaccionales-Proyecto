import { useCallback, useEffect, useState } from 'react'

/** Carga datos al montar y permite recargarlos. */
export function useCarga<T>(cargar: () => Promise<T>) {
  const [datos, setDatos] = useState<T | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [cargando, setCargando] = useState(true)

  const recargar = useCallback(() => {
    setCargando(true)
    setError(null)
    cargar()
      .then(setDatos)
      .catch((e: Error) => setError(e.message))
      .finally(() => setCargando(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    recargar()
  }, [recargar])

  return { datos, error, cargando, recargar }
}
