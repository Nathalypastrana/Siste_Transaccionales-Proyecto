import { useMemo, useState } from 'react'
import { api, ApiError } from '../api'
import { Link } from 'react-router-dom'
import { Cargando, ErrorCarga } from '../components/Estado'
import { useToast } from '../components/Toast'
import { Paginacion, usePaginacion } from '../components/Paginacion'
import { useCarga } from '../hooks'
import type { Huesped, HuespedForm } from '../types'
import { normalizar, ultimos10 } from '../utils'

const VACIO: HuespedForm = {
  tipoDocumento: 'CC',
  numeroDocumento: '',
  nombres: '',
  apellidos: '',
  telefono: '',
  correo: '',
}

const TIPOS: Array<{ valor: string; texto: string }> = [
  { valor: 'CC', texto: 'Cédula de ciudadanía' },
  { valor: 'CE', texto: 'Cédula de extranjería' },
  { valor: 'TI', texto: 'Tarjeta de identidad' },
  { valor: 'PA', texto: 'Pasaporte (PA)' },
  { valor: 'PASAPORTE', texto: 'Pasaporte' },
]

type Contacto = 'TODOS' | 'CON_TELEFONO' | 'CON_CORREO' | 'SIN_TELEFONO' | 'SIN_CORREO'
type Orden = 'APELLIDOS' | 'NOMBRES' | 'DOCUMENTO' | 'RECIENTES'
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

/** Avisa antes de enviar si ya existe otro huésped con el mismo documento, nombre completo, correo o teléfono. */
function duplicados(f: HuespedForm, lista: Huesped[], idPropio: number | null): Errores {
  const e: Errores = {}
  const doc = f.numeroDocumento.trim().toUpperCase()
  const nombre = normalizar(`${f.nombres} ${f.apellidos}`)
  const correo = f.correo.trim().toLowerCase()
  const tel = ultimos10(f.telefono)
  for (const h of lista) {
    if (h.id === idPropio) continue
    const quien = `${h.nombres} ${h.apellidos}`
    if (doc && h.numeroDocumento.toUpperCase() === doc) e.numeroDocumento = `Ya está registrado (${quien})`
    if (nombre && normalizar(`${h.nombres} ${h.apellidos}`) === nombre) {
      e.nombres = `Ya existe un huésped con este nombre completo (${h.tipoDocumento} ${h.numeroDocumento})`
      e.apellidos = 'Nombre completo repetido'
    }
    if (correo && (h.correo ?? '').toLowerCase() === correo) e.correo = `Este correo ya es de ${quien}`
    if (tel && ultimos10(h.telefono) === tel) e.telefono = `Este teléfono ya es de ${quien}`
  }
  return e
}

