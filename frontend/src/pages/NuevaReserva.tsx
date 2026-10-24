import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, ApiError } from '../api'
import { Alerta, Cargando } from '../components/Estado'
import { useCarga } from '../hooks'
import type { Habitacion, Reserva } from '../types'
import { fechaCorta, hoy, moneda, noches } from '../utils'

interface Errores {
  huespedId?: string
  usuarioId?: string
  fechaEntrada?: string
  fechaSalida?: string
  habitacionId?: string
}

export default function NuevaReserva() {
  const base = useCarga(async () => {
    const [huespedes, usuarios] = await Promise.all([api.huespedes(), api.usuarios()])
    return { huespedes, usuarios }
  })

  const [huespedId, setHuespedId] = useState('')
  const [usuarioId, setUsuarioId] = useState('')
  const [fechaEntrada, setFechaEntrada] = useState('')
  const [fechaSalida, setFechaSalida] = useState('')
  const [habitacionId, setHabitacionId] = useState('')

  const [disponibles, setDisponibles] = useState<Habitacion[] | null>(null)
  const [buscando, setBuscando] = useState(false)
  const [errores, setErrores] = useState<Errores>({})
  const [errorGlobal, setErrorGlobal] = useState<string | null>(null)
  const [creada, setCreada] = useState<Reserva | null>(null)
  const [enviando, setEnviando] = useState(false)

  const fechasValidas = fechaEntrada !== '' && fechaSalida !== '' && fechaSalida > fechaEntrada

  // Cada vez que cambian las fechas, consulta qué habitaciones están libres
  useEffect(() => {
    setHabitacionId('')
    setDisponibles(null)
    if (!fechasValidas) return

    let vigente = true
    setBuscando(true)
    api
      .disponibles(fechaEntrada, fechaSalida)
      .then((h) => vigente && setDisponibles(h))
      .catch((e: Error) => vigente && setErrorGlobal(e.message))
      .finally(() => vigente && setBuscando(false))
    return () => {
      vigente = false
    }
  }, [fechaEntrada, fechaSalida, fechasValidas])

  const seleccionada = disponibles?.find((h) => String(h.id) === habitacionId)
  const cantNoches = fechasValidas ? noches(fechaEntrada, fechaSalida) : 0
  const totalEstimado =
    seleccionada?.precioNoche != null ? seleccionada.precioNoche * cantNoches : null

  function validar(): Errores {
    const e: Errores = {}
    if (!huespedId) e.huespedId = 'Selecciona un huésped'
    if (!usuarioId) e.usuarioId = 'Selecciona quién registra la reserva'
    if (!fechaEntrada) e.fechaEntrada = 'La fecha de entrada es obligatoria'
    else if (fechaEntrada < hoy()) e.fechaEntrada = 'La fecha de entrada no puede ser pasada'
    if (!fechaSalida) e.fechaSalida = 'La fecha de salida es obligatoria'
    else if (fechaEntrada && fechaSalida <= fechaEntrada)
      e.fechaSalida = 'La salida debe ser posterior a la entrada'
    if (!habitacionId) e.habitacionId = 'Selecciona una habitación disponible'
    return e
  }

  async function enviar(ev: React.FormEvent) {
    ev.preventDefault()
    setErrorGlobal(null)
    setCreada(null)
    const errs = validar()
    setErrores(errs)
    if (Object.keys(errs).length > 0) return

    setEnviando(true)
    try {
      const r = await api.crearReserva({
        huespedId: Number(huespedId),
        habitacionId: Number(habitacionId),
        usuarioId: Number(usuarioId),
        fechaEntrada,
        fechaSalida,
      })
      setCreada(r)
      setHabitacionId('')
      setDisponibles(null)
      setFechaEntrada('')
      setFechaSalida('')
    } catch (e) {
      setErrorGlobal(e instanceof ApiError ? e.message : 'Error inesperado')
      // Si otra solicitud se adelantó, refresca la disponibilidad
      if (e instanceof ApiError && e.status === 409 && fechasValidas) {
        api.disponibles(fechaEntrada, fechaSalida).then(setDisponibles).catch(() => {})
        setHabitacionId('')
      }
    } finally {
      setEnviando(false)
    }
  }

  if (base.cargando) return <Cargando />
  if (base.error || !base.datos) return <Alerta tipo="error">{base.error}</Alerta>

  const { huespedes, usuarios } = base.datos

  return (
    <>
      <header className="encabezado">
        <h1>Nueva reserva</h1>
      </header>

      {creada && (
        <Alerta tipo="exito">
          Reserva <strong>#{creada.id}</strong> confirmada para {creada.huespedNombre}: habitación{' '}
          {creada.habitacionNumero}, {fechaCorta(creada.fechaEntrada)} al{' '}
          {fechaCorta(creada.fechaSalida)}. Total {moneda(creada.valorTotal)}.{' '}
          <Link to="/reservas">Ver reservas</Link>
        </Alerta>
      )}
      {errorGlobal && <Alerta tipo="error">{errorGlobal}</Alerta>}

      <form onSubmit={enviar} noValidate className="panel formulario">
        <label>
          Huésped *
          <select
            value={huespedId}
            onChange={(e) => {
              setHuespedId(e.target.value)
              setErrores((x) => ({ ...x, huespedId: undefined }))
            }}
            className={errores.huespedId ? 'invalido' : ''}
          >
            <option value="">— Selecciona —</option>
            {huespedes.map((h) => (
              <option key={h.id} value={h.id}>
                {h.nombres} {h.apellidos} ({h.tipoDocumento} {h.numeroDocumento})
              </option>
            ))}
          </select>
          {errores.huespedId && <span className="error-campo">{errores.huespedId}</span>}
          {huespedes.length === 0 && (
            <span className="ayuda">
              No hay huéspedes. <Link to="/huespedes">Registra uno primero</Link>.
            </span>
          )}
        </label>

        <label>
          Registrada por *
          <select
            value={usuarioId}
            onChange={(e) => {
              setUsuarioId(e.target.value)
              setErrores((x) => ({ ...x, usuarioId: undefined }))
            }}
            className={errores.usuarioId ? 'invalido' : ''}
          >
            <option value="">— Selecciona —</option>
            {usuarios.map((u) => (
              <option key={u.id} value={u.id}>
                {u.nombre} ({u.rol})
              </option>
            ))}
          </select>
          {errores.usuarioId && <span className="error-campo">{errores.usuarioId}</span>}
        </label>

        <label>
          Fecha de entrada *
          <input
            type="date"
            min={hoy()}
            value={fechaEntrada}
            onChange={(e) => {
              setFechaEntrada(e.target.value)
              setErrores((x) => ({ ...x, fechaEntrada: undefined }))
            }}
            className={errores.fechaEntrada ? 'invalido' : ''}
          />
          {errores.fechaEntrada && <span className="error-campo">{errores.fechaEntrada}</span>}
        </label>

        <label>
          Fecha de salida *
          <input
            type="date"
            min={fechaEntrada || hoy()}
            value={fechaSalida}
            onChange={(e) => {
              setFechaSalida(e.target.value)
              setErrores((x) => ({ ...x, fechaSalida: undefined }))
            }}
            className={errores.fechaSalida ? 'invalido' : ''}
          />
          {errores.fechaSalida && <span className="error-campo">{errores.fechaSalida}</span>}
        </label>

        <label className="ancho-completo">
          Habitación disponible *
          <select
            value={habitacionId}
            disabled={!fechasValidas || buscando}
            onChange={(e) => {
              setHabitacionId(e.target.value)
              setErrores((x) => ({ ...x, habitacionId: undefined }))
            }}
            className={errores.habitacionId ? 'invalido' : ''}
          >
            <option value="">
              {!fechasValidas
                ? 'Elige primero las fechas'
                : buscando
                  ? 'Buscando…'
                  : '— Selecciona —'}
            </option>
            {disponibles?.map((h) => (
              <option key={h.id} value={h.id} disabled={h.precioNoche == null}>
                Hab. {h.numero} · {h.tipo} · {h.capacidad} pers. · {moneda(h.precioNoche)}/noche
                {h.precioNoche == null ? ' (sin tarifa vigente)' : ''}
              </option>
            ))}
          </select>
          {errores.habitacionId && <span className="error-campo">{errores.habitacionId}</span>}
          {disponibles && disponibles.length === 0 && (
            <span className="ayuda">No hay habitaciones libres en esas fechas.</span>
          )}
        </label>

        {totalEstimado != null && (
          <div className="resumen ancho-completo">
            {cantNoches} {cantNoches === 1 ? 'noche' : 'noches'} × {moneda(seleccionada?.precioNoche)}{' '}
            = <strong>{moneda(totalEstimado)}</strong>
          </div>
        )}

        <div className="acciones ancho-completo">
          <button type="submit" className="boton" disabled={enviando}>
            {enviando ? 'Reservando…' : 'Confirmar reserva'}
          </button>
        </div>
      </form>
    </>
  )
}
