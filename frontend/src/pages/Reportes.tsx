import { useState } from 'react'
import { api, ApiError } from '../api'
import { Alerta, Cargando } from '../components/Estado'
import { useCarga } from '../hooks'
import type { ReporteOcupacion } from '../types'
import { fechaCorta, sumarDias } from '../utils'

function primerDiaMes(offset: number): string {
  const d = new Date()
  const f = new Date(d.getFullYear(), d.getMonth() + offset, 1)
  return `${f.getFullYear()}-${String(f.getMonth() + 1).padStart(2, '0')}-01`
}

export default function Reportes() {
  const tipos = useCarga(api.tipos)
  const [desde, setDesde] = useState(primerDiaMes(0))
  const [hasta, setHasta] = useState(primerDiaMes(1))
  const [tipoId, setTipoId] = useState('')
  const [reporte, setReporte] = useState<ReporteOcupacion | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [cargando, setCargando] = useState(false)

  async function generar(ev: React.FormEvent) {
    ev.preventDefault()
    setError(null)
    if (!desde || !hasta || hasta <= desde) {
      setError('La fecha final debe ser posterior a la inicial.')
      return
    }
    setCargando(true)
    try {
      setReporte(await api.reporteOcupacion(desde, hasta, tipoId ? Number(tipoId) : undefined))
    } catch (e) {
      setReporte(null)
      setError(e instanceof ApiError ? e.message : 'No se pudo generar el reporte.')
    } finally {
      setCargando(false)
    }
  }

  return (
    <>
      <header className="encabezado">
        <h1>Reporte de ocupación</h1>
      </header>

      <form className="panel formulario" onSubmit={generar} noValidate>
        <label>
          Desde (incluida)
          <input type="date" value={desde} onChange={(e) => setDesde(e.target.value)} />
        </label>
        <label>
          Hasta (no incluida)
          <input type="date" value={hasta} onChange={(e) => setHasta(e.target.value)} />
        </label>
        <label>
          Tipo de habitación (opcional)
          <select value={tipoId} onChange={(e) => setTipoId(e.target.value)}>
            <option value="">Todos</option>
            {(tipos.datos ?? []).map((t) => (
              <option key={t.id} value={t.id}>{t.nombre}</option>
            ))}
          </select>
        </label>
        <div className="acciones">
          <button className="boton" disabled={cargando}>Generar reporte</button>
        </div>
      </form>

      {error && <Alerta tipo="error">{error}</Alerta>}
      {cargando && <Cargando texto="Generando…" />}

      {reporte && !cargando && (
        <>
          <p className="ayuda">
            Periodo {fechaCorta(reporte.desde)} al {fechaCorta(sumarDias(reporte.hasta, -1))} ({reporte.dias} días). Las
            reservas canceladas no cuentan como ocupación y las habitaciones en mantenimiento no cuentan como disponibles.
          </p>
          <section className="tarjetas">
            <div className="tarjeta-dato"><span>Ocupación</span><strong>{reporte.porcentajeOcupacion}%</strong></div>
            <div className="tarjeta-dato"><span>Noches ocupadas</span><strong>{reporte.nochesOcupadas} / {reporte.nochesDisponibles}</strong></div>
            <div className="tarjeta-dato"><span>Habitaciones con reservas</span><strong>{reporte.habitacionesConReservas}</strong></div>
            <div className="tarjeta-dato"><span>Habitaciones sin reservas</span><strong>{reporte.habitacionesSinReservas}</strong></div>
          </section>

          <section className="panel">
            <h2>Reservas del periodo por estado</h2>
            <table>
              <thead><tr><th>Estado</th><th>Reservas</th></tr></thead>
              <tbody>
                {Object.entries(reporte.reservasPorEstado).map(([estado, n]) => (
                  <tr key={estado}><td>{estado}</td><td>{n}</td></tr>
                ))}
              </tbody>
            </table>
          </section>

          <section className="panel">
            <h2>Ocupación por tipo de habitación</h2>
            {reporte.porTipo.length === 0 ? (
              <p className="vacio">No hay habitaciones para el filtro elegido.</p>
            ) : (
              <table>
                <thead><tr><th>Tipo</th><th>Habitaciones</th><th>Noches ocupadas</th><th>Ocupación</th></tr></thead>
                <tbody>
                  {reporte.porTipo.map((t) => (
                    <tr key={t.tipo}>
                      <td>{t.tipo}</td>
                      <td>{t.habitaciones}</td>
                      <td>{t.nochesOcupadas} / {t.nochesDisponibles}</td>
                      <td>
                        <div className="barra" aria-label={`${t.porcentajeOcupacion}%`}>
                          <div style={{ width: `${Math.min(100, t.porcentajeOcupacion)}%` }} />
                        </div>
                        {t.porcentajeOcupacion}%
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </section>
        </>
      )}
    </>
  )
}
