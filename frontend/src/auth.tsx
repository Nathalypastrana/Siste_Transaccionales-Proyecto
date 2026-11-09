import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { alRecibirNoAutorizado, api, expiracionToken, guardarToken, hayToken } from './api'
import type { Usuario } from './types'

interface Sesion {
  usuario: Usuario | null
  cargando: boolean
  esAdmin: boolean
  esGeneral: boolean
  /** Segundos que le quedan a la sesión (null si no hay sesión). */
  segundosRestantes: number | null
  /** Aviso para mostrar en el login (p. ej. "tu sesión venció"). */
  mensajeSesion: string | null
  limpiarMensaje: () => void
  iniciarSesion: (correo: string, password: string) => Promise<void>
  renovarSesion: () => Promise<void>
  cerrarSesion: () => void
}

const Contexto = createContext<Sesion | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(null)
  const [cargando, setCargando] = useState(hayToken())
  const [restante, setRestante] = useState<number | null>(null)
  const [mensaje, setMensaje] = useState<string | null>(null)

  const salir = useCallback((motivo: string | null) => {
    guardarToken(null)
    setUsuario(null)
    setRestante(null)
    setMensaje(motivo)
  }, [])
  const cerrarSesion = useCallback(() => salir(null), [salir])

  useEffect(() => {
    alRecibirNoAutorizado(() => salir('Tu sesión venció. Inicia sesión de nuevo.'))
    if (!hayToken()) return
    api
      .yo()
      .then(setUsuario)
      .catch(() => salir(null))
      .finally(() => setCargando(false))
  }, [salir])

  // Cuenta regresiva: avisa cuánto queda y cierra la sesión cuando vence.
  useEffect(() => {
    if (!usuario) return
    const tic = () => {
      const exp = expiracionToken()
      if (exp === null) return
      const r = Math.floor((exp - Date.now()) / 1000)
      if (r <= 0) salir('Tu sesión venció. Inicia sesión de nuevo.')
      else setRestante(r)
    }
    tic()
    const id = window.setInterval(tic, 1000)
    return () => window.clearInterval(id)
  }, [usuario, salir])

  const iniciarSesion = useCallback(async (correo: string, password: string) => {
    const r = await api.login(correo, password)
    guardarToken(r.token)
    setMensaje(null)
    setUsuario(r.usuario)
  }, [])

  const renovarSesion = useCallback(async () => {
    const r = await api.renovarSesion()
    guardarToken(r.token)
    setUsuario(r.usuario)
    const exp = expiracionToken()
    if (exp !== null) setRestante(Math.floor((exp - Date.now()) / 1000))
  }, [])

  return (
    <Contexto.Provider
      value={{
        usuario,
        cargando,
        esAdmin: usuario?.rol === 'ADMINISTRADOR' || usuario?.rol === 'ADMIN_GENERAL',
        esGeneral: usuario?.rol === 'ADMIN_GENERAL',
        segundosRestantes: restante,
        mensajeSesion: mensaje,
        limpiarMensaje: () => setMensaje(null),
        iniciarSesion,
        renovarSesion,
        cerrarSesion,
      }}
    >
      {children}
    </Contexto.Provider>
  )
}

export function useAuth(): Sesion {
  const c = useContext(Contexto)
  if (!c) throw new Error('useAuth debe usarse dentro de AuthProvider')
  return c
}

/** 1 h 05 min / 4 min 30 s: texto corto para mostrar el tiempo restante. */
export function formatoRestante(segundos: number): string {
  const h = Math.floor(segundos / 3600)
  const m = Math.floor((segundos % 3600) / 60)
  const s = segundos % 60
  if (h > 0) return `${h} h ${String(m).padStart(2, '0')} min`
  return `${m} min ${String(s).padStart(2, '0')} s`
}
