import { useEffect, useState } from 'react'
import { api, ApiError } from '../api'
import type { Comprobante } from '../types'
import { fechaCorta, moneda } from '../utils'
import { useToast } from './Toast'

/** Ventana con el comprobante de una reserva (RF 07): lo muestra, lo genera si falta y permite imprimirlo. */
export default function ComprobanteModal({ reservaId, onCerrar }: { reservaId: number; onCerrar: () => void }) {
  const toast = useToast()
  const [comp, setComp] = useState<Comprobante | null>(null)
  const [estado, setEstado] = useState<'cargando' | 'sin' | 'error' | 'listo'>('cargando')
  const [mensaje, setMensaje] = useState('')

  useEffect(() => {
    api
      .comprobante(reservaId)
      .then((c) => {
        setComp(c)
        setEstado('listo')
      })
      .catch((e: Error) => {
        if (e instanceof ApiError && e.status === 404) setEstado('sin')
        else {
          setMensaje(e.message)
          setEstado('error')
        }
      })
  }, [reservaId])

  async function generar() {
    try {
      setComp(await api.generarComprobante(reservaId))
      setEstado('listo')
      toast('exito', 'Comprobante generado.')
    } catch (e) {
      toast('error', e instanceof ApiError ? e.message : 'No se pudo generar el comprobante.')
    }
  }

  return (
    <div className="modal-fondo" onClick={onCerrar}>
      <div className="modal modal-ancho" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
        {estado === 'cargando' && <p className="cargando">Cargando…</p>}
        {estado === 'error' && <div className="alerta alerta-error">{mensaje}</div>}
        {estado === 'sin' && (
          <>
            <h2>Comprobante de la reserva #{reservaId}</h2>
            <p>Esta reserva todavía no tiene comprobante. Solo se genera para reservas confirmadas.</p>
            <div className="modal-acciones">
              <button className="boton-secundario" onClick={onCerrar}>Cerrar</button>
              <button className="boton" onClick={generar}>Generar comprobante</button>
            </div>
          </>
        )}
        {estado === 'listo' && comp && (
          <>
            <div className="comprobante" id="comprobante-imprimible">
              <h2>Comprobante de reserva</h2>
              <p className="comprobante-numero">{comp.numeroComprobante}</p>
              <dl>
                <dt>Reserva</dt><dd>#{comp.reservaId} ({comp.estadoReserva})</dd>
                <dt>Huésped</dt><dd>{comp.huesped} — {comp.documento}</dd>
                <dt>Habitación</dt><dd>{comp.habitacion} ({comp.tipoHabitacion})</dd>
                <dt>Estadía</dt><dd>{fechaCorta(comp.fechaEntrada)} al {fechaCorta(comp.fechaSalida)} ({comp.noches} noches)</dd>
                <dt>Tarifa por noche</dt><dd>{moneda(comp.tarifaNoche)}</dd>
                <dt>Total</dt><dd><strong>{moneda(comp.valorTotal)}</strong></dd>
                <dt>Generado</dt><dd>{new Date(comp.fechaGeneracion).toLocaleString('es-CO')}</dd>
              </dl>
              <p className="ayuda">Con el número del comprobante el huésped consulta su reserva en el asistente de WhatsApp.</p>
            </div>
            <div className="modal-acciones no-imprimir">
              <button className="boton-secundario" onClick={onCerrar}>Cerrar</button>
              <button className="boton" onClick={() => window.print()}>Imprimir</button>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
