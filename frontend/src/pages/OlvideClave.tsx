import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api, ApiError } from '../api'

export default function OlvideClave() {
  const [correo, setCorreo] = useState('')
  const [mensaje, setMensaje] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  async function enviar(ev: React.FormEvent) {
    ev.preventDefault()
    if (!correo.trim()) {
      setError('Escribe tu correo.')
      return
    }
    setEnviando(true)
    setError(null)
    try {
      const r = await api.olvideClave(correo.trim())
      setMensaje(r.mensaje)
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudo enviar la solicitud.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="login-fondo">
      <form className="panel login" onSubmit={enviar} noValidate>
        <h1>Recuperar contraseña</h1>
        {mensaje ? (
          <div className="alerta alerta-exito" role="status">{mensaje}</div>
        ) : (
          <>
            <p className="ayuda">Escribe el correo de tu cuenta y te enviaremos un enlace para crear una contraseña nueva.</p>
            <label>
              Correo
              <input type="email" autoComplete="username" value={correo} onChange={(e) => setCorreo(e.target.value)} autoFocus />
            </label>
            {error && <div className="alerta alerta-error" role="alert">{error}</div>}
            <button className="boton ancho-completo" disabled={enviando}>{enviando ? 'Enviando…' : 'Enviar enlace'}</button>
          </>
        )}
        <p className="enlace-volver"><Link to="/login">← Volver al inicio de sesión</Link></p>
      </form>
    </div>
  )
}
