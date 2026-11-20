# Entrega final: prototipo, pruebas y servicio de IA

Documento guía para la socialización final y la entrega en Canvas. Reúne en un solo lugar el acceso al prototipo,
las evidencias de las pruebas y la evidencia del servicio de inteligencia artificial.

## 1. Acceso al prototipo

| Recurso | Enlace |
|---------|--------|
| Repositorio | https://github.com/Nathalypastrana/Siste_Transaccionales-Proyecto |
| Video de la demostración | _(pegar aquí el enlace de YouTube "no listado" o Google Drive)_ |
| Instalación y ejecución | [`README.md`](../README.md), secciones *Instalación* y *Ejecución* |

El prototipo se ejecuta localmente (SQL Server en Docker + backend Spring Boot + frontend React). Las cuentas de
prueba (recepcionista, administrador del hotel y administrador general) se entregan en el comentario de Canvas,
**no** en el repositorio.

## 2. Procesos transaccionales demostrados

| Proceso | Garantía transaccional |
|---------|------------------------|
| Crear reserva + comprobante | Una sola transacción con bloqueo pesimista de la habitación: o se guardan ambos o ninguno. Sin cruces de fechas. |
| Modificar reserva | Revalida disponibilidad y recalcula el total dentro de la transacción; auditoría con antes/después. |
| Cancelar reserva | Cambia el estado y libera la habitación en la misma transacción. |
| Registrar / editar huésped | Sin duplicados de documento, nombre completo, correo ni teléfono (interfaz + backend + índices únicos en SQL Server). |
| Notificaciones | Se envían solo después de confirmar la transacción (correo y WhatsApp); cada intento queda en Auditoría. |

## 3. Evidencias de las pruebas

Las salidas de texto quedan en `evidencias/` al ejecutar los scripts; las capturas se guardan en la misma carpeta.

| ID | Prueba | Cómo se genera | Evidencia |
|----|--------|----------------|-----------|
| E01–E04 | Crear, comprobante, modificar y cancelar reserva | Interfaz + `./pruebas/evidencia.sh <n.º reserva>` | `E0x_ui.png`, `E0x_bd.png`, `persistencia_*.txt` |
| E05 | Reserva solapada rechazada (409) | Interfaz | `E05_ui.png`, `E05_bd.png` |
| E06–E08 | Habitación, tarifa y usuario (contraseña en BCrypt) | Interfaz + `evidencia.sh` | `E06`–`E08` `_ui/_bd.png` |
| E09 | Notificación al huésped (correo y WhatsApp) | Crear o modificar una reserva | `E09_ui.png` (correo/WhatsApp), `E09_bd.png` (Auditoría) |
| E10 | Concurrencia: 10 solicitudes simultáneas | `./pruebas/concurrencia.sh 10 4 <entrada> <salida>` | `concurrencia_*.txt` (1 × 201, 9 × 409) |
| E11 | Flujo completo por API con verificación en BD | `./pruebas/evidencia_e2e.sh 1 4` | `e2e_*.txt` |
| E12 | Rendimiento con 10.000 reservas | `database/datos_rendimiento.sql` + `python3 pruebas/rendimiento.py` | salida con p95 por escenario |
| E13 | Huésped duplicado rechazado | Huéspedes: crear con correo o teléfono existente | `E13_ui.png` |
| E14 | Seguridad: bloqueo tras 5 intentos fallidos | Login | `E14_ui.png` + Auditoría `LOGIN_BLOQUEADO` |
| E15 | Seguridad del webhook (firma) | `curl` sin firma → `Firma inválida`; token falso → `Verificación rechazada` | `E15_terminal.png` |

Resultados detallados: [`plan-pruebas.md`](plan-pruebas.md) (38 casos) e [`informe-pruebas.md`](informe-pruebas.md).

## 4. Servicio de inteligencia artificial: consultor virtual

### ¿Qué problema resuelve?
Los huéspedes preguntan por WhatsApp, a cualquier hora, por disponibilidad, precios, el estado de su reserva o la
política de cancelación. El consultor responde al instante **con datos reales del sistema** y libera a recepción de las
consultas repetitivas; lo que no puede resolver lo deriva a una persona.

### ¿Cómo se integra?

