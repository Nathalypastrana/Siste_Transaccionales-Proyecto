# Servicio de Inteligencia Artificial: consultor virtual de reservas

**Ficha para la sustentación.** Responde las cinco preguntas del profesor. Detalle técnico en `asistente-ia.md`.

## 1. ¿Qué problema resuelve?
Los huéspedes preguntan por WhatsApp lo mismo una y otra vez: qué habitaciones hay en unas fechas, cuánto cuestan, cómo va su
reserva, cuál es la política de cancelación. Hoy lo contesta un recepcionista a mano. El consultor responde de inmediato,
a cualquier hora, con **datos reales del sistema**, y deja a recepción solo lo que requiere una persona (crear, cambiar o cancelar reservas).

## 2. ¿Cómo se integra?
- Canal: WhatsApp Business Platform (Cloud API) → webhook `POST /api/whatsapp/webhook` (valida la firma HMAC de Meta).
  Para pruebas y demostración hay un simulador con la misma lógica (pantalla *Consultor virtual*).
- Cerebro: un **modelo de lenguaje con llamada a funciones** (Google Gemini o Claude, intercambiables con `ASISTENTE_PROVEEDOR`)
  detrás de la interfaz `ModeloLenguaje`.
- Herramientas que el modelo puede pedir (solo lectura): `consultar_disponibilidad`, `consultar_mis_reservas`,
  `consultar_politica_cancelacion`, `derivar_a_recepcionista`. Ejecutan los **mismos servicios** que usa la API REST y leen SQL Server.
- Un **clasificador de intención** propio (red neuronal 6-4-5, ReLU + softmax, entrenada al arrancar) detecta si la consulta es de
  disponibilidad, reservas, cancelación, cambio o trato humano, y le da al modelo una pista en el prompt.

## 3. ¿Qué información procesa?
- El mensaje del huésped y el historial reciente de la conversación.
- Su **primer nombre** y cuántas reservas previas tiene (el huésped se reconoce por los últimos 10 dígitos del teléfono).
- Resultados de las herramientas: habitaciones libres, tarifa vigente, total estimado, estado de sus reservas.
- **No** se envían al modelo teléfono completo, documento, correo ni detalle de reservas ajenas. El teléfono se enmascara en los logs.
- Antes de procesar nada, el huésped debe aceptar el aviso de privacidad (`ACEPTO`); puede retirar su autorización con `REVOCAR`
  (Ley 1581 de 2012). Para ver reservas debe dar su **código de reserva** (`CR-XXXXXXXX`).

## 4. ¿Qué resultado genera?
Una respuesta en lenguaje natural, personalizada y con cifras exactas (por ejemplo: "Hola Lucía, para 2 personas del 5 al 8 de
diciembre hay habitación Doble a $180.000 por noche, $540.000 en total"). Cuando no puede o no debe resolver, genera una **derivación**:
un resumen de la consulta que se envía a recepción.

## 5. ¿Cómo usa el sistema ese resultado?
- La respuesta se devuelve al huésped por WhatsApp (o a la pantalla del simulador).
- Las derivaciones se avisan al número de recepción (`RECEPCION_WHATSAPP`) y quedan en el log para que un recepcionista actúe.
- Las cifras salen de las reglas del propio sistema (tarifas, disponibilidad), así que **coinciden con la pantalla *Nueva reserva***;
  el modelo no inventa precios ni disponibilidad.
- El consultor **nunca crea ni modifica reservas**: la decisión transaccional sigue en el recepcionista, con control de concurrencia y auditoría.
- Las autorizaciones de datos quedan en la tabla `AutorizacionDatos` y se resumen en la pantalla *Sistema* del administrador general.

## Evidencia a adjuntar en Canvas
- Capturas del Consultor virtual: (a) aviso de privacidad y `ACEPTO`, (b) disponibilidad con precio, (c) consulta de reserva con código,
  (d) derivación a recepción ("quiero cambiar mi reserva"), (e) número no registrado o sin autorización.
- Captura del log del backend con `Función solicitada por el modelo: ...` y `Clasificador de intención entrenado: 64/64`.
- Captura de la pantalla *Sistema* (proveedor de IA, autorizaciones) y de la tabla `AutorizacionDatos`.
