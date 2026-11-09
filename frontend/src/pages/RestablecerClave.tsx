import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api, ApiError } from '../api'

export default function RestablecerClave() {
  const [params] = useSearchParams()
  const token = params.get('token') ?? ''
  const [password, setPassword] = useState('')
  const [repetir, setRepetir] = useState('')
  const [listo, setListo] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  async function enviar(ev: React.FormEvent) {
    ev.preventDefault()
    if (password.length < 8) return setError('La contraseña debe tener al menos 8 caracteres.')
    if (password !== repetir) return setError('Las contraseñas no coinciden.')
    setEnviando(true)
    setError(null)
    try {
      const r = await api.restablecerClave(token, password)
      setListo(r.mensaje)
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudo actualizar la contraseña.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="login-fondo">
      <form className="panel login" onSubmit={enviar} noValidate>
        <h1>Nueva contraseña</h1>
        {!token ? (
          <div className="alerta alerta-error" role="alert">El enlace no es válido. Solicita uno nuevo.</div>
        ) : listo ? (
          <div className="alerta alerta-exito" role="status">{listo}</div>
        ) : (
          <>
            <label>
              Nueva contraseña (mínimo 8 caracteres)
              <input type="password" autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} autoFocus />
            </label>
            <label>
              Repite la contraseña
              <input type="password" autoComplete="new-password" value={repetir} onChange={(e) => setRepetir(e.target.value)} />
            </label>
            {error && <div className="alerta alerta-error" role="alert">{error}</div>}
            <button className="boton ancho-completo" disabled={enviando}>{enviando ? 'Guardando…' : 'Guardar contraseña'}</button>
          </>
        )}
        <p className="enlace-volver">
          {!token || error ? <Link to="/olvide">Solicitar un enlace nuevo</Link> : null}
          {' '}
          <Link to="/login">Ir al inicio de sesión</Link>
        </p>
      </form>
    </div>
  )
}
