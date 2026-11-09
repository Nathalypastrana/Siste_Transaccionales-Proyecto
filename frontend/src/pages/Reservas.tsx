import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, ApiError } from '../api'
import ComprobanteModal from '../components/Comprobante'
import Confirmar from '../components/Confirmar'
import { Cargando, ErrorCarga, Insignia } from '../components/Estado'
import { useToast } from '../components/Toast'
import type { EstadoReserva, Habitacion, Reserva } from '../types'
import { fechaCorta, hoy, moneda } from '../utils'

const ESTADOS: Array<EstadoReserva | ''> = ['', 'PENDIENTE', 'CONFIRMADA', 'CANCELADA', 'FINALIZADA']

function ModificarReserva({
  reserva,
  onCerrar,
  onGuardada,
}: {
  reserva: Reserva
  onCerrar: () => void
  onGuardada: () => void
}) {
  const toast = useToast()
  const [habitaciones, setHabitaciones] = useState<Habitacion[]>([])
  const [habitacionId, setHabitacionId] = useState(String(reserva.habitacionId))
  const [entrada, setEntrada] = useState(reserva.fechaEntrada)
  const [salida, setSalida] = useState(reserva.fechaSalida)
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    api.habitaciones().then(setHabitaciones).catch((e: Error) => setError(e.message))
  }, [])

  async function guardar(ev: React.FormEvent) {
    ev.preventDefault()
    if (!entrada || !salida || salida <= entrada) {
      setError('La fecha de salida debe ser posterior a la de entrada.')
      return
    }
    setEnviando(true)
    setError(null)
    try {
      const r = await api.modificarReserva(reserva.id, {
        habitacionId: Number(habitacionId),
        fechaEntrada: entrada,
        fechaSalida: salida,
      })
      toast('exito', `Reserva #${r.id} actualizada. Nuevo total: ${moneda(r.valorTotal)}.`)
      onGuardada()
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Error inesperado al modificar.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="modal-fondo" onClick={onCerrar}>
      <form className="modal modal-ancho formulario" onSubmit={guardar} onClick={(e) => e.stopPropagation()} noValidate>
        <h2 className="ancho-completo">Modificar reserva #{reserva.id}</h2>
        <p className="ancho-completo ayuda">
          {reserva.huespedNombre}. Se verifica la disponibilidad y se recalcula el valor; si no hay cupo, la reserva queda como estaba.
        </p>
        <label>
          Habitación
          <select value={habitacionId} onChange={(e) => setHabitacionId(e.target.value)}>
            {habitaciones.map((h) => (
              <option key={h.id} value={h.id} disabled={h.estado === 'MANTENIMIENTO'}>
                {h.numero} — {h.tipo}
                {h.estado === 'MANTENIMIENTO' ? ' (mantenimiento)' : ''}
              </option>
            ))}
          </select>
        </label>
        <span />
        <label>
          Fecha de entrada
          <input type="date" min={hoy()} value={entrada} onChange={(e) => setEntrada(e.target.value)} />
        </label>
        <label>
          Fecha de salida
          <input type="date" min={entrada || hoy()} value={salida} onChange={(e) => setSalida(e.target.value)} />
        </label>
        {error && <div className="alerta alerta-error ancho-completo" role="alert">{error}</div>}
        <div className="modal-acciones ancho-completo">
          <button type="button" className="boton-secundario" onClick={onCerrar}>Volver</button>
          <button className="boton" disabled={enviando}>{enviando ? 'Guardando…' : 'Guardar cambios'}</button>
        </div>
      </form>
    </div>
  )
}

