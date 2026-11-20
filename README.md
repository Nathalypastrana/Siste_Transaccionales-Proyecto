# Sistema transaccional para la gestión de reservas de un hotel

Aplicación web para registrar huéspedes y gestionar reservas de habitaciones del hotel **En mi pueblito**. Su objetivo
central es **garantizar que no existan reservas superpuestas**, incluso cuando varias solicitudes llegan al mismo tiempo.
Incluye control de acceso por roles, auditoría, notificaciones por correo y WhatsApp, y un **consultor virtual con IA**
que atiende a los huéspedes.

## Tecnologías

| Capa | Tecnología |
|------|------------|
| Interfaz | React 18 + TypeScript + Vite |
| API / lógica de negocio | Java 17 + Spring Boot 3 (Spring Web, Validation, Security, Mail) |
| Acceso a datos | Spring Data JPA + Hibernate |
| Base de datos | SQL Server 2019+ (contenedor Docker) |
| Consultor virtual (IA) | API de Gemini (Google) con *tool use* de solo lectura + red neuronal (MLP 6-4-5) para clasificar la intención |
| Mensajería | Correo SMTP (Gmail) · WhatsApp: Meta Cloud API o Twilio (`WHATSAPP_PROVEEDOR`) |

## Estructura del repositorio

```
├── backend/     # API REST (controller → service → repository → model), seguridad, IA, notificaciones, WhatsApp
├── frontend/    # Interfaz React (pages, components, api.ts)
├── database/    # Scripts SQL: esquema, datos, seguridad, unicidad, rendimiento, verificación
├── pruebas/     # Concurrencia, rendimiento y evidencias de persistencia
├── evidencias/  # Salidas de las pruebas (se generan con los scripts de pruebas/)
└── docs/        # Guías: verificación, plan e informe de pruebas, seguridad, notificaciones, IA
```

Separación de responsabilidades: la **interfaz** (`frontend/src/pages`) presenta y valida; la **lógica de negocio**
vive en `backend/.../service`; el **acceso a datos** en `backend/.../repository`; y la **conexión a la base** en
`backend/.../config/DatabaseConnection.java`, configurada desde `application.properties` y variables de entorno.

## Requisitos

- **JDK 17** o superior (el proyecto trae `./mvnw`, no hace falta instalar Maven)
- **Node.js 18** o superior
- **SQL Server** 2019 o superior (puede ser un contenedor Docker)
- Git

## Instalación y configuración

### 1. Base de datos

Con SQL Server en ejecución (ejemplo con un contenedor llamado `sqlserver`; la clave de `sa` va en `SA_PASSWORD`):

```bash
read -s SA_PASSWORD && export SA_PASSWORD
for f in schema.sql datos_prueba.sql; do
  docker cp database/$f sqlserver:/tmp/$f
  docker exec -e SQLCMDPASSWORD="$SA_PASSWORD" sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -C -i /tmp/$f
done
```

| Script | Para qué |
|--------|----------|
| `schema.sql` | Crea la base `ReservasHotelDB`, tablas, restricciones e índices. **No borra datos**; se puede repetir. |
| `datos_prueba.sql` | Datos mínimos: huéspedes, habitaciones, tarifas y usuarios de prueba. |
| `datos_demostracion.sql` | (Opcional) 50 huéspedes, 15 habitaciones de 5 tipos, tarifas 2026-2027 y 52 reservas con comprobantes. Datos inventados. |
| `crear_usuario_aplicacion.sql` | Crea el login `hotel_app` con permisos mínimos (lectura/escritura). La aplicación **no** debe usar `sa`. |
| `asegurar_credenciales.sql` | Desactiva cuentas con contraseña en texto plano (todas deben estar en BCrypt). |
| `unicidad_huespedes.sql` | Para bases existentes: normaliza datos, informa duplicados y crea los índices únicos de huéspedes. |
| `datos_rendimiento.sql` | 100 habitaciones y 10.000 reservas para la prueba de rendimiento (trae su bloque de limpieza). |
| `reiniciar_base.sql` | Elimina **todas** las tablas y sus datos. Solo en desarrollo. |

