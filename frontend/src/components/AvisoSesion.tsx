import { useEffect, useRef, useState } from 'react'
import { formatoRestante, useAuth } from '../auth'
import { useToast } from './Toast'

const AVISO_SUAVE = 10 * 60
const AVISO_MODAL = 5 * 60

/**
 * Avisos de vencimiento de la sesión (dura 2 horas): un mensaje a los 10 minutos y una ventana a los 5
 * para renovarla o cerrar sesión. Si no se renueva, la sesión se cierra sola al vencer.
 */
export default function AvisoSesion() {
  const { segundosRestantes, renovarSesion, cerrarSesion } = useAuth()
  const toast = useToast()
  const avisado = useRef(false)
  const [renovando, setRenovando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (segundosRestantes === null) return
    if (segundosRestantes > AVISO_SUAVE) {
      avisado.current = false
    } else if (!avisado.current) {
      avisado.current = true
      toast('error', 'Tu sesión vence en menos de 10 minutos. Renuévala para no perder tu trabajo.')
    }
  }, [segundosRestantes, toast])

  if (segundosRestantes === null || segundosRestantes > AVISO_MODAL) return null

  async function renovar() {
    setRenovando(true)
    setError(null)
    try {
      await renovarSesion()
    } catch {
      setError('No se pudo renovar. Intenta de nuevo o inicia sesión otra vez.')
    } finally {
      setRenovando(false)
    }
  }

  return (
    <div className="modal-fondo" role="alertdialog" aria-modal="true" aria-labelledby="titulo-sesion">
      <div className="modal">
        <h2 id="titulo-sesion">Tu sesión está por vencer</h2>
        <p>
          Se cerrará en <strong>{formatoRestante(segundosRestantes)}</strong>. Las sesiones duran 2 horas por
          seguridad; puedes renovarla ahora para seguir trabajando.
        </p>
        {error && <div className="alerta alerta-error" role="alert">{error}</div>}
        <div className="modal-acciones">
          <button className="boton-secundario" onClick={cerrarSesion}>Cerrar sesión</button>
          <button className="boton" onClick={renovar} disabled={renovando}>
            {renovando ? 'Renovando…' : 'Renovar sesión'}
          </button>
        </div>
      </div>
    </div>
  )
}