export default function Reservas() {
  const toast = useToast()
  const [estado, setEstado] = useState<EstadoReserva | ''>('')
  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')
  const [busqueda, setBusqueda] = useState('')
  const [datos, setDatos] = useState<Reserva[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [cargando, setCargando] = useState(true)
  const [porCancelar, setPorCancelar] = useState<number | null>(null)
  const [porModificar, setPorModificar] = useState<Reserva | null>(null)
  const [verComprobante, setVerComprobante] = useState<number | null>(null)

  const recargar = useCallback(() => {
    setCargando(true)
    setError(null)
    api
      .reservas({ estado, desde, hasta })
      .then(setDatos)
      .catch((e: Error) => setError(e.message))
      .finally(() => setCargando(false))
  }, [estado, desde, hasta])

  useEffect(() => {
    recargar()
  }, [recargar])

  async function cancelar(id: number) {
    setPorCancelar(null)
    try {
      await api.cancelarReserva(id)
      toast('exito', `La reserva #${id} fue cancelada.`)
      recargar()
    } catch (e) {
      toast('error', e instanceof ApiError ? e.message : 'Error inesperado al cancelar.')
    }
  }

  const q = busqueda.trim().toLowerCase()
  const visibles = (datos ?? []).filter(
    (r) =>
      !q ||
      r.huespedNombre.toLowerCase().includes(q) ||
      r.habitacionNumero.toLowerCase().includes(q) ||
      String(r.id) === q,
  )

  return (
    <>
      <header className="encabezado">
        <h1>Historial de reservas</h1>
        <Link to="/reservas/nueva" className="boton">
          + Nueva reserva
        </Link>
      </header>

      <section className="panel">
        <div className="filtros">
          <input
            className="buscador"
            placeholder="Buscar por huésped, habitación o número"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
          />
          <label>
            Estado
            <select value={estado} onChange={(e) => setEstado(e.target.value as EstadoReserva | '')}>
              {ESTADOS.map((e) => (
                <option key={e} value={e}>{e === '' ? 'Todos' : e}</option>
              ))}
            </select>
          </label>
          <label>
            Estadía desde
            <input type="date" value={desde} onChange={(e) => setDesde(e.target.value)} />
          </label>
          <label>
            hasta
            <input type="date" min={desde} value={hasta} onChange={(e) => setHasta(e.target.value)} />
          </label>
          {(estado || desde || hasta) && (
            <button className="boton-secundario" onClick={() => { setEstado(''); setDesde(''); setHasta('') }}>
              Limpiar filtros
            </button>
          )}
        </div>

        {cargando && <Cargando />}
        {!cargando && (error || !datos) && <ErrorCarga mensaje={error} onReintentar={recargar} />}
        {!cargando && datos && !error && (visibles.length === 0 ? (
          <p className="vacio">No hay reservas para los filtros indicados.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>#</th><th>Huésped</th><th>Hab.</th><th>Entrada</th><th>Salida</th><th>Estado</th><th>Total</th><th></th>
              </tr>
            </thead>
            <tbody>
              {visibles.map((r) => {
                const activa = r.estado === 'CONFIRMADA' || r.estado === 'PENDIENTE'
                const futura = r.fechaEntrada > hoy()
                return (
                  <tr key={r.id}>
                    <td>{r.id}</td>
                    <td>{r.huespedNombre}</td>
                    <td>{r.habitacionNumero}</td>
                    <td>{fechaCorta(r.fechaEntrada)}</td>
                    <td>{fechaCorta(r.fechaSalida)}</td>
                    <td><Insignia estado={r.estado} /></td>
                    <td>{moneda(r.valorTotal)}</td>
                    <td className="fila-acciones">
                      {r.estado === 'CONFIRMADA' && (
                        <button className="boton-secundario" onClick={() => setVerComprobante(r.id)}>Comprobante</button>
                      )}
                      {activa && futura && (
                        <>
                          <button className="boton-secundario" onClick={() => setPorModificar(r)}>Modificar</button>
                          <button className="boton-peligro" onClick={() => setPorCancelar(r.id)}>Cancelar</button>
                        </>
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        ))}
        {datos && datos.length >= 200 && (
          <p className="ayuda">Se muestran las 200 reservas más recientes; usa los filtros para acotar.</p>
        )}
      </section>

      {porCancelar !== null && (
        <Confirmar
          titulo="Cancelar reserva"
          mensaje={`¿Seguro que deseas cancelar la reserva #${porCancelar}? La habitación quedará libre en esas fechas.`}
          textoConfirmar="Sí, cancelar reserva"
          onConfirmar={() => cancelar(porCancelar)}
          onCancelar={() => setPorCancelar(null)}
        />
      )}
      {porModificar && (
        <ModificarReserva
          reserva={porModificar}
          onCerrar={() => setPorModificar(null)}
          onGuardada={() => { setPorModificar(null); recargar() }}
        />
      )}
      {verComprobante !== null && (
        <ComprobanteModal reservaId={verComprobante} onCerrar={() => setVerComprobante(null)} />
      )}
    </>
  )
}
