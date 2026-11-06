import { useEffect, useRef, useState } from 'react'
import { api, ApiError } from '../api'
import { Cargando, ErrorCarga } from '../components/Estado'
import { useToast } from '../components/Toast'
import { useCarga } from '../hooks'
import { hoy, sumarDias } from '../utils'

interface Mensaje {
  de: 'huesped' | 'consultor'
  texto: string
}

const OTRO = 'otro'

/**
 * Simulador del canal de WhatsApp: envía el mensaje al mismo flujo que usa el webhook
 * (identificación por teléfono -> modelo con herramientas -> respuesta).
 */
export default function Consultor() {
  const { datos, error, cargando, recargar } = useCarga(api.huespedes)
  const toast = useToast()
  const [seleccion, setSeleccion] = useState('')
  const [otroTelefono, setOtroTelefono] = useState('')
  const [mensajes, setMensajes] = useState<Mensaje[]>([])
  const [texto, setTexto] = useState('')
  const [enviando, setEnviando] = useState(false)
  const fin = useRef<HTMLDivElement>(null)

  useEffect(() => {
    fin.current?.scrollIntoView({ behavior: 'smooth' })
  }, [mensajes, enviando])

  if (cargando) return <Cargando />
  if (error || !datos) return <ErrorCarga mensaje={error} onReintentar={recargar} />

  const conTelefono = datos.filter((h) => h.telefono)
  const elegido = conTelefono.find((h) => String(h.id) === seleccion)
  const usaOtro = seleccion === OTRO || conTelefono.length === 0
  const telefono = (usaOtro ? otroTelefono : (elegido?.telefono ?? '')).trim()

  const entrada = sumarDias(hoy(), 30)
  const salida = sumarDias(hoy(), 33)
  const sugerencias = [
    `¿Qué habitaciones hay para 2 personas del ${entrada} al ${salida}?`,
    '¿Cuál es el estado de mi reserva?',
    '¿Qué política de cancelación tienen?',
  ]

  function cambiarSeleccion(valor: string) {
    setSeleccion(valor)
    setMensajes([])
  }

  async function enviar(mensaje: string) {
    const m = mensaje.trim()
    if (!m || !telefono || enviando) return
    setMensajes((x) => [...x, { de: 'huesped', texto: m }])
    setTexto('')
    setEnviando(true)
    try {
      const r = await api.simularAsistente(telefono, m)
      setMensajes((x) => [...x, { de: 'consultor', texto: r.respuesta }])
    } catch (e) {
      toast('error', e instanceof ApiError ? e.message : 'Error inesperado al consultar al asistente.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <>
      <header className="encabezado">
        <h1>Consultor virtual</h1>
      </header>

      <section className="panel">
        <p className="chat-nota">
          Simulador del canal de WhatsApp: el consultor identifica al huésped por su teléfono y responde con los
          datos reales del sistema (disponibilidad, tarifas, reservas).
        </p>

        <div className="chat-cabecera">
          {conTelefono.length > 0 && (
            <label>
              Escribe como
              <select value={seleccion} onChange={(e) => cambiarSeleccion(e.target.value)}>
                <option value="">— Selecciona un huésped —</option>
                {conTelefono.map((h) => (
                  <option key={h.id} value={h.id}>
                    {h.nombres} {h.apellidos} ({h.telefono})
                  </option>
                ))}
                <option value={OTRO}>Otro número (no registrado)</option>
              </select>
            </label>
          )}
          {usaOtro && (
            <label>
              Número de WhatsApp
              <input
                value={otroTelefono}
                onChange={(e) => setOtroTelefono(e.target.value)}
                placeholder="573001234567"
              />
            </label>
          )}
        </div>

        <div className="chat" aria-live="polite">
          {mensajes.length === 0 && (
            <div className="chat-vacio">
              <p>{telefono ? 'Escribe una consulta o prueba una sugerencia.' : 'Elige un huésped o un número para comenzar.'}</p>
              {telefono && (
                <div className="sugerencias">
                  {sugerencias.map((s) => (
                    <button key={s} className="sugerencia" onClick={() => enviar(s)}>
                      {s}
                    </button>
                  ))}
                </div>
              )}
            </div>
          )}
          {mensajes.map((m, i) => (
            <div key={i} className={`burbuja burbuja-${m.de}`}>
              {m.texto}
            </div>
          ))}
          {enviando && <div className="burbuja burbuja-consultor burbuja-escribiendo">Escribiendo…</div>}
          <div ref={fin} />
        </div>

        <form
          className="chat-entrada"
          onSubmit={(e) => {
            e.preventDefault()
            enviar(texto)
          }}
        >
          <input
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
            placeholder={telefono ? 'Escribe un mensaje…' : 'Primero elige quién escribe'}
            disabled={!telefono || enviando}
            maxLength={1000}
          />
          <button type="submit" className="boton" disabled={!telefono || enviando || !texto.trim()}>
            Enviar
          </button>
        </form>
      </section>
    </>
  )
}
