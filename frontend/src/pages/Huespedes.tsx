import { useState } from 'react'
import { api, ApiError } from '../api'
import { Alerta, Cargando } from '../components/Estado'
import { useCarga } from '../hooks'
import type { HuespedForm } from '../types'

const VACIO: HuespedForm = {
  tipoDocumento: 'CC',
  numeroDocumento: '',
  nombres: '',
  apellidos: '',
  telefono: '',
  correo: '',
}

type Errores = Partial<Record<keyof HuespedForm, string>>

function validar(f: HuespedForm): Errores {
  const e: Errores = {}
  if (!f.numeroDocumento.trim()) e.numeroDocumento = 'El número de documento es obligatorio'
  else if (!/^[A-Za-z0-9]{4,30}$/.test(f.numeroDocumento.trim()))
    e.numeroDocumento = 'Solo letras y números (mínimo 4)'
  if (!f.nombres.trim()) e.nombres = 'Los nombres son obligatorios'
  if (!f.apellidos.trim()) e.apellidos = 'Los apellidos son obligatorios'
  if (f.telefono && !/^[0-9+ ()-]{7,20}$/.test(f.telefono))
    e.telefono = 'Teléfono no válido (7 a 20 dígitos)'
  if (f.correo && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(f.correo)) e.correo = 'Correo no válido'
  return e
}

export default function Huespedes() {
  const { datos, error, cargando, recargar } = useCarga(api.huespedes)
  const [form, setForm] = useState<HuespedForm>(VACIO)
  const [errores, setErrores] = useState<Errores>({})
  const [mensaje, setMensaje] = useState<{ tipo: 'error' | 'exito'; texto: string } | null>(null)
  const [enviando, setEnviando] = useState(false)

  function cambiar(campo: keyof HuespedForm, valor: string) {
    setForm((f) => ({ ...f, [campo]: valor }))
    setErrores((e) => ({ ...e, [campo]: undefined }))
  }

  async function enviar(ev: React.FormEvent) {
    ev.preventDefault()
    setMensaje(null)
    const errs = validar(form)
    setErrores(errs)
    if (Object.keys(errs).length > 0) return

    setEnviando(true)
    try {
      const h = await api.crearHuesped(form)
      setMensaje({ tipo: 'exito', texto: `Huésped ${h.nombres} ${h.apellidos} registrado.` })
      setForm(VACIO)
      recargar()
    } catch (e) {
      setMensaje({ tipo: 'error', texto: e instanceof ApiError ? e.message : 'Error inesperado' })
    } finally {
      setEnviando(false)
    }
  }

  return (
    <>
      <header className="encabezado">
        <h1>Huéspedes</h1>
      </header>

      <section className="panel">
        <h2>Registrar huésped</h2>
        {mensaje && <Alerta tipo={mensaje.tipo}>{mensaje.texto}</Alerta>}
        <form onSubmit={enviar} noValidate className="formulario">
          <label>
            Tipo de documento
            <select value={form.tipoDocumento} onChange={(e) => cambiar('tipoDocumento', e.target.value)}>
              <option value="CC">Cédula de ciudadanía</option>
              <option value="CE">Cédula de extranjería</option>
              <option value="TI">Tarjeta de identidad</option>
              <option value="PASAPORTE">Pasaporte</option>
            </select>
          </label>
          <label>
            Número de documento *
            <input
              value={form.numeroDocumento}
              onChange={(e) => cambiar('numeroDocumento', e.target.value)}
              className={errores.numeroDocumento ? 'invalido' : ''}
            />
            {errores.numeroDocumento && <span className="error-campo">{errores.numeroDocumento}</span>}
          </label>
          <label>
            Nombres *
            <input
              value={form.nombres}
              onChange={(e) => cambiar('nombres', e.target.value)}
              className={errores.nombres ? 'invalido' : ''}
            />
            {errores.nombres && <span className="error-campo">{errores.nombres}</span>}
          </label>
          <label>
            Apellidos *
            <input
              value={form.apellidos}
              onChange={(e) => cambiar('apellidos', e.target.value)}
              className={errores.apellidos ? 'invalido' : ''}
            />
            {errores.apellidos && <span className="error-campo">{errores.apellidos}</span>}
          </label>
          <label>
            Teléfono
            <input
              value={form.telefono}
              onChange={(e) => cambiar('telefono', e.target.value)}
              className={errores.telefono ? 'invalido' : ''}
            />
            {errores.telefono && <span className="error-campo">{errores.telefono}</span>}
          </label>
          <label>
            Correo
            <input
              value={form.correo}
              onChange={(e) => cambiar('correo', e.target.value)}
              className={errores.correo ? 'invalido' : ''}
            />
            {errores.correo && <span className="error-campo">{errores.correo}</span>}
          </label>
          <div className="acciones">
            <button type="submit" className="boton" disabled={enviando}>
              {enviando ? 'Guardando…' : 'Registrar huésped'}
            </button>
          </div>
        </form>
      </section>

      <section className="panel">
        <h2>Huéspedes registrados</h2>
        {cargando && <Cargando />}
        {error && <Alerta tipo="error">{error}</Alerta>}
        {datos && datos.length === 0 && <p className="vacio">No hay huéspedes registrados.</p>}
        {datos && datos.length > 0 && (
          <table>
            <thead>
              <tr>
                <th>Documento</th>
                <th>Nombre</th>
                <th>Teléfono</th>
                <th>Correo</th>
              </tr>
            </thead>
            <tbody>
              {datos.map((h) => (
                <tr key={h.id}>
                  <td>
                    {h.tipoDocumento} {h.numeroDocumento}
                  </td>
                  <td>
                    {h.nombres} {h.apellidos}
                  </td>
                  <td>{h.telefono ?? '—'}</td>
                  <td>{h.correo ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </>
  )
}
