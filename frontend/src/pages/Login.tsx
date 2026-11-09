import { useState } from 'react'
import { ApiError } from '../api'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth'

export default function Login() {
  const { iniciarSesion, mensajeSesion, limpiarMensaje } = useAuth()
  const [correo, setCorreo] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  async function enviar(ev: React.FormEvent) {
    ev.preventDefault()
    if (!correo.trim() || !password) {
      setError('Escribe tu correo y tu contraseña.')
      return
    }
    setEnviando(true)
    setError(null)
    try {
      await iniciarSesion(correo.trim(), password)
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Error inesperado al iniciar sesión.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="login-fondo">
      <form className="panel login" onSubmit={enviar} noValidate>
        <div className="marca marca-login">
          <span className="marca-icono">H</span>
          <div>
            <strong>Hotel Reservas</strong>
            <small>Sistema transaccional</small>
          </div>
        </div>
        <h1>Iniciar sesión</h1>
        {mensajeSesion && (
          <div className="alerta alerta-error" role="status">
            {mensajeSesion}
          </div>
        )}
        <label>
          Correo
          <input type="email" autoComplete="username" value={correo} onChange={(e) => { setCorreo(e.target.value); limpiarMensaje() }} autoFocus />
        </label>
        <label>
          Contraseña
          <input
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>
        {error && (
          <div className="alerta alerta-error" role="alert">
            {error}
          </div>
        )}
        <button className="boton ancho-completo" disabled={enviando}>
          {enviando ? 'Ingresando…' : 'Ingresar'}
        </button>
        <p className="enlace-volver"><Link to="/olvide">¿Olvidaste tu contraseña?</Link></p>
        <p className="enlace-volver"><Link to="/">← Volver a la página del hotel</Link></p>
      </form>
    </div>
  )
}