export default function Huespedes() {
  const { datos, error, cargando, recargar } = useCarga(api.huespedes)
  const [form, setForm] = useState<HuespedForm>(VACIO)
  const [editando, setEditando] = useState<Huesped | null>(null)
  const [errores, setErrores] = useState<Errores>({})
  const [enviando, setEnviando] = useState(false)
  const toast = useToast()

  // Filtros y buscador
  const [texto, setTexto] = useState('')
  const [tipo, setTipo] = useState('TODOS')
  const [contacto, setContacto] = useState<Contacto>('TODOS')
  const [orden, setOrden] = useState<Orden>('APELLIDOS')

  const visibles = useMemo(() => {
    if (!datos) return []
    const q = normalizar(texto)
    const qDigitos = texto.replace(/\D/g, '')
    const lista = datos.filter((h) => {
      if (tipo !== 'TODOS' && h.tipoDocumento !== tipo) return false
      if (contacto === 'CON_TELEFONO' && !h.telefono) return false
      if (contacto === 'CON_CORREO' && !h.correo) return false
      if (contacto === 'SIN_TELEFONO' && h.telefono) return false
      if (contacto === 'SIN_CORREO' && h.correo) return false
      if (!q) return true
      const campos = normalizar(`${h.tipoDocumento} ${h.numeroDocumento} ${h.nombres} ${h.apellidos} ${h.correo ?? ''}`)
      const telefono = (h.telefono ?? '').replace(/\D/g, '')
      return campos.includes(q) || (qDigitos.length >= 3 && telefono.includes(qDigitos))
    })
    const comparar: Record<Orden, (a: Huesped, b: Huesped) => number> = {
      APELLIDOS: (a, b) => `${a.apellidos} ${a.nombres}`.localeCompare(`${b.apellidos} ${b.nombres}`, 'es'),
      NOMBRES: (a, b) => `${a.nombres} ${a.apellidos}`.localeCompare(`${b.nombres} ${b.apellidos}`, 'es'),
      DOCUMENTO: (a, b) => a.numeroDocumento.localeCompare(b.numeroDocumento, 'es', { numeric: true }),
      RECIENTES: (a, b) => b.id - a.id,
    }
    return [...lista].sort(comparar[orden])
  }, [datos, texto, tipo, contacto, orden])

  const pag = usePaginacion(visibles)

  const hayFiltros = texto !== '' || tipo !== 'TODOS' || contacto !== 'TODOS'
  const tiposEnUso = useMemo(() => {
    const usados = new Set((datos ?? []).map((h) => h.tipoDocumento))
    return TIPOS.filter((t) => usados.has(t.valor))
  }, [datos])

  function cambiar(campo: keyof HuespedForm, valor: string) {
    setForm((f) => ({ ...f, [campo]: valor }))
    setErrores((e) => ({ ...e, [campo]: undefined }))
  }

  function editar(h: Huesped) {
    setEditando(h)
    setForm({
      tipoDocumento: h.tipoDocumento,
      numeroDocumento: h.numeroDocumento,
      nombres: h.nombres,
      apellidos: h.apellidos,
      telefono: h.telefono ?? '',
      correo: h.correo ?? '',
    })
    setErrores({})
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  function cancelarEdicion() {
    setEditando(null)
    setForm(VACIO)
    setErrores({})
  }

  async function enviar(ev: React.FormEvent) {
    ev.preventDefault()
    const errs = { ...duplicados(form, datos ?? [], editando?.id ?? null), ...validar(form) }
    setErrores(errs)
    if (Object.keys(errs).length > 0) {
      toast('error', 'Revisa los campos marcados en rojo antes de guardar.')
      return
    }

    setEnviando(true)
    try {
      if (editando) {
        const h = await api.actualizarHuesped(editando.id, form)
        toast('exito', `Datos de ${h.nombres} ${h.apellidos} actualizados.`)
      } else {
        const h = await api.crearHuesped(form)
        toast('exito', `Huésped ${h.nombres} ${h.apellidos} registrado correctamente.`)
      }
      setEditando(null)
      setForm(VACIO)
      recargar()
    } catch (e) {
      toast('error', e instanceof ApiError ? e.message : 'Error inesperado al guardar el huésped.')
    } finally {
      setEnviando(false)
    }
  }

  function campo(nombre: keyof HuespedForm, etiqueta: string, tipoInput = 'text') {
    return (
      <label>
        {etiqueta}
        <input
          type={tipoInput}
          value={form[nombre]}
          onChange={(e) => cambiar(nombre, e.target.value)}
          className={errores[nombre] ? 'invalido' : ''}
        />
        {errores[nombre] && <span className="error-campo">{errores[nombre]}</span>}
      </label>
    )
  }

  return (
    <>
      <header className="encabezado">
        <h1>Huéspedes</h1>
        <Link to="/reservas/nueva" className="boton-secundario">
          Nueva reserva →
        </Link>
      </header>

      <section className="panel">
        <h2>{editando ? `Editar huésped: ${editando.nombres} ${editando.apellidos}` : 'Registrar huésped'}</h2>
        <form onSubmit={enviar} noValidate className="formulario">
          <label>
            Tipo de documento
            <select value={form.tipoDocumento} onChange={(e) => cambiar('tipoDocumento', e.target.value)}>
              {TIPOS.map((t) => (
                <option key={t.valor} value={t.valor}>{t.texto}</option>
              ))}
            </select>
          </label>
          {campo('numeroDocumento', 'Número de documento *')}
          {campo('nombres', 'Nombres *')}
          {campo('apellidos', 'Apellidos *')}
          {campo('telefono', 'Teléfono', 'tel')}
          {campo('correo', 'Correo', 'email')}
          <div className="acciones">
            {editando && (
              <button type="button" className="boton-secundario" onClick={cancelarEdicion} disabled={enviando}>
                Cancelar edición
              </button>
            )}
            <button type="submit" className="boton" disabled={enviando}>
              {enviando ? 'Guardando…' : editando ? 'Guardar cambios' : 'Registrar huésped'}
            </button>
          </div>
        </form>
        <p className="ayuda">
          No se permiten dos huéspedes con el mismo número de documento, el mismo nombre completo, el mismo correo o el
          mismo teléfono.
        </p>
      </section>

      <section className="panel">
        <h2>Huéspedes registrados</h2>
        {cargando && <Cargando />}
        {error && <ErrorCarga mensaje={error} onReintentar={recargar} />}
        {datos && (
          <>
            <div className="filtros">
              <input
                className="buscador"
                type="search"
                placeholder="Buscar por nombre, documento, teléfono o correo"
                value={texto}
                onChange={(e) => setTexto(e.target.value)}
              />
              <label>
                Tipo de documento
                <select value={tipo} onChange={(e) => setTipo(e.target.value)}>
                  <option value="TODOS">Todos</option>
                  {tiposEnUso.map((t) => (
                    <option key={t.valor} value={t.valor}>{t.valor}</option>
                  ))}
                </select>
              </label>
              <label>
                Contacto
                <select value={contacto} onChange={(e) => setContacto(e.target.value as Contacto)}>
                  <option value="TODOS">Todos</option>
                  <option value="CON_TELEFONO">Con teléfono</option>
                  <option value="CON_CORREO">Con correo</option>
                  <option value="SIN_TELEFONO">Sin teléfono</option>
                  <option value="SIN_CORREO">Sin correo</option>
                </select>
              </label>
              <label>
                Ordenar por
                <select value={orden} onChange={(e) => setOrden(e.target.value as Orden)}>
                  <option value="APELLIDOS">Apellidos</option>
                  <option value="NOMBRES">Nombres</option>
                  <option value="DOCUMENTO">Documento</option>
                  <option value="RECIENTES">Más recientes</option>
                </select>
              </label>
              {hayFiltros && (
                <button
                  type="button"
                  className="boton-secundario"
                  onClick={() => { setTexto(''); setTipo('TODOS'); setContacto('TODOS') }}
                >
                  Limpiar filtros
                </button>
              )}
            </div>
            <p className="ayuda">{visibles.length} de {datos.length} huéspedes coinciden con los filtros.</p>

            {visibles.length === 0 ? (
              <p className="vacio">
                {datos.length === 0 ? 'No hay huéspedes registrados.' : 'Ningún huésped coincide con la búsqueda.'}
              </p>
            ) : (
              <>
              <table>
                <thead>
                  <tr>
                    <th>Documento</th>
                    <th>Nombre</th>
                    <th>Teléfono</th>
                    <th>Correo</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {pag.visibles.map((h) => (
                    <tr key={h.id} className={editando?.id === h.id ? 'seleccionada' : undefined}>
                      <td>{h.tipoDocumento} {h.numeroDocumento}</td>
                      <td>{h.nombres} {h.apellidos}</td>
                      <td>{h.telefono ?? '—'}</td>
                      <td>{h.correo ?? '—'}</td>
                      <td className="fila-acciones">
                        <button type="button" className="boton-secundario" onClick={() => editar(h)}>
                          Editar
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <Paginacion p={pag} nombre="huéspedes" />
              </>
            )}
          </>
        )}
      </section>
    </>
  )
}