Para `crear_usuario_aplicacion.sql`: `... sqlcmd ... -i /tmp/crear_usuario_aplicacion.sql -v APP_DB_PASSWORD="<clave>"`.

### 2. Variables de entorno

Copia la plantilla y completa los valores (el archivo `.env` está en `.gitignore`; **nunca** se sube):

```bash
cp .env.example .env
openssl rand -base64 36    # valor para JWT_SECRET
```

Los valores con espacios van entre comillas simples (`NOMBRE_HOTEL='Hotel En mi pueblito'`) y no debe haber
espacios alrededor del `=`.

| Variable | Uso |
|----------|-----|
| `DB_USER`, `DB_PASSWORD` | **Obligatorias**, sin valor por defecto. Recomendado `hotel_app`. |
| `DB_URL` | Por defecto `jdbc:sqlserver://localhost:1433;databaseName=ReservasHotelDB;encrypt=true;trustServerCertificate=true` |
| `JWT_SECRET`, `JWT_MINUTOS` | Firma de las sesiones (≥ 32 caracteres) y duración (120 min por defecto). |
| `ADMIN_CORREO`, `ADMIN_PASSWORD` | Crean el primer administrador general si no existe ninguno. Después se dejan vacías. |
| `MIGRAR_CLAVES_PLANAS` | Por defecto `false`. En `true` convierte a BCrypt contraseñas antiguas en texto plano al iniciar sesión. |
| `ASISTENTE_PROVEEDOR`, `GEMINI_API_KEY`, `GEMINI_MODELO`, `ASISTENTE_SIMULADOR` | Consultor de IA. El simulador solo en desarrollo o demostración. |
| `SPRING_MAIL_*`, `SMTP_FROM`, `APP_URL` | Correo (recuperación de contraseña y notificaciones). |
| `NOTIFICACIONES_CANALES` | `correo`, `whatsapp`, `correo,whatsapp` o vacío (sin notificaciones). |
| `WHATSAPP_PROVEEDOR` | `meta` (por defecto) o `twilio`. Ver [`docs/notificaciones.md`](docs/notificaciones.md). |
| `WHATSAPP_*` | Meta Cloud API: token, ID del número, token de verificación, App Secret, plantilla. |
| `TWILIO_*` | Twilio: Account SID, Auth Token, número de WhatsApp y URL pública (ngrok) del backend. |

### 3. Frontend

```bash
cd frontend && npm install
```

Si el backend no está en `http://localhost:8080`, crear `frontend/.env.local` con `VITE_API_URL=http://host:puerto`.

## Ejecución

```bash
# Terminal 1 - API (puerto 8080)
cd backend
set -a; source ../.env; set +a
./mvnw spring-boot:run

# Terminal 2 - Interfaz (puerto 5173)
cd frontend
npm run dev
```

Abrir http://localhost:5173 (página pública) y entrar por **Acceso del personal**. Para WhatsApp con webhooks hace
falta un túnel público: `ngrok http 8080`.

## Roles

| Rol | Qué puede hacer | Pantalla de inicio |
|-----|-----------------|--------------------|
| Recepcionista | Reservas (crear, modificar, cancelar, comprobante), huéspedes, consulta de habitaciones y consultor virtual | Llegadas y salidas del día |
| Administrador del hotel | Todo lo de recepción + reportes, tipos de habitación, habitaciones, tarifas y cuentas de recepcionistas | Ocupación del mes |
| Administrador general | **Todas** las funciones anteriores + cuentas de cualquier rol, auditoría y estado del sistema | Pestañas: estado del sistema, panel del hotel y panel de recepción |

Los permisos se validan en el backend (`@PreAuthorize`), no solo ocultando opciones en la interfaz.

## Funcionalidades