```
Huésped (WhatsApp / simulador)
   │  mensaje
   ▼
Webhook /api/whatsapp/twilio o /api/whatsapp/webhook  ── valida la firma (HMAC) ──► rechaza si no es válida
   │
   ▼
AsistenteService ── autorización de datos (ACEPTO / REVOCAR, Ley 1581) y límite de 20 mensajes/hora
   │
   ├─► Red neuronal (MLP 6-4-5) ─► intención probable (pista para el modelo)
   │
   ▼
Gemini (tool use) ─► herramientas de SOLO LECTURA ─► mismos servicios del sistema transaccional
   │     consultar_disponibilidad · consultar_mis_reservas (exige código de comprobante)
   │     consultar_politica_cancelacion · derivar_a_recepcionista
   ▼
Respuesta en lenguaje natural ─► WhatsApp del huésped   (derivación ─► aviso a recepción)
```

El modelo **nunca accede a la base de datos**: solo puede pedir las herramientas que la aplicación define, y estas usan
los mismos servicios (y las mismas reglas) que la API REST.

### ¿Qué información procesa?
- El texto del mensaje: fechas, número de personas, tipo de habitación, código de comprobante si pregunta por su reserva.
- El teléfono, solo para identificar al huésped dentro del sistema (no se envía al modelo).
- Al modelo de IA solo llega el **primer nombre** y la conversación: nunca documento, correo ni teléfono.
- Antes de procesar datos personales se pide autorización expresa (`ACEPTO`), que se puede revocar (`REVOCAR`).

### ¿Qué resultado genera?
- La **red neuronal** clasifica la intención en una de cinco: disponibilidad, tarifas, estado de reserva, política de
  cancelación o hablar con recepción (entrenamiento al iniciar: 64/64 combinaciones correctas).
- **Gemini** produce una respuesta en lenguaje natural con los datos reales que devolvieron las herramientas:
  habitaciones libres y precio total, estado de una reserva o condiciones de cancelación; o una **derivación a recepción**.

### ¿Cómo utiliza el sistema dicho resultado?
- La respuesta se envía al huésped por WhatsApp (o se muestra en el simulador del *Consultor virtual*).
- Las derivaciones generan un aviso a recepción (`RECEPCION_WHATSAPP`) para que una persona continúe la atención.
- La IA **solo informa**: crear, modificar o cancelar una reserva siempre pasa por la transacción que realiza el
  personal, por lo que la integridad de las reservas nunca depende de la IA.

### Evidencias del servicio de IA

| ID | Evidencia | Archivo |
|----|-----------|---------|
| IA-01 | Entrenamiento de la red neuronal en el log (`Clasificador de intención entrenado: 64/64`) | `IA01_log.png` |
| IA-02 | Simulador: aviso de privacidad y `ACEPTO` | `IA02_autorizacion.png` |
| IA-03 | Simulador: disponibilidad para 2 personas en unas fechas, igual a lo que muestra *Nueva reserva* | `IA03_disponibilidad.png` + `IA03_nueva_reserva.png` |
| IA-04 | Simulador: "quiero cambiar mi reserva" → derivación a recepción | `IA04_derivacion.png` |
| IA-05 | WhatsApp real (Twilio): mensaje entrante en ngrok (200) y respuesta TwiML generada | `IA05_ngrok.png` |
| IA-06 | Notificación de reserva recibida por WhatsApp y registrada en Auditoría | `IA06_whatsapp.png` |

Documentación técnica: [`asistente-ia.md`](asistente-ia.md) y [`ficha-ia.md`](ficha-ia.md).

### Limitaciones de las cuentas de prueba
- **Meta (WhatsApp Cloud API):** una cuenta nueva queda restringida hasta verificar legalmente el negocio
  (error `131031`); la integración llega hasta la API de Meta y los mensajes son aceptados.
- **Twilio (número de prueba):** solo permite enviar plantillas preaprobadas por Twilio, por eso la notificación usa una
  plantilla de ejemplo y el detalle completo de la reserva llega por correo. En producción se registra una plantilla
  propia sin cambiar el código.

## 5. Lista de verificación para Canvas

- [ ] Enlace del repositorio (actualizado) y del video de la demostración.
- [ ] Cuentas de prueba en el comentario de la entrega.
- [ ] Carpeta `evidencias/` con E01–E15 (capturas y salidas de texto).
- [ ] `plan-pruebas.md` e `informe-pruebas.md` completos (exportados a PDF).
- [ ] Evidencias IA-01 a IA-06, `asistente-ia.md` y `ficha-ia.md`.
- [ ] Revisar que ninguna captura muestre contraseñas, tokens ni teléfonos completos.
