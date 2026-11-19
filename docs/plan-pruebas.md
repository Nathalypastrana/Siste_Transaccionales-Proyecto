# Plan de pruebas y resultados

**Versión (commit):** `________` · **Fecha:** `________` · **Ejecutó:** `________` · **Entorno:** macOS, Java 17, SQL Server en Docker, Node `____`.
Informe consolidado: `informe-pruebas.md`. Evidencia de persistencia UI → BD: `evidencia-persistencia.md`.
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
| P24 | Rendimiento (RNF 05) | Cargar `datos_rendimiento.sql` y correr `pruebas/rendimiento.py` | Todos los escenarios dentro del umbral | | |
| P25 | Sesión de 2 h | Arrancar con `JWT_MINUTOS=6` y esperar | Aviso a los 10 min (aquí al inicio), ventana de renovación a los 5, cierre al vencer | | |
| P26 | Recuperar contraseña | "¿Olvidaste tu contraseña?" → enlace del log | Cambia la clave; el enlace no sirve una segunda vez | | |
| P27 | IA: consentimiento | Primer mensaje al consultor | Pide `ACEPTO`; sin aceptar no responde con datos | | |
| P28 | IA: disponibilidad | "¿Qué hay para 2 personas del … al …?" | Precio y total iguales a *Nueva reserva* | | |
| P29 | IA: mis reservas | Pedir el estado de la reserva dando el `CR-…` | Muestra solo reservas de ese huésped; con código ajeno o inventado no muestra nada | | |
| P30 | IA: derivación | "Quiero cambiar mi reserva" | Deriva a recepción y no modifica nada | | |
| P31 | IA: número no registrado | Mismo mensaje desde un teléfono sin huésped | No muestra reservas de nadie | | |
| P32 | IA: revocar | Escribir `REVOCAR` | Confirma; el siguiente mensaje vuelve a pedir autorización | | |
| P33 | Notificación por correo | Crear, modificar y cancelar una reserva de un huésped con tu correo | Llegan 3 correos (confirmada, modificada, cancelada) con habitación, fechas, total y código | | |
| P34 | Notificación por WhatsApp | Lo mismo con un huésped cuyo teléfono es tu celular verificado | Llegan 3 mensajes de WhatsApp | | |
| P35 | Registro de notificaciones | Revisar *Auditoría* | Aparecen `NOTIFICACION_ENVIADA` (canal y destino enmascarado); un huésped sin correo/teléfono genera `NOTIFICACION_OMITIDA` | | |
| P36 | Recordatorio de llegada | Crear una reserva con entrada mañana; en *Sistema* pulsar "Enviar recordatorios ahora" | Llega el aviso "Te esperamos mañana…" por correo/WhatsApp; Auditoría muestra `NOTIFICACION_ENVIADA` con `RECORDATORIO` | | |
| P37 | Resumen diario | En *Sistema* pulsar "Enviar resumen ahora" | Los administradores reciben el correo con ocupación, llegadas y salidas de hoy | | |
| P38 | Alerta de tarifas | Desactivar o dejar vencer la tarifa de un tipo; abrir el inicio del administrador del hotel | Aparece el aviso rojo con el tipo y la fecha desde la que falta tarifa; al corregirla desaparece | | |

## Resumen para el informe
- Casos ejecutados: ___ de 38. Aprobados: ___. Fallidos: ___ (describir y cómo se corrigieron).
- Fecha, versión del código (commit) y equipo en el que se ejecutó.
