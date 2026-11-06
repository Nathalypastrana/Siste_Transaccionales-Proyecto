# Consultor virtual de reservas (IA + WhatsApp)

Asistente conversacional que atiende a los huéspedes por WhatsApp con datos reales del sistema.
Usa un **modelo de lenguaje con llamada a herramientas** (Claude o Google Gemini, a elegir) y la **WhatsApp Business Platform (Cloud API)**.

## Flujo y dónde está cada paso

| # | Paso | Código |
|---|------|--------|
| 1 | El huésped escribe por WhatsApp | (Meta) |
| 2 | Meta entrega el mensaje al webhook | `whatsapp/WhatsAppWebhookController` (valida la firma HMAC) |
| 3 | Se identifica al huésped por su teléfono y se envía la pregunta con su contexto | `asistente/AsistenteService` + `HuespedService.buscarPorTelefono` |
| 4 | El modelo solicita una herramienta | `asistente/modelo/ClaudeClient` o `GeminiClient` (bucle de herramientas) |
| 5 | La aplicación ejecuta la herramienta | `asistente/HerramientasReservas` → `HabitacionService`, `ReservaService` |
| 6 | Los servicios leen SQL Server | repositorios JPA (los mismos que usa la API REST) |
| 7 | El modelo redacta la respuesta personalizada | `ClaudeClient` / `GeminiClient` |
| 8 | Se envía por WhatsApp | `whatsapp/WhatsAppClient` |

Las herramientas llaman a los **servicios** que respaldan la API REST, no a la API por HTTP: se reutilizan las
mismas reglas y datos sin una llamada de red a sí mismos.

El resto del sistema solo conoce la interfaz `ModeloLenguaje`. Hay dos implementaciones, `ClaudeClient` y `GeminiClient`, y se elige con `ASISTENTE_PROVEEDOR`
(`claude` por defecto, o `gemini`). El webhook, las herramientas y la API no cambian.

## Herramientas (todas de solo lectura)

| Herramienta | Para qué |
|-------------|----------|
| `consultar_disponibilidad` | Tipos de habitación libres entre dos fechas, con tarifa vigente y total estimado (filtra por número de personas) |
| `consultar_mis_reservas` | Estado de las reservas del huésped que escribe |
| `consultar_politica_cancelacion` | Política de cancelación (configurable) |
| `derivar_a_recepcionista` | Pasa la consulta a una persona y avisa a recepción |

El consultor **no crea, modifica ni cancela reservas**: eso lo confirma el recepcionista.

## Personalización

El prompt incluye el nombre del huésped, cuántas reservas no canceladas tuvo y la fecha de su última reserva.
Si el número no está registrado, no se usa nombre ni se ofrecen datos de reservas, pero sí disponibilidad y precios.
Responde en el idioma del huésped y, si no puede resolver algo con las herramientas, lo deriva.

## Decisiones de seguridad

- **El huésped sale del número de WhatsApp, no del texto del mensaje.** Ninguna herramienta recibe un id de huésped
  como parámetro, así que nadie puede consultar reservas ajenas escribiendo otro nombre o número.
- **Webhook firmado:** el POST solo se acepta con `X-Hub-Signature-256` válida (HMAC-SHA256 con el secreto de la app).
  Sin `WHATSAPP_APP_SECRET` se rechaza todo.
- **Claves solo por variables de entorno**, nunca en el repositorio.
- **Control de costos:** máximo 20 mensajes por hora por número, mensajes de hasta 1000 caracteres, máximo 5 llamadas
  de herramientas por consulta y 1024 tokens por respuesta.
- **Simulador desactivado por defecto**: consulta reservas por teléfono sin autenticación, solo para desarrollo.
- Los mensajes del huésped se tratan como consultas: el prompt indica no cambiar las reglas por lo que pidan.

## Variables de entorno

