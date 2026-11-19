# Notificaciones de reservas al huésped (correo y/o WhatsApp)

Cuando se **crea, modifica o cancela** una reserva, el huésped recibe un aviso con habitación, fechas, total y código de reserva.
Canales con `NOTIFICACIONES_CANALES`: `correo,whatsapp` (por defecto, ambos), `correo` o `whatsapp`.
Se envía al correo y al teléfono registrados en el huésped (si falta uno, ese canal se omite). El aviso sale solo cuando la
reserva ya quedó guardada y no demora la respuesta al recepcionista. Cada intento queda en **Auditoría**
(`NOTIFICACION_ENVIADA`, `NOTIFICACION_FALLIDA`, `NOTIFICACION_OMITIDA`) con el canal y el destino enmascarado.

## Variables

| Variable | Para qué |
|---|---|
| `NOTIFICACIONES_CANALES` | `correo,whatsapp` / `correo` / `whatsapp` |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `SMTP_FROM` | Servidor de correo |
| `WHATSAPP_ACCESS_TOKEN`, `WHATSAPP_PHONE_NUMBER_ID` | Cuenta de WhatsApp Cloud API |
| `WHATSAPP_PLANTILLA_RESERVA`, `WHATSAPP_IDIOMA_PLANTILLA` | Plantilla aprobada (ver abajo) |
| `INDICATIVO_PAIS` | Indicativo que se antepone a teléfonos de 10 dígitos (57) |
| `NOMBRE_HOTEL` | Nombre que aparece en el asunto y el texto del correo |

## Correo con Gmail (para pruebas y demostración)
1. Cuenta de Google → Seguridad → activar la verificación en 2 pasos.
2. Seguridad → **Contraseñas de aplicaciones** → crear una (16 letras; úsala sin espacios).
3. Exportar antes de arrancar el backend:
```
export SPRING_MAIL_HOST='smtp.gmail.com'
export SPRING_MAIL_PORT='587'
export SPRING_MAIL_USERNAME='tu-correo@gmail.com'
export SPRING_MAIL_PASSWORD='la-contraseña-de-aplicación'
export SMTP_FROM='tu-correo@gmail.com'
```
Sin `SPRING_MAIL_HOST` el correo no sale: el texto solo se escribe en el log del backend y la auditoría marca `NOTIFICACION_FALLIDA`.

## WhatsApp (Cloud API de Meta)
**Regla de Meta:** a un cliente que no ha escrito en las últimas 24 horas solo se le puede enviar una **plantilla aprobada**.
Por eso las notificaciones usan una plantilla (`WHATSAPP_PLANTILLA_RESERVA`). Si la dejas vacía se envía texto libre, que solo
llega si el cliente te escribió en las últimas 24 horas.

1. developers.facebook.com → **Mis apps → Crear app** → tipo *Negocios* → agregar el producto **WhatsApp**.
2. WhatsApp → **Configuración de la API**: ahí aparecen el **número de prueba**, el **Identificador del número de teléfono**
   (`WHATSAPP_PHONE_NUMBER_ID`) y el **token de acceso temporal** (`WHATSAPP_ACCESS_TOKEN`, dura 24 horas).
3. En "Para", agrega tu celular (y los de quien vaya a probar) y confirma con el código que llega por WhatsApp.
   Con el número de prueba **solo se puede escribir a esos números verificados** (hasta 5). Para escribir a cualquier cliente
   hay que registrar un número propio y verificar el negocio en Meta.
4. WhatsApp Manager → **Plantillas de mensajes → Crear**: categoría *Utilidad*, nombre `notificacion_reserva`, idioma Español, cuerpo:
   `Hola {{1}}. Novedad de tu reserva: {{2}} Gracias por elegirnos.` (con ejemplos para {{1}} y {{2}}). Esperar la aprobación.
5. Exportar y arrancar:
```
export WHATSAPP_ACCESS_TOKEN='...'
export WHATSAPP_PHONE_NUMBER_ID='...'
export WHATSAPP_PLANTILLA_RESERVA='notificacion_reserva'
```
Para probar **antes** de que apruebe la plantilla: deja `WHATSAPP_PLANTILLA_RESERVA` vacía, escríbele un "hola" desde tu celular al número
de prueba (abre 24 horas de ventana) y crea una reserva con tu teléfono.

El huésped debe tener el teléfono con indicativo o de 10 dígitos (se le antepone `57`), y el correo, en su ficha.
Si quieres que el consultor reciba mensajes (webhook), faltan además `WHATSAPP_VERIFY_TOKEN`, `WHATSAPP_APP_SECRET` y un túnel (ngrok) con la URL
`https://<tu-túnel>/api/whatsapp/webhook`, suscrita al campo `messages`; no hace falta para las notificaciones.

## Avisos programados (todos los días, hora de Bogotá)

| Hora | Aviso | Destino |
|---|---|---|
| 7:00 | **Resumen del día**: ocupación, llegadas y salidas de hoy, llegadas de mañana y tarifas por vencer | Correo a los administradores activos (hotel y general) |
| 9:00 | **Recordatorio de llegada** a los huéspedes que entran mañana (reservas confirmadas o pendientes) | Correo y/o WhatsApp del huésped, según `NOTIFICACIONES_CANALES` |

- El recordatorio no se repite a la misma reserva (ni siquiera si el servidor se reinicia): se consulta la auditoría de las últimas 20 horas.
- **Alerta de tarifas por vencer:** revisa los próximos 15 días (`NOTIFICACIONES_DIAS_ALERTA_TARIFA`) para cada tipo de habitación con habitaciones en servicio. Si a un
  tipo le falta tarifa vigente, aparece un aviso rojo en el inicio del administrador del hotel, en Sistema y en Administración → Tarifas, y se incluye en el resumen diario.
- **Probarlos sin esperar:** el administrador general tiene en la pantalla *Sistema* los botones "Enviar resumen ahora" y "Enviar recordatorios ahora"
  (`POST /api/sistema/tareas/resumen` y `/recordatorios`). Quedan en Auditoría como `TAREA_MANUAL` y los envíos como `NOTIFICACION_ENVIADA` / `RESUMEN_DIARIO`.
- Para el recordatorio de WhatsApp se usa la misma plantilla aprobada de las notificaciones (variables: nombre y resumen).
