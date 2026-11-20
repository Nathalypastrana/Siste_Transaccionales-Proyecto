# Plan de pruebas y resultados

**Versión (commit):** `________` · **Fecha:** `________` · **Ejecutó:** `________` · **Entorno:** macOS, Java 17, SQL Server en Docker, Node `____`.
Informe consolidado: `informe-pruebas.md`. Los casos también están en Testiny (importar `docs/testiny/casos-prueba.csv`). Evidencia de persistencia UI → BD: `evidencia-persistencia.md`.
Para cada caso anota en *Evidencia* el nombre de la captura y, si cambia datos, la sección de `pruebas/consultas_evidencia.sql` que lo confirma.

Marca cada caso con ✔ o ✘ al ejecutarlo y guarda una captura con el nombre del caso (por ejemplo `P05.png`).
Preparación: backend y frontend corriendo, tarifas vigentes para las fechas usadas, un huésped con teléfono registrado.
Usa fechas futuras libres. `{H}` es una habitación disponible.

| Id | Proceso (RF) | Pasos | Resultado esperado | Resultado | Evidencia |
|----|--------------|-------|--------------------|-----------|-----------|
| P01 | Inicio de sesión (RF 01) | Entrar con un usuario válido | Entra al panel de su rol | | |
| P02 | Inicio de sesión | Contraseña incorrecta | Mensaje genérico, no entra | | |
| P03 | Bloqueo por intentos | 5 contraseñas incorrectas seguidas | Respuesta 429 "demasiados intentos" | | |
| P04 | Permisos por rol | Con recepcionista abrir `/administracion` | "Sin permisos" | | |
| P05 | Permisos en el servidor | `curl` a `/api/usuarios` con token de recepcionista | 403 | | |
| P06 | Sin sesión | `curl http://localhost:8080/api/reservas` | 401 | | |
| P07 | Crear reserva (RF 04) | Nueva reserva, `{H}`, fechas futuras | 201, total = noches × tarifa, comprobante `CR-…` | | |
| P08 | Fechas inválidas | Entrada en el pasado o salida ≤ entrada | Rechazada con mensaje claro | | |
| P09 | Solapamiento | Reservar `{H}` en fechas que ya tiene reserva | 409, no se crea | | |
| P10 | Sin tarifa | Reservar en fechas sin tarifa vigente | Rechazada con mensaje claro | | |
| P11 | Habitación en mantenimiento | Reservar una habitación en MANTENIMIENTO | Rechazada | | |
| P12 | Modificar reserva (RF 05) | Cambiar fechas/habitación de una reserva futura | Se recalcula el valor; si no hay cupo, queda como estaba | | |
| P13 | Cancelar (RF 06) | Cancelar una reserva futura | Estado CANCELADA, la habitación se libera | | |
| P14 | Cancelar una que ya inició | Intentar cancelar una reserva en curso | Rechazada | | |
| P15 | Comprobante (RF 07) | Abrir/imprimir el comprobante | Muestra número, huésped, habitación, fechas, total | | |
| P16 | Historial con filtros (RF 03) | Filtrar por estado y por fechas | Solo aparecen las que cumplen | | |
| P17 | Estados de habitación | Crear reserva que cubra hoy | La habitación pasa a RESERVADA; al cancelar vuelve a DISPONIBLE | | |
| P18 | Reporte de ocupación | Reportes del mes | Porcentaje y noches coherentes con las reservas | | |
| P19 | Administración (RF 08–10) | Crear tipo, habitación y tarifa | Quedan guardados y se ven en la interfaz | | |
| P20 | Usuarios | Crear recepcionista como administrador del hotel | Creado; no puede crear administradores | | |
| P21 | Auditoría | Revisar *Auditoría* tras P07, P12, P13, P19 | Aparece cada acción con usuario, fecha, antes/después, IP | | |
| P22 | Concurrencia | `./pruebas/concurrencia.sh 10 {id} 2027-03-10 2027-03-12` | **1** respuesta 201 y **9** 409; `RESULTADO: OK` | | |
| P23 | Concurrencia en BD | `verificar_flujo.sql`, consulta 3 | Sin filas (ninguna reserva solapada) | | |
| P24 | Rendimiento (RNF 05) | `pruebas/p24_rendimiento.sh` (carga 100 hab. + 10.000 reservas, corre `rendimiento.py` y limpia) | Todos los escenarios dentro del umbral | 1.ª corrida: falla por pool de conexiones agotado (p95 mixta 5,3 s, 57 errores); se subió `maximum-pool-size` a 40. 2.ª corrida: mixta p95 0,414 s, 0 errores; concurrencia 1 reserva en 100 %. **Aprobado** | `evidencias/rendimiento_2026-11-20_1403.txt` (falla) y `rendimiento_2026-11-20_1412.txt` (cumple) |
| P25 | Sesión de 2 h | Arrancar con `JWT_MINUTOS=6` y esperar | Aviso a los 10 min (aquí al inicio), ventana de renovación a los 5, cierre al vencer | | |
| P26 | Recuperar contraseña | "¿Olvidaste tu contraseña?" → enlace del log | Cambia la clave; el enlace no sirve una segunda vez | | |
| P27 | IA: consentimiento | Primer mensaje al consultor | Pide `ACEPTO`; sin aceptar no responde con datos | **Aprobado**: sin autorización solo muestra el aviso; con ACEPTO responde disponibilidad; tras REVOCAR vuelve al aviso. `AutorizacionDatos` registra fecha, versión y revocación | Capturas del simulador (Nicolás, 3156929115) y consulta SQL |
| P28 | IA: disponibilidad | "¿Qué hay para 2 personas del … al …?" | Precio y total iguales a *Nueva reserva* | | |
| P29 | IA: mis reservas | Pedir el estado de la reserva dando el `CR-…` | Muestra solo reservas de ese huésped; con código ajeno o inventado no muestra nada | | |
| P30 | IA: derivación | "Quiero cambiar mi reserva" | Deriva a recepción y no modifica nada | | |
| P31 | IA: número no registrado | Mismo mensaje desde un teléfono sin huésped | No muestra reservas de nadie | | |
| P32 | IA: revocar | Escribir `REVOCAR` | Confirma; el siguiente mensaje vuelve a pedir autorización | **Aprobado** (ejecutado junto con P27): `revocada = 1` con su fecha | Captura de REVOCAR y consulta SQL |
| P33 | Notificación por correo | Crear, modificar y cancelar una reserva de un huésped con tu correo | Llegan 3 correos (confirmada, modificada, cancelada) con habitación, fechas, total y código | | |
| P34 | Notificación por WhatsApp | Lo mismo con un huésped cuyo teléfono es tu celular verificado | Llegan 3 mensajes de WhatsApp | | |
| P35 | Registro de notificaciones | Revisar *Auditoría* | Aparecen `NOTIFICACION_ENVIADA` (canal y destino enmascarado); un huésped sin correo/teléfono genera `NOTIFICACION_OMITIDA` | | |
| P36 | Recordatorio de llegada | Crear una reserva con entrada mañana; en *Sistema* pulsar "Enviar recordatorios ahora" | Llega el aviso "Te esperamos mañana…" por correo/WhatsApp; Auditoría muestra `NOTIFICACION_ENVIADA` con `RECORDATORIO` | | |
| P37 | Resumen diario | En *Sistema* pulsar "Enviar resumen ahora" | Los administradores reciben el correo con ocupación, llegadas y salidas de hoy | | |
| P38 | Alerta de tarifas | Desactivar o dejar vencer la tarifa de un tipo; abrir el inicio del administrador del hotel | Aparece el aviso rojo con el tipo y la fecha desde la que falta tarifa; al corregirla desaparece | | |
| P39 | Huésped duplicado | Registrar un huésped con documento, nombre completo, correo o teléfono ya existentes | Marca el campo repetido; la API responde 409 y no se crea | | |
| P40 | Editar huésped | En Huéspedes pulsar *Editar*, cambiar el teléfono y guardar | Se guarda; Auditoría registra `HUESPED_MODIFICADO` con los campos cambiados | | |
| P41 | Unicidad en la BD | Ejecutar `database/unicidad_huespedes.sql` e insertar en SQL un huésped con un correo existente | Se crean los índices `UX_Huesped_*`; el INSERT falla | **Aprobado** tras 2 hallazgos: (1) el script detectó 2 nombres duplicados previos y se detuvo sin crear índices (se corrigieron); (2) los índices filtrados fallaban en `sqlcmd` por `QUOTED_IDENTIFIER OFF` (se agregó `SET QUOTED_IDENTIFIER ON` a los scripts). Luego: 4 índices creados y el INSERT con correo repetido falla (Msg 2601, `UX_Huesped_Correo`) | Salidas de terminal de las 3 corridas |
| P42 | Buscador de huésped | En *Nueva reserva* escribir parte del nombre, documento o teléfono | Aparecen solo coincidencias; se elige con clic o teclado | | |
| P43 | Búsqueda, filtros y paginación | Usar buscador, filtros y páginas en Huéspedes, Reservas, Habitaciones, Administración y Auditoría | Las listas se filtran y paginan correctamente | | |
| P44 | Firma del webhook | `curl` POST a `/api/whatsapp/twilio` sin firma; verificación con token falso | "Firma inválida" (403) y "Verificación rechazada"; un mensaje firmado responde 200 | **Aprobado** con `pruebas/p44_firma_webhook.sh`: 6/6 (sin firma, firma falsa y cuerpo alterado → 403; firmado → 200 con TwiML; Meta token falso y POST sin firma → 403) | `evidencias/p44_firma_webhook_2026-11-20_1517.txt` y log del backend |
| P45 | Administrador general | Entrar como administrador general | Ve todas las funciones (incluidas las de recepción); inicio con 3 pestañas | | |

## Resumen para el informe
- Casos ejecutados: ___ de 45. Aprobados: ___. Fallidos: ___ (describir y cómo se corrigieron).
- Fecha, versión del código (commit) y equipo en el que se ejecutó.
