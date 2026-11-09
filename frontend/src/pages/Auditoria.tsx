import { useCallback, useEffect, useState } from 'react'
import { api } from '../api'
import { Cargando, ErrorCarga } from '../components/Estado'
import type { RegistroAuditoria } from '../types'

const ACCIONES = [
  'LOGIN_OK', 'LOGIN_FALLIDO', 'LOGIN_BLOQUEADO', 'SESION_RENOVADA',
  'CLAVE_RECUPERACION_SOLICITADA', 'CLAVE_RESTABLECIDA',
  'RESERVA_CREADA', 'RESERVA_MODIFICADA', 'RESERVA_CANCELADA', 'COMPROBANTE_GENERADO',
  'HABITACION_CREADA', 'HABITACION_ACTUALIZADA', 'TIPO_CREADO', 'TIPO_ACTUALIZADO',
  'TARIFA_CREADA', 'TARIFA_ACTUALIZADA', 'TARIFA_ACTIVADA', 'TARIFA_DESACTIVADA',
  'USUARIO_CREADO', 'USUARIO_ACTUALIZADO',
]

/** Registro de auditoría (solo administrador general): quién hizo qué y cuándo. */
export default function Auditoria() {
  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')
  const [accion, setAccion] = useState('')
  const [texto, setTexto] = useState('')
  const [datos, setDatos] = useState<RegistroAuditoria[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [cargando, setCargando] = useState(true)

  const cargar = useCallback(() => {
    setCargando(true)
    setError(null)
    api
      .auditoria({ desde, hasta, accion, texto: texto.trim() })
      .then(setDatos)
      .catch((e: Error) => setError(e.message))
      .finally(() => setCargando(false))
  }, [desde, hasta, accion, texto])

  useEffect(() => {
    const id = window.setTimeout(cargar, 300)
    return () => window.clearTimeout(id)
  }, [cargar])

  return (
    <>
      <header className="encabezado">
        <h1>Auditoría</h1>
        <button className="boton-secundario" onClick={cargar}>Actualizar</button>
      </header>
      <section className="panel">
        <div className="filtros">
          <input className="buscador" placeholder="Buscar por correo, detalle o número" value={texto} onChange={(e) => setTexto(e.target.value)} />
          <label>Acción
            <select value={accion} onChange={(e) => setAccion(e.target.value)}>
              <option value="">Todas</option>
              {ACCIONES.map((a) => <option key={a} value={a}>{a}</option>)}
            </select>
          </label>
          <label>Desde<input type="date" value={desde} onChange={(e) => setDesde(e.target.value)} /></label>
          <label>Hasta<input type="date" min={desde} value={hasta} onChange={(e) => setHasta(e.target.value)} /></label>
        </div>
        {cargando && <Cargando />}
        {!cargando && (error || !datos) && <ErrorCarga mensaje={error} onReintentar={cargar} />}
        {!cargando && datos && !error && (datos.length === 0 ? (
          <p className="vacio">No hay registros para los filtros indicados.</p>
        ) : (
          <div className="tabla-ancha">
            <table>
              <thead><tr><th>Fecha</th><th>Usuario</th><th>Acción</th><th>Elemento</th><th>Detalle</th><th>IP</th></tr></thead>
              <tbody>
                {datos.map((r) => (
                  <tr key={r.id}>
                    <td>{new Date(r.fecha).toLocaleString('es-CO')}</td>
                    <td>{r.correo ?? '—'}{r.rol ? <small className="ayuda"> ({r.rol})</small> : null}</td>
                    <td>{r.accion}</td>
                    <td>{r.entidad ? `${r.entidad}${r.entidadId ? ' #' + r.entidadId : ''}` : '—'}</td>
                    <td>{r.detalle ?? ''}</td>
                    <td>{r.ip ?? ''}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ))}
        {datos && datos.length >= 200 && <p className="ayuda">Se muestran los 200 registros más recientes; usa los filtros para acotar.</p>}
      </section>
    </>
  )
}