| Pantalla | Qué permite |
|----------|-------------|
| Nueva reserva | **Buscador de huésped** (nombre, documento, teléfono o correo), fechas, solo habitaciones libres, cálculo del total y confirmación |
| Reservas | Buscar y filtrar por estado y fechas; modificar, cancelar (con confirmación) y ver el comprobante |
| Huéspedes | Registrar y **editar** huéspedes; **buscador**, filtros por tipo de documento y datos de contacto, y orden |
| Habitaciones | Buscador y filtros por estado, tipo, piso y capacidad mínima |
| Administración | Tipos, habitaciones, tarifas y usuarios (con buscador y filtros por rol y estado) |
| Reportes | Ocupación por periodo y tipo, reservas por estado |
| Auditoría | Buscador, filtros por acción y fechas |
| Consultor virtual | Simulador del chat de WhatsApp con el asistente de IA |

- **Paginación** en todas las listas (registros por página configurables; Auditoría empieza en 5).
- **Estados legibles** (Disponible, En mantenimiento, Confirmada…); en la base de datos y la API siguen en mayúsculas.
- Avisos de éxito o error en cada operación, ruta de navegación, página 404 y botón **Reintentar** sin conexión.

### Huéspedes sin duplicados

No pueden existir dos huéspedes con el mismo **número de documento**, el mismo **nombre completo**, el mismo **correo**
ni el mismo **teléfono**. La regla se aplica en tres niveles:

1. **Interfaz:** marca el campo repetido y dice con qué huésped choca, antes de enviar.
2. **Backend:** compara sin distinguir mayúsculas, tildes ni espacios (teléfono por sus últimos 10 dígitos) y responde `409`.
3. **Base de datos:** índices únicos `UX_Huesped_*` (correo y teléfono filtrados para permitir vacíos). Garantizan la
   regla aunque lleguen dos registros al mismo tiempo. En bases existentes: `database/unicidad_huespedes.sql`.

## Validación y manejo de errores

- **Interfaz:** campos obligatorios, formato de documento, teléfono y correo, fechas coherentes y duplicados.
- **API:** Bean Validation; respuestas `400` (datos inválidos), `401`/`403` (sin sesión o sin permiso), `404` (no existe),
  `409` (conflicto: habitación ya reservada, dato duplicado), `503` (base de datos sin conexión). Nunca se muestran
  detalles internos.

