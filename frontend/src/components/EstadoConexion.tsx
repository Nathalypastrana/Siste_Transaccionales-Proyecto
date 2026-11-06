import { useEffect, useState } from 'react'
import { api } from '../api'

type Estado = 'cargando' | 'ok' | 'error'

/** Indicador del menú lateral: consulta /api/salud para mostrar si la base de datos responde. */
export default function EstadoConexion() {
  const [estado, setEstado] = useState<Estado>('cargando')
  const [detalle, setDetalle] = useState('')

  useEffect(() => {
    let activo = true
    const consultar = () =>
      api
        .salud()
        .then((s) => {
          if (!activo) return
          setEstado('ok')
          setDetalle(s.baseDatos)
        })
        .catch((e: Error) => {
          if (!activo) return
          setEstado('error')
          setDetalle(e.message)
        })

    consultar()
    const temporizador = setInterval(consultar, 20_000)
    return () => {
      activo = false
      clearInterval(temporizador)
    }
  }, [])

  const texto =
    estado === 'cargando' ? 'Verificando…' : estado === 'ok' ? `Base de datos conectada` : 'Sin conexión'

  return (
    <div className={`conexion conexion-${estado}`} title={detalle}>
      <span className="conexion-punto" />
      <div>
        <strong>{texto}</strong>
        {estado === 'ok' && <small>{detalle}</small>}
      </div>
    </div>
  )
}