| Variable | Uso |
|----------|-----|
| `ASISTENTE_PROVEEDOR` | `claude` (por defecto) o `gemini` |
| `GEMINI_API_KEY` | Clave de Google AI Studio (https://aistudio.google.com/apikey). Tiene nivel gratuito |
| `GEMINI_MODELO` | Modelo de Gemini (por defecto `gemini-2.5-flash`; `gemini-2.5-flash-lite` si se agota la cuota) |
| `ANTHROPIC_API_KEY` | Clave de la API de Claude (https://console.anthropic.com). Es de pago por uso |
| `ANTHROPIC_MODELO` | Modelo de Claude (por defecto `claude-haiku-5-5`) |
| `ASISTENTE_SIMULADOR` | `true` para activar el simulador (solo desarrollo) |
| `WHATSAPP_VERIFY_TOKEN` | Texto que tú eliges; Meta lo envía al verificar el webhook |
| `WHATSAPP_APP_SECRET` | Secreto de la app de Meta (valida la firma de los mensajes) |
| `WHATSAPP_ACCESS_TOKEN` | Token para enviar mensajes |
| `WHATSAPP_PHONE_NUMBER_ID` | Id del número de WhatsApp Business |
| `RECEPCION_WHATSAPP` | (Opcional) número de recepción, con indicativo y solo dígitos, que recibe las derivaciones |

La política de cancelación se ajusta con la propiedad `asistente.politica-cancelacion`. Por defecto describe lo que el
sistema aplica hoy (cancelar reservas pendientes o confirmadas antes de la fecha de entrada): cámbiala por la del hotel.

## Usar Gemini (nivel gratuito)

```
export ASISTENTE_PROVEEDOR=gemini
export GEMINI_API_KEY='clave-de-ai-studio'
```

Advertencias:
- **Privacidad:** en el nivel gratuito Google puede usar los mensajes para mejorar sus productos. El prompt lleva el nombre
  y las reservas del huésped, así que con el nivel gratuito usa solo datos de prueba; con huéspedes reales conviene el nivel de pago.
- **Límites:** el nivel gratuito tiene límites de peticiones por minuto y por día que cambian; consúltalos en AI Studio.
  Si se superan, la API responde 429 y el consultor contesta que un recepcionista contactará al huésped.
- El adaptador sigue la documentación de *function calling* de Gemini, pero no se ha probado contra la API real desde el entorno de desarrollo.

## Probar sin WhatsApp (simulador)

```
export ASISTENTE_PROVEEDOR=gemini   # o claude
export GEMINI_API_KEY='...'         # o ANTHROPIC_API_KEY
export ASISTENTE_SIMULADOR=true
cd backend && mvn spring-boot:run
```

En la interfaz: **Consultor virtual** (elige un huésped con teléfono registrado y escribe). O por API:

```
curl -X POST http://localhost:8080/api/asistente/simular \
  -H "Content-Type: application/json" \
  -d '{"telefono":"3001234567","mensaje":"¿Qué habitaciones hay para 2 personas del 2026-12-05 al 2026-12-08?"}'
```

El teléfono se compara por sus últimos 10 dígitos, de modo que `573001234567` (formato de WhatsApp) coincide con
`300 123 4567` guardado en el huésped.

Casos para comprobar:
1. Disponibilidad y precio con fechas (debe coincidir con la pantalla *Nueva reserva*).
2. "¿Cuál es el estado de mi reserva?" con un huésped que tenga reservas.
3. El mismo mensaje desde un número no registrado: no debe mostrar reservas.
4. "Quiero cambiar mi reserva": debe derivar a recepción.
5. Sin clave del proveedor elegido: responde que un recepcionista contactará al huésped.

## Conectar WhatsApp real (Meta)

1. En https://developers.facebook.com crear una app de tipo **Business** y agregar el producto **WhatsApp**.
2. En *WhatsApp → API Setup* aparece un **número de prueba**, su **Phone number ID** y un **token temporal** (dura ~24 h).
   El número de prueba solo envía a hasta 5 destinatarios que verifiques ahí.
3. En *App settings → Basic* copiar el **App Secret**.
4. Exponer el backend con una URL HTTPS pública durante el desarrollo, por ejemplo `ngrok http 8080`.
5. En *WhatsApp → Configuration → Webhook*: URL `https://<tu-dominio>/api/whatsapp/webhook` y como *Verify token* el valor de
   `WHATSAPP_VERIFY_TOKEN`. Meta hace un GET de verificación; después suscribirse al campo **messages**.
6. Arrancar el backend con las variables definidas y escribir al número de prueba desde WhatsApp.

Los mensajes de texto libre se pueden responder dentro de las 24 h posteriores al último mensaje del huésped.

## Limitaciones actuales

- El historial de conversación y el límite por hora viven en memoria: se pierden al reiniciar el backend.
- Solo se procesan mensajes de texto.
- No hay login (JWT pendiente): por eso el simulador está apagado por defecto.
- No hay pruebas automáticas contra la API del modelo (Claude/Gemini) ni contra Meta; deben verificarse manualmente con las credenciales propias.