## API (principales rutas)

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/api/auth/login`, `/api/auth/renovar`, `/api/auth/olvide`, `/api/auth/restablecer` | Sesión y recuperación de contraseña |
| GET | `/api/salud` | Estado de la conexión con la base de datos |
| GET | `/api/habitaciones`, `/api/habitaciones/disponibles?entrada=&salida=` | Habitaciones y disponibilidad |
| GET / POST | `/api/huespedes` | Listar / registrar huéspedes |
| PUT | `/api/huespedes/{id}` | Modificar un huésped |
| GET / POST | `/api/reservas` | Listar (filtros `estado`, `desde`, `hasta`) / crear reservas |
| GET / PATCH | `/api/reservas/{id}` | Consultar / modificar una reserva |
| PATCH | `/api/reservas/{id}/cancelar` | Cancelar una reserva |
| POST / GET | `/api/reservas/{id}/comprobante` | Generar / consultar el comprobante |
| GET / POST / PUT | `/api/tipos-habitacion`, `/api/tarifas`, `/api/usuarios` | Administración (y `PATCH .../estado` en tarifas y habitaciones) |
| GET | `/api/reportes/ocupacion?desde&hasta&tipoId` | Reporte de ocupación |
| GET | `/api/auditoria` | Registro de auditoría (administrador general) |
| GET | `/api/sistema/estado` | Estado y configuración del sistema (administrador general) |
| POST | `/api/asistente/simular` | Simulador del consultor (solo con `ASISTENTE_SIMULADOR=true`) |
| GET / POST | `/api/whatsapp/webhook` | Webhook de Meta (verificación y mensajes, firma HMAC-SHA256) |
| POST | `/api/whatsapp/twilio`, `/api/whatsapp/twilio/estado` | Webhook de Twilio (mensajes y estados, firma HMAC-SHA1) |
| GET | `/api/publico/habitaciones` | Página pública (sin sesión) |

## Control de concurrencia

`ReservaService.crear` se ejecuta en una sola transacción: bloquea la fila de la habitación (`PESSIMISTIC_WRITE`),
verifica que no haya cruces de fechas con reservas activas y guarda la reserva y su comprobante. Si dos solicitudes
compiten por la misma habitación y fechas, solo una se confirma y la otra recibe `409`. Cualquier error revierte la
transacción completa.

```bash
./pruebas/concurrencia.sh 10 4 2027-03-10 2027-03-12   # esperado: un 201 y nueve 409
```

Pasos completos en [`docs/verificacion.md`](docs/verificacion.md).

## Consultor virtual de reservas (IA)

Atiende consultas de huéspedes (disponibilidad, tarifas, estado de su reserva, política de cancelación) usando la API
de Gemini. El modelo **no accede a la base de datos**: solicita herramientas de solo lectura definidas por la
aplicación. Una red neuronal (perceptrón multicapa 6-4-5, entrenado al iniciar) clasifica la intención del mensaje como
apoyo. Antes de procesar datos pide autorización (`ACEPTO` / `REVOCAR`, Ley 1581 de 2012), exige el código del
comprobante para mostrar una reserva, solo envía al modelo el primer nombre y limita a 20 mensajes por hora por número.
Detalles en [`docs/asistente-ia.md`](docs/asistente-ia.md).

## Notificaciones

Al crear, modificar o cancelar una reserva se avisa al huésped por **correo, WhatsApp o ambos**
(`NOTIFICACIONES_CANALES`), solo después de confirmar la transacción. Además: recordatorio de llegada (9:00), resumen
diario a los administradores (7:00) y alerta de tarifas por vencer. Cada intento queda en Auditoría.

WhatsApp funciona con **Meta Cloud API** o **Twilio** (`WHATSAPP_PROVEEDOR`). Limitaciones de las cuentas de prueba:
Meta exige verificar el negocio (error `131031`) y el número de prueba de Twilio solo envía plantillas preaprobadas
(`WHATSAPP_PLANTILLA_RESERVA=HX...`). Configuración paso a paso en [`docs/notificaciones.md`](docs/notificaciones.md).

## Seguridad

- Sesión con **JWT** de 2 horas con aviso y renovación; contraseñas con **BCrypt**; bloqueo tras 5 intentos fallidos
  en 15 minutos; recuperación de contraseña con enlace de un solo uso (30 min, se guarda solo su hash).
- Conexión a la base con usuario de **mínimo privilegio** (`hotel_app`); consultas parametrizadas (JPA).
- **Auditoría** de inicios de sesión y cambios (antes/después, IP), sin contraseñas ni tokens y con datos de contacto
  enmascarados.
- Webhooks de WhatsApp validados con firma; CORS restringido al frontend; **ningún secreto en el repositorio**.

Guía completa: [`docs/seguridad-credenciales.md`](docs/seguridad-credenciales.md).

## Pruebas y evidencias

- [`docs/plan-pruebas.md`](docs/plan-pruebas.md) (38 casos), [`docs/informe-pruebas.md`](docs/informe-pruebas.md) y
  [`docs/evidencia-persistencia.md`](docs/evidencia-persistencia.md) (interfaz → base de datos).
- `./pruebas/evidencia.sh <n.º de reserva>`: consulta la BD y guarda el resultado en `evidencias/`.
- `./pruebas/evidencia_e2e.sh <idHuesped> <idHabitacion>`: crear → modificar → cancelar, consultando la BD en cada paso.
- `./pruebas/concurrencia.sh` y `python3 pruebas/rendimiento.py` (con `database/datos_rendimiento.sql`; apagar las
  notificaciones durante la prueba).

- Guía de la entrega final (prototipo, pruebas e IA): [`docs/entrega-final.md`](docs/entrega-final.md).
