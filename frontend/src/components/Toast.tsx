import { createContext, useCallback, useContext, useState } from 'react'

type Tipo = 'exito' | 'error'

interface Aviso {
  id: number
  tipo: Tipo
  texto: string
}

const Contexto = createContext<(tipo: Tipo, texto: string) => void>(() => {})

/** Devuelve toast(tipo, texto) para mostrar un aviso de éxito o de error. */
export function useToast() {
  return useContext(Contexto)
}

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [avisos, setAvisos] = useState<Aviso[]>([])

  const cerrar = useCallback((id: number) => {
    setAvisos((a) => a.filter((x) => x.id !== id))
  }, [])

  const mostrar = useCallback(
    (tipo: Tipo, texto: string) => {
      const id = Date.now() + Math.random()
      setAvisos((a) => [...a, { id, tipo, texto }])
      setTimeout(() => cerrar(id), tipo === 'error' ? 7000 : 4500)
    },
    [cerrar],
  )

  return (
    <Contexto.Provider value={mostrar}>
      {children}
      <div className="avisos" aria-live="polite">
        {avisos.map((a) => (
          <div key={a.id} className={`aviso aviso-${a.tipo}`} role={a.tipo === 'error' ? 'alert' : 'status'}>
            <span className="aviso-icono">{a.tipo === 'exito' ? '✓' : '!'}</span>
            <span className="aviso-texto">{a.texto}</span>
            <button className="aviso-cerrar" onClick={() => cerrar(a.id)} aria-label="Cerrar aviso">
              ×
            </button>
          </div>
        ))}
      </div>
    </Contexto.Provider>
  )
}
