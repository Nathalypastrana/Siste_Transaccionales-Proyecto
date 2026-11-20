import { useMemo, useRef, useState } from 'react'
import type { Huesped } from '../types'
import { normalizar } from '../utils'

interface Props {
  huespedes: Huesped[]
  valor: string
  onCambiar: (id: string) => void
  invalido?: boolean
}

const MAXIMO = 8

/** Campo con buscador para elegir un huésped por nombre, documento, teléfono o correo. */
export default function BuscadorHuesped({ huespedes, valor, onCambiar, invalido }: Props) {
  const [texto, setTexto] = useState('')
  const [abierto, setAbierto] = useState(false)
  const [activo, setActivo] = useState(0)
  const entrada = useRef<HTMLInputElement>(null)

  const elegido = huespedes.find((h) => String(h.id) === valor) ?? null

  const coincidencias = useMemo(() => {
    const q = normalizar(texto)
    const digitos = texto.replace(/\D/g, '')
    const lista = !q
      ? huespedes
      : huespedes.filter((h) => {
          const campos = normalizar(`${h.nombres} ${h.apellidos} ${h.tipoDocumento} ${h.numeroDocumento} ${h.correo ?? ''}`)
          const tel = (h.telefono ?? '').replace(/\D/g, '')
          return campos.includes(q) || (digitos.length >= 3 && tel.includes(digitos))
        })
    return lista.slice(0, MAXIMO)
  }, [huespedes, texto])

  function elegir(h: Huesped) {
    onCambiar(String(h.id))
    setTexto('')
    setAbierto(false)
  }

  function teclado(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setAbierto(true)
      setActivo((i) => Math.min(i + 1, coincidencias.length - 1))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setActivo((i) => Math.max(i - 1, 0))
    } else if (e.key === 'Enter' && abierto && coincidencias[activo]) {
      e.preventDefault()
      elegir(coincidencias[activo])
    } else if (e.key === 'Escape') {
      setAbierto(false)
    }
  }

  if (elegido) {
    return (
      <div className={`huesped-elegido${invalido ? ' invalido' : ''}`}>
        <span>
          <strong>{elegido.nombres} {elegido.apellidos}</strong>
          <span className="ayuda"> · {elegido.tipoDocumento} {elegido.numeroDocumento}{elegido.telefono ? ` · ${elegido.telefono}` : ''}</span>
        </span>
        <button
          type="button"
          className="boton-secundario"
          onClick={() => { onCambiar(''); setTimeout(() => entrada.current?.focus(), 0) }}
        >
          Cambiar
        </button>
      </div>
    )
  }

  return (
    <div className="combo">
      <input
        ref={entrada}
        type="search"
        role="combobox"
        aria-expanded={abierto}
        aria-autocomplete="list"
        placeholder="Escribe nombre, documento, teléfono o correo"
        value={texto}
        className={invalido ? 'invalido' : ''}
        onChange={(e) => { setTexto(e.target.value); setAbierto(true); setActivo(0) }}
        onFocus={() => setAbierto(true)}
        onBlur={() => setTimeout(() => setAbierto(false), 150)}
        onKeyDown={teclado}
      />
      {abierto && (
        <ul className="combo-lista" role="listbox">
          {coincidencias.length === 0 && <li className="combo-vacio">Ningún huésped coincide con «{texto}».</li>}
          {coincidencias.map((h, i) => (
            <li
              key={h.id}
              role="option"
              aria-selected={i === activo}
              className={i === activo ? 'activo' : undefined}
              onMouseDown={(e) => { e.preventDefault(); elegir(h) }}
              onMouseEnter={() => setActivo(i)}
            >
              <strong>{h.nombres} {h.apellidos}</strong>
              <span className="ayuda">{h.tipoDocumento} {h.numeroDocumento}{h.telefono ? ` · ${h.telefono}` : ''}</span>
            </li>
          ))}
          {!texto && huespedes.length > MAXIMO && (
            <li className="combo-vacio">Mostrando {MAXIMO} de {huespedes.length}. Escribe para buscar.</li>
          )}
        </ul>
      )}
    </div>
  )
}
