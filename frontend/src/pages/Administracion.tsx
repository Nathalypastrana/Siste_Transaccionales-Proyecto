import { useState } from 'react'
import { api, ApiError } from '../api'
import { Alerta, Cargando, ErrorCarga } from '../components/Estado'
import { useToast } from '../components/Toast'
import { useCarga } from '../hooks'
import { useAuth } from '../auth'
import { ETIQUETA_ROL } from '../types'
import type { EstadoHabitacion, Rol } from '../types'
import { fechaCorta, moneda } from '../utils'
import LogoHotel from '../components/LogoHotel'

type Pestana = 'tipos' | 'habitaciones' | 'tarifas' | 'usuarios'

const PESTANAS: Array<{ id: Pestana; texto: string }> = [
  { id: 'tipos', texto: 'Tipos de habitación' },
  { id: 'habitaciones', texto: 'Habitaciones' },
  { id: 'tarifas', texto: 'Tarifas' },
  { id: 'usuarios', texto: 'Usuarios' },
]

function mensajeError(e: unknown): string {
  return e instanceof ApiError ? e.message : 'Error inesperado.'
}

/* ───────── Tipos de habitación ───────── */
function Tipos() {
  const toast = useToast()
  const { datos, error, cargando, recargar } = useCarga(api.tipos)
  const [editando, setEditando] = useState<number | null>(null)
  const [nombre, setNombre] = useState('')
  const [descripcion, setDescripcion] = useState('')
  const [capacidad, setCapacidad] = useState('2')
  const [err, setErr] = useState<string | null>(null)

  function limpiar() {
    setEditando(null); setNombre(''); setDescripcion(''); setCapacidad('2'); setErr(null)
  }

  async function guardar(ev: React.FormEvent) {
    ev.preventDefault()
    setErr(null)
    try {
      await api.guardarTipo(editando, { nombre: nombre.trim(), descripcion: descripcion.trim() || null, capacidad: Number(capacidad) })
      toast('exito', editando ? 'Tipo actualizado.' : 'Tipo creado.')
      limpiar()
      recargar()
    } catch (e) {
      setErr(mensajeError(e))
    }
  }

  return (
    <>
      <form className="panel formulario" onSubmit={guardar} noValidate>
        <h2 className="ancho-completo">{editando ? 'Editar tipo' : 'Nuevo tipo de habitación'}</h2>
        <label>Nombre<input value={nombre} onChange={(e) => setNombre(e.target.value)} maxLength={50} /></label>
        <label>Capacidad<input type="number" min={1} value={capacidad} onChange={(e) => setCapacidad(e.target.value)} /></label>
        <label className="ancho-completo">Descripción (opcional)<input value={descripcion} onChange={(e) => setDescripcion(e.target.value)} maxLength={255} /></label>
        {err && <div className="ancho-completo"><Alerta tipo="error">{err}</Alerta></div>}
        <div className="acciones ancho-completo">
          {editando && <button type="button" className="boton-secundario" onClick={limpiar}>Cancelar edición</button>}
          <button className="boton">{editando ? 'Guardar cambios' : 'Crear tipo'}</button>
        </div>
      </form>
      <section className="panel">
        {cargando && <Cargando />}
        {!cargando && (error || !datos) && <ErrorCarga mensaje={error} onReintentar={recargar} />}
        {!cargando && datos && (
          <table>
            <thead><tr><th>Nombre</th><th>Capacidad</th><th>Descripción</th><th></th></tr></thead>
            <tbody>
              {datos.map((t) => (
                <tr key={t.id}>
                  <td>{t.nombre}</td><td>{t.capacidad}</td><td>{t.descripcion ?? '—'}</td>
                  <td className="fila-acciones">
                    <button className="boton-secundario" onClick={() => {
                      setEditando(t.id); setNombre(t.nombre); setDescripcion(t.descripcion ?? ''); setCapacidad(String(t.capacidad)); setErr(null)
                    }}>Editar</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </>
  )
}

/* ───────── Habitaciones ───────── */
function HabitacionesAdmin({ soloEstado, onCambiarTarifa }: { soloEstado: boolean; onCambiarTarifa: (tipoId: number) => void }) {
  const toast = useToast()
  const tipos = useCarga(api.tipos)
  const { datos, error, cargando, recargar } = useCarga(api.habitaciones)
  const [editando, setEditando] = useState<number | null>(null)
  const [numero, setNumero] = useState('')
  const [piso, setPiso] = useState('1')
  const [capacidad, setCapacidad] = useState('2')
  const [estado, setEstado] = useState<EstadoHabitacion>('DISPONIBLE')
  const [tipoId, setTipoId] = useState('')
  const [err, setErr] = useState<string | null>(null)

  function limpiar() {
    setEditando(null); setNumero(''); setPiso('1'); setCapacidad('2'); setEstado('DISPONIBLE'); setTipoId(''); setErr(null)
  }

  async function cambiarEstado(id: number, nuevo: string) {
    try {
      await api.cambiarEstadoHabitacion(id, nuevo)
      toast('exito', 'Estado actualizado.')
    } catch (e) {
      toast('error', mensajeError(e))
    }
    recargar()
  }

  async function guardar(ev: React.FormEvent) {
    ev.preventDefault()
    setErr(null)
    if (!tipoId) { setErr('Selecciona el tipo de habitación.'); return }
    try {
      await api.guardarHabitacion(editando, {
        numero: numero.trim(), piso: Number(piso), capacidad: Number(capacidad),
        estado: editando ? estado : undefined, tipoHabitacionId: Number(tipoId),
      })
      toast('exito', editando ? 'Habitación actualizada.' : 'Habitación creada.')
      limpiar()
      recargar()
    } catch (e) {
      setErr(mensajeError(e))
    }
  }

  return (
    <>
      {!soloEstado && (
      <form className="panel formulario" onSubmit={guardar} noValidate>
        <h2 className="ancho-completo">{editando ? 'Editar habitación' : 'Nueva habitación'}</h2>
        <label>Número<input value={numero} onChange={(e) => setNumero(e.target.value)} maxLength={10} /></label>
        <label>Tipo
          <select value={tipoId} onChange={(e) => setTipoId(e.target.value)}>
            <option value="">Selecciona…</option>
            {(tipos.datos ?? []).map((t) => <option key={t.id} value={t.id}>{t.nombre}</option>)}
          </select>
        </label>
        <label>Piso<input type="number" value={piso} onChange={(e) => setPiso(e.target.value)} /></label>
        <label>Capacidad<input type="number" min={1} value={capacidad} onChange={(e) => setCapacidad(e.target.value)} /></label>
        {editando && (
          <label>Estado
            <select value={estado} onChange={(e) => setEstado(e.target.value as EstadoHabitacion)}>
              <option value="DISPONIBLE">DISPONIBLE</option>
              <option value="OCUPADA">OCUPADA</option>
              <option value="MANTENIMIENTO">MANTENIMIENTO</option>
            </select>
            <span className="ayuda">RESERVADA se calcula sola según las reservas vigentes.</span>
          </label>
        )}
        {err && <div className="ancho-completo"><Alerta tipo="error">{err}</Alerta></div>}
        <div className="acciones ancho-completo">
          {editando && <button type="button" className="boton-secundario" onClick={limpiar}>Cancelar edición</button>}
          <button className="boton">{editando ? 'Guardar cambios' : 'Crear habitación'}</button>
        </div>
      </form>
      )}
      <section className="panel">
        {cargando && <Cargando />}
        {!cargando && (error || !datos) && <ErrorCarga mensaje={error} onReintentar={recargar} />}
        {!cargando && datos && (
          <table>
            <thead><tr><th>Número</th><th>Tipo</th><th>Piso</th><th>Capacidad</th><th>Tarifa/noche</th><th>Estado</th><th></th></tr></thead>
            <tbody>
              {datos.map((h) => (
                <tr key={h.id}>
                  <td>{h.numero}</td><td>{h.tipo}</td><td>{h.piso}</td><td>{h.capacidad}</td><td>{moneda(h.precioNoche)}</td>
                  <td>
                    <select value={h.estado} onChange={(e) => cambiarEstado(h.id, e.target.value)} aria-label={`Estado de la habitación ${h.numero}`}>
                      <option value="DISPONIBLE">DISPONIBLE</option>
                      <option value="OCUPADA">OCUPADA</option>
                      <option value="MANTENIMIENTO">MANTENIMIENTO</option>
                      {h.estado === 'RESERVADA' && <option value="RESERVADA" disabled>RESERVADA</option>}
                    </select>
                  </td>
                  <td className="fila-acciones">
                    {!soloEstado && (
                      <>
                        <button className="boton-secundario" onClick={() => {
                          setEditando(h.id); setNumero(h.numero); setPiso(String(h.piso)); setCapacidad(String(h.capacidad))
                          setEstado(h.estado === 'RESERVADA' ? 'DISPONIBLE' : h.estado); setTipoId(String(h.tipoId)); setErr(null)
                          window.scrollTo({ top: 0, behavior: 'smooth' })
                        }}>Editar</button>
                        <button className="boton-secundario" onClick={() => onCambiarTarifa(h.tipoId)}>Cambiar tarifa</button>
                      </>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </>
  )
}

/* ───────── Tarifas ───────── */
function Tarifas({ tipoInicial }: { tipoInicial: string }) {
  const toast = useToast()
  const tipos = useCarga(api.tipos)
  const { datos, error, cargando, recargar } = useCarga(api.tarifas)
  const [editando, setEditando] = useState<number | null>(null)
  const [tipoId, setTipoId] = useState(tipoInicial)
  const [valor, setValor] = useState('')
  const [inicio, setInicio] = useState('')
  const [fin, setFin] = useState('')
  const [err, setErr] = useState<string | null>(null)

  function limpiar() {
    setEditando(null); setTipoId(''); setValor(''); setInicio(''); setFin(''); setErr(null)
  }

  async function guardar(ev: React.FormEvent) {
    ev.preventDefault()
    setErr(null)
    if (!tipoId || !valor || !inicio || !fin) { setErr('Completa todos los campos.'); return }
    if (fin < inicio) { setErr('La fecha de fin no puede ser anterior a la de inicio.'); return }
    try {
      await api.guardarTarifa(editando, { tipoHabitacionId: Number(tipoId), valor: Number(valor), fechaInicio: inicio, fechaFin: fin, activa: true })
      toast('exito', editando ? 'Tarifa actualizada.' : 'Tarifa creada.')
      limpiar()
      recargar()
    } catch (e) {
      setErr(mensajeError(e))
    }
  }

  async function alternar(id: number, activa: boolean) {
    try {
      await api.estadoTarifa(id, activa)
      toast('exito', activa ? 'Tarifa activada.' : 'Tarifa desactivada.')
      recargar()
    } catch (e) {
      toast('error', mensajeError(e))
    }
  }

  return (
    <>
      <form className="panel formulario" onSubmit={guardar} noValidate>
        <h2 className="ancho-completo">{editando ? 'Editar tarifa' : 'Nueva tarifa'}</h2>
        <label>Tipo de habitación
          <select value={tipoId} onChange={(e) => setTipoId(e.target.value)}>
            <option value="">Selecciona…</option>
            {(tipos.datos ?? []).map((t) => <option key={t.id} value={t.id}>{t.nombre}</option>)}
          </select>
        </label>
        <label>Valor por noche (COP)<input type="number" min={1} value={valor} onChange={(e) => setValor(e.target.value)} /></label>
        <label>Vigente desde<input type="date" value={inicio} onChange={(e) => setInicio(e.target.value)} /></label>
        <label>Vigente hasta<input type="date" value={fin} onChange={(e) => setFin(e.target.value)} /></label>
        {err && <div className="ancho-completo"><Alerta tipo="error">{err}</Alerta></div>}
        <div className="acciones ancho-completo">
          {editando && <button type="button" className="boton-secundario" onClick={limpiar}>Cancelar edición</button>}
          <button className="boton">{editando ? 'Guardar cambios' : 'Crear tarifa'}</button>
        </div>
      </form>
      <section className="panel">
        {cargando && <Cargando />}
        {!cargando && (error || !datos) && <ErrorCarga mensaje={error} onReintentar={recargar} />}
        {!cargando && datos && (
          <table>
            <thead><tr><th>Tipo</th><th>Valor/noche</th><th>Desde</th><th>Hasta</th><th>Estado</th><th></th></tr></thead>
            <tbody>
              {datos.map((t) => (
                <tr key={t.id}>
                  <td>{t.tipo}</td><td>{moneda(t.valor)}</td><td>{fechaCorta(t.fechaInicio)}</td><td>{fechaCorta(t.fechaFin)}</td>
                  <td>{t.activa ? 'Activa' : 'Inactiva'}</td>
                  <td className="fila-acciones">
                    <button className="boton-secundario" onClick={() => {
                      setEditando(t.id); setTipoId(String(t.tipoHabitacionId)); setValor(String(t.valor))
                      setInicio(t.fechaInicio); setFin(t.fechaFin); setErr(null)
                    }}>Editar</button>
                    <button className="boton-secundario" onClick={() => alternar(t.id, !t.activa)}>
                      {t.activa ? 'Desactivar' : 'Activar'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </>
  )
}

/* ───────── Usuarios ───────── */
function Usuarios() {
  const toast = useToast()
  const { esGeneral } = useAuth()
  const { datos, error, cargando, recargar } = useCarga(api.usuarios)
  const [editando, setEditando] = useState<number | null>(null)
  const [nombre, setNombre] = useState('')
  const [correo, setCorreo] = useState('')
  const [password, setPassword] = useState('')
  const [rol, setRol] = useState<Rol>('RECEPCIONISTA')
  const [activo, setActivo] = useState(true)
  const [err, setErr] = useState<string | null>(null)

  function limpiar() {
    setEditando(null); setNombre(''); setCorreo(''); setPassword(''); setRol('RECEPCIONISTA'); setActivo(true); setErr(null)
  }

  async function guardar(ev: React.FormEvent) {
    ev.preventDefault()
    setErr(null)
    if (!editando && password.length < 8) { setErr('La contraseña debe tener al menos 8 caracteres.'); return }
    try {
      await api.guardarUsuario(editando, { nombre: nombre.trim(), correo: correo.trim(), password: password || null, rol, activo })
      toast('exito', editando ? 'Usuario actualizado.' : 'Usuario creado.')
      limpiar()
      recargar()
    } catch (e) {
      setErr(mensajeError(e))
    }
  }

  return (
    <>
      <form className="panel formulario" onSubmit={guardar} noValidate>
        <h2 className="ancho-completo">{editando ? 'Editar usuario' : 'Nuevo usuario'}</h2>
        <label>Nombre<input value={nombre} onChange={(e) => setNombre(e.target.value)} maxLength={100} /></label>
        <label>Correo<input type="email" value={correo} onChange={(e) => setCorreo(e.target.value)} maxLength={100} /></label>
        <label>{editando ? 'Nueva contraseña (opcional)' : 'Contraseña (mín. 8)'}
          <input type="password" autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} />
        </label>
        <label>Rol
          <select value={rol} onChange={(e) => setRol(e.target.value as Rol)}>
            <option value="RECEPCIONISTA">{ETIQUETA_ROL.RECEPCIONISTA}</option>
            {esGeneral && <option value="ADMINISTRADOR">{ETIQUETA_ROL.ADMINISTRADOR}</option>}
            {esGeneral && <option value="ADMIN_GENERAL">{ETIQUETA_ROL.ADMIN_GENERAL}</option>}
          </select>
          {!esGeneral && <span className="ayuda">Como administrador del hotel gestionas cuentas de recepcionistas.</span>}
        </label>
        <label><span>Activo</span>
          <input type="checkbox" checked={activo} onChange={(e) => setActivo(e.target.checked)} />
        </label>
        {err && <div className="ancho-completo"><Alerta tipo="error">{err}</Alerta></div>}
        <div className="acciones ancho-completo">
          {editando && <button type="button" className="boton-secundario" onClick={limpiar}>Cancelar edición</button>}
          <button className="boton">{editando ? 'Guardar cambios' : 'Crear usuario'}</button>
        </div>
      </form>
      <section className="panel">
        {cargando && <Cargando />}
        {!cargando && (error || !datos) && <ErrorCarga mensaje={error} onReintentar={recargar} />}
        {!cargando && datos && (
          <table>
            <thead><tr><th>Nombre</th><th>Correo</th><th>Rol</th><th>Estado</th><th></th></tr></thead>
            <tbody>
              {datos.map((u) => (
                <tr key={u.id}>
                  <td>{u.nombre}</td><td>{u.correo}</td><td>{ETIQUETA_ROL[u.rol]}</td><td>{u.activo ? 'Activo' : 'Inactivo'}</td>
                  <td className="fila-acciones">
                    <button className="boton-secundario" onClick={() => {
                      setEditando(u.id); setNombre(u.nombre); setCorreo(u.correo); setPassword(''); setRol(u.rol); setActivo(u.activo); setErr(null)
                    }}>Editar</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </>
  )
}

export default function Administracion() {
  // Ambos administradores gestionan tipos, habitaciones y tarifas; en Usuarios el del hotel solo ve recepcionistas.
  const pestanas = PESTANAS
  const [pestana, setPestana] = useState<Pestana>(pestanas[0].id)
  const [tipoTarifa, setTipoTarifa] = useState('')
  return (
    <>
      <header className="encabezado"><h1>Administración</h1></header>
      <div className="pestanas" role="tablist">
        {pestanas.map((p) => (
          <button key={p.id} role="tab" aria-selected={pestana === p.id}
            className={pestana === p.id ? 'pestana activa' : 'pestana'} onClick={() => setPestana(p.id)}>
            {p.texto}
          </button>
        ))}
      </div>
      {pestana === 'tipos' && <Tipos />}
      {pestana === 'habitaciones' && (
        <HabitacionesAdmin soloEstado={false} onCambiarTarifa={(id) => { setTipoTarifa(String(id)); setPestana('tarifas') }} />
      )}
      {pestana === 'tarifas' && <Tarifas key={tipoTarifa} tipoInicial={tipoTarifa} />}
      {pestana === 'usuarios' && <Usuarios />}
    </>
  )
}
