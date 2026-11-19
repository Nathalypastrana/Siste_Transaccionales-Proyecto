import { useState } from 'react'
import { api } from '../api'
import AlertaTarifas from '../components/AlertaTarifas'
import { Cargando, ErrorCarga } from '../components/Estado'
import { useCarga } from '../hooks'
import { ETIQUETA_ROL } from '../types'
import type { Rol } from '../types'

function Fila({ texto, ok, detalle }: { texto: string; ok: boolean; detalle?: string }) {
  return (
    <tr>
      <td>{texto}</td>
      <td>
        <span className={ok ? 'insignia insignia-disponible' : 'insignia insignia-mantenimiento'}>{ok ? 'OK' : 'Revisar'}</span>
      </td>
      <td>{detalle ?? ''}</td>
    </tr>
  )
}

/** Vista del administrador general del programa: estado y configuración (sin secretos ni datos personales). */
export default function Sistema() {
  const { datos: s, error, cargando, recargar } = useCarga(api.estadoSistema)
  const [ejecutando, setEjecutando] = useState(false)
  const [resultado, setResultado] = useState<string | null>(null)

  async function ejecutar(nombre: 'recordatorios' | 'resumen') {
    setEjecutando(true)
    setResultado(null)
    try {
      setResultado((await api.ejecutarTarea(nombre)).mensaje)
    } catch (e) {
      setResultado(e instanceof Error ? e.message : 'No se pudo ejecutar la tarea.')
    } finally {
      setEjecutando(false)
    }
  }
  return (
    <>
      <header className="encabezado">
        <h1>Estado del sistema</h1>
        <button className="boton-secundario" onClick={recargar}>Actualizar</button>
      </header>
      {cargando && <Cargando />}
      {!cargando && (error || !s) && <ErrorCarga mensaje={error} onReintentar={recargar} />}
      {!cargando && s && (
        <>
          <AlertaTarifas />
          <section className="tarjetas">
            {(Object.keys(s.usuariosActivosPorRol) as Rol[]).map((r) => (
              <div className="tarjeta-dato" key={r}>
                <span>{ETIQUETA_ROL[r] ?? r} (activos)</span>
                <strong>{s.usuariosActivosPorRol[r]}</strong>
              </div>
            ))}
            <div className="tarjeta-dato"><span>Cuentas inactivas</span><strong>{s.usuariosInactivos}</strong></div>
            <div className="tarjeta-dato"><span>Habitaciones</span><strong>{s.habitaciones}</strong></div>
          </section>

          <section className="panel">
            <h2>Configuración</h2>
            <table>
              <thead><tr><th>Elemento</th><th>Estado</th><th>Detalle</th></tr></thead>
              <tbody>
                <Fila texto="Clave de sesiones (JWT_SECRET)" ok={s.jwtSecretoDefinido} detalle={s.jwtSecretoDefinido ? '' : 'Usa una clave temporal: las sesiones se pierden al reiniciar'} />
                <Fila texto="Consultor virtual" ok={s.modeloConfigurado} detalle={`Proveedor: ${s.proveedorIa}`} />
                <Fila texto="Correo (SMTP)" ok={s.correoConfigurado} detalle={s.correoConfigurado ? '' : 'Sin SMTP: los correos no salen (solo quedan en el log)'} />
                <Fila texto="WhatsApp (envío)" ok={s.whatsappConfigurado} detalle={s.whatsappConfigurado ? '' : 'Faltan token o número de WhatsApp'} />
                <Fila texto="Simulador del consultor" ok={!s.simuladorHabilitado} detalle={s.simuladorHabilitado ? 'Activo: apágalo en producción' : 'Apagado'} />
              </tbody>
            </table>
          </section>

          <section className="panel">
            <h2>Notificaciones de reservas</h2>
            <p>Canales activos: <strong>{s.canalesNotificacion || 'ninguno'}</strong>. Se avisa al huésped al crear, modificar o cancelar una reserva.</p>
          </section>

          <section className="panel">
            <h2>Avisos programados</h2>
            <p>
              Todos los días: <strong>7:00</strong> resumen del día a los administradores y <strong>9:00</strong>{' '}
              recordatorio de llegada a los huéspedes que entran mañana. Puedes ejecutarlos ahora para probarlos.
            </p>
            <div className="acciones">
              <button className="boton-secundario" disabled={ejecutando} onClick={() => ejecutar('resumen')}>
                Enviar resumen ahora
              </button>
              <button className="boton-secundario" disabled={ejecutando} onClick={() => ejecutar('recordatorios')}>
                Enviar recordatorios ahora
              </button>
            </div>
            {resultado && <p className="ayuda" role="status">{resultado}</p>}
          </section>

          <section className="panel">
            <h2>Tratamiento de datos (consultor)</h2>
            <p>Versión del aviso: <strong>{s.versionAviso || '—'}</strong></p>
            <p>Autorizaciones vigentes: <strong>{s.autorizacionesVigentes}</strong> · Revocadas: <strong>{s.autorizacionesRevocadas}</strong></p>
          </section>
          <p className="ayuda">Actualizado: {new Date(s.fechaHora).toLocaleString('es-CO')}</p>
        </>
      )}
    </>
  )
}
