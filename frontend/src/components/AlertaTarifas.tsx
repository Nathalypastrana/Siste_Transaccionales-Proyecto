import { Link } from 'react-router-dom'
import { api } from '../api'
import { useCarga } from '../hooks'
import { fechaCorta } from '../utils'

/** Aviso a los administradores: tipos de habitación que se quedarán sin tarifa vigente. No muestra nada si todo está bien. */
export default function AlertaTarifas() {
  const { datos } = useCarga(api.alertasTarifa)
  if (!datos || datos.length === 0) return null
  return (
    <div className="alerta alerta-error" role="alert">
      <strong>Tarifas por vencer.</strong> Sin tarifa vigente no se pueden crear reservas.
      <ul>
        {datos.map((a) => (
          <li key={a.tipo}>
            {a.tipo}:{' '}
            {a.sinTarifaHoy
              ? 'ya no tiene tarifa vigente hoy.'
              : `sin tarifa desde el ${fechaCorta(a.primeraFechaSinTarifa)} (en ${a.diasRestantes} días).`}
          </li>
        ))}
      </ul>
      <Link to="/administracion">Ir a Administración → Tarifas</Link>
    </div>
  )
}
