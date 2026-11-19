# Evidencia de ejecución: registro desde la interfaz y persistencia en la base de datos

Objetivo: demostrar, para cada operación, que lo que se hace en la interfaz **queda guardado en SQL Server** y que se puede rastrear.
Trazabilidad: el número de reserva que muestra la interfaz = `Reserva.idReserva` = `Auditoria.entidadId`.

## Cómo generar la evidencia (≈10 minutos)
1. Backend y frontend corriendo (ver README). En otra terminal: `export DB_PASSWORD='…'` (y `DB_USER` si no es `sa`).
2. Haz la operación en la interfaz y **captura la pantalla** (muestra el número de reserva y la hora del equipo).
3. Inmediatamente ejecuta `./pruebas/evidencia.sh <número de reserva>` y **captura la terminal** (queda además guardado en `evidencias/persistencia_<fecha>.txt`).
4. Opcional, automatizado por API: `./pruebas/evidencia_e2e.sh <idHuésped> <idHabitación>` (crea, modifica y cancela, y consulta la BD tras cada paso; queda en `evidencias/e2e_<fecha>.txt`).

## Tabla de evidencias (completar con las capturas)
| Id | Operación en la interfaz | Captura de la interfaz | Qué debe verse en la BD (sección de `consultas_evidencia.sql`) | Captura BD |
|----|--------------------------|------------------------|-----------------------------------------------------------------|------------|
| E01 | Crear reserva | `E01_ui.png` | §1–2: fila nueva en `Reserva`, estado `PENDIENTE`/`CONFIRMADA`, `valorTotal` = noches × tarifa, `registrada_por` = usuario logueado; §4: `RESERVA_CREADA` | `E01_bd.png` |
| E02 | Comprobante | `E02_ui.png` | §3: fila en `Comprobante` con `numeroComprobante` igual al de pantalla; §4: `COMPROBANTE_GENERADO` | `E02_bd.png` |
| E03 | Modificar reserva | `E03_ui.png` | §2: fechas y valor actualizados; §4: `RESERVA_MODIFICADA` con "Antes/Ahora" | `E03_bd.png` |
| E04 | Cancelar reserva | `E04_ui.png` | §2: estado `CANCELADA`; §6: habitación liberada; §4: `RESERVA_CANCELADA` | `E04_bd.png` |
| E05 | Reserva solapada rechazada | `E05_ui.png` | §9: cruces = 0; no hay fila nueva en §1 | `E05_bd.png` |
| E06 | Crear habitación / cambiar estado | `E06_ui.png` | §6: la habitación y su estado | `E06_bd.png` |
| E07 | Crear tarifa | `E07_ui.png` | §7: fila nueva en `Tarifa` | `E07_bd.png` |
| E08 | Crear usuario | `E08_ui.png` | §8: `almacenamiento = BCrypt`, prefijo `$2a/$2b`, longitud 60 (la contraseña no se guarda en claro) | `E08_bd.png` |
| E09 | Notificación al cliente | `E09_ui.png` (mensaje/WhatsApp/correo recibido) | §5: acciones de notificación en `Auditoria` | `E09_bd.png` |
| E10 | Concurrencia | salida de `concurrencia.sh` (1×201, 9×409) | §9: cruces = 0 | `E10_bd.png` |

## Criterios de aceptación
- La reserva creada en la interfaz aparece en BD con el mismo número, huésped, habitación, fechas y total.
- Cada operación tiene su fila en `Auditoria` con usuario, fecha e IP.
- `cruces = 0` en todo momento; las contraseñas aparecen siempre como `BCrypt`.
