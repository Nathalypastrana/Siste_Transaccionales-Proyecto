# Sistema transaccional para la gestión de reservas de un hotel

Aplicación web para registrar huéspedes y gestionar reservas de habitaciones. Su objetivo central es
**garantizar que no existan reservas superpuestas**, incluso cuando varias solicitudes llegan al mismo tiempo.

## Tecnologías

| Capa | Tecnología |
|------|------------|
| Interfaz | React 18 + TypeScript + Vite |
| API / lógica de negocio | Java 17 + Spring Boot 3 (Spring Web, Validation, Security) |
| Consultor virtual (IA) | API de Claude (Anthropic) con *tool use* + WhatsApp Business Platform (Cloud API) |
| Acceso a datos | Spring Data JPA + Hibernate |
| Base de datos | SQL Server |

## Estructura del repositorio

```
├── backend/     # API REST (controller → service → repository → model)
├── frontend/    # Interfaz React (pages, components, api.ts)
├── database/    # Scripts SQL (creación, datos de prueba, reinicio, verificación)
├── pruebas/     # Prueba de concurrencia
└── docs/        # Guía de verificación del flujo completo
```

Separación de responsabilidades: la **interfaz** (`frontend/src/pages`) solo presenta y valida; la
**lógica de negocio** vive en `backend/.../service`; el **acceso a datos** en `backend/.../repository`; y la
**conexión a la base** en un módulo propio, `backend/.../config/DatabaseConnection.java`, configurado desde
`backend/src/main/resources/application.properties`.

## Requisitos

- **JDK 17** o superior
- **Maven 3.9** o superior
- **Node.js 18** o superior (recomendado la versión LTS)
- **SQL Server** 2019 o superior (puede ser un contenedor Docker)
- Git

## Instalación y configuración

### 1. Base de datos

Con SQL Server en ejecución, ejecutar en este orden:

1. `database/schema.sql`: crea la base `ReservasHotelDB` y las tablas. **No borra datos**; se puede repetir.
2. `database/datos_prueba.sql`: carga huéspedes, habitaciones, tarifas y usuarios de prueba.

> `database/reiniciar_base.sql` elimina **todas** las tablas y sus datos. Úsalo solo en desarrollo.

Ejemplo con un contenedor Docker llamado `sqlserver`:

```
docker cp database/schema.sql sqlserver:/tmp/schema.sql
docker exec sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P '<clave>' -C -i /tmp/schema.sql
docker cp database/datos_prueba.sql sqlserver:/tmp/datos_prueba.sql
docker exec sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P '<clave>' -C -i /tmp/datos_prueba.sql
```

(En algunas imágenes la ruta es `/opt/mssql-tools/bin/sqlcmd`.)

### 2. Conexión del backend

La conexión se define en `backend/src/main/resources/application.properties` y se sobrescribe con variables
de entorno, para no guardar claves en el repositorio:

| Variable | Valor por defecto |
|----------|-------------------|
| `DB_URL` | `jdbc:sqlserver://localhost:1433;databaseName=ReservasHotelDB;encrypt=true;trustServerCertificate=true` |
| `DB_USER` | `sa` |
| `DB_PASSWORD` | (definir siempre) |

```
export DB_USER='sa'
export DB_PASSWORD='<clave>'
```

(En PowerShell: `$env:DB_PASSWORD='<clave>'`.) Si la clave tiene caracteres como `!` o `$`, ponla entre comillas simples.

### 3. Frontend

```
cd frontend
npm install
```

Si el backend no está en `http://localhost:8080`, crear `frontend/.env.local` con `VITE_API_URL=http://host:puerto`.

## Ejecución

En dos terminales:

```
# Terminal 1 - API (puerto 8080)
cd backend
mvn spring-boot:run

# Terminal 2 - Interfaz (puerto 5173)
cd frontend
npm run dev
```

Abrir http://localhost:5173. El menú lateral muestra el estado de la conexión con la base de datos
(verde: conectada; rojo: sin conexión).

## Funcionalidades

| Pantalla | Qué permite |
|----------|-------------|
| Inicio | Indicadores y últimas reservas |
| Nueva reserva | Elegir huésped y fechas, ver solo las habitaciones libres, calcular el total y confirmar |
| Reservas | Consultar, buscar y cancelar reservas (con confirmación) |
| Habitaciones | Consultar habitaciones, tarifa vigente y estado |
| Huéspedes | Registrar huéspedes (validado) y consultar el listado |
| Consultor virtual | Simulador del chat de WhatsApp para probar el asistente de IA |

Navegación: menú lateral, ruta de navegación (Inicio / Reservas / …), botones de retorno y página 404 con
enlace al inicio. Las operaciones muestran avisos de éxito o de error; si no hay conexión aparece un mensaje
con botón **Reintentar**.

## Validación y manejo de errores

- **Interfaz:** campos obligatorios, formato de documento, teléfono y correo, fechas coherentes.
- **API:** Bean Validation en los datos recibidos; respuestas `400` (datos inválidos), `404` (no existe),
  `409` (conflicto, por ejemplo habitación ya reservada o documento duplicado) y `503` (base de datos sin conexión).

## API

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/salud` | Estado de la conexión con la base de datos |
| GET | `/api/habitaciones` | Listar habitaciones con tarifa vigente |
| GET | `/api/habitaciones/disponibles?entrada=&salida=` | Habitaciones libres en un rango |
| GET / POST | `/api/huespedes` | Listar / registrar huéspedes |
| GET | `/api/usuarios` | Usuarios activos |
| GET / POST | `/api/reservas` | Listar / crear reservas |
| GET | `/api/reservas/{id}` | Consultar una reserva |
| PATCH | `/api/reservas/{id}/cancelar` | Cancelar una reserva |
| GET / POST | `/api/whatsapp/webhook` | Webhook de WhatsApp (verificación y mensajes entrantes, con firma) |
| POST | `/api/asistente/simular` | Simulador del consultor (desactivado por defecto) |

## Control de concurrencia

`ReservaService.crear` se ejecuta en una sola transacción: bloquea la fila de la habitación
(`PESSIMISTIC_WRITE`), verifica que no haya cruces de fechas con reservas activas y guarda la reserva y su
comprobante. Si dos solicitudes compiten por la misma habitación y fechas, solo una se confirma y la otra
recibe `409`. Cualquier error revierte la transacción completa.

Prueba automática (con el backend corriendo):

```
./pruebas/concurrencia.sh 10 4 2027-03-10 2027-03-12
```

Resultado esperado: una respuesta `201` y nueve `409`. Los pasos completos y las consultas de verificación
están en [`docs/verificacion.md`](docs/verificacion.md).

## Consultor virtual de reservas (IA)

Asistente que atiende consultas de huéspedes por WhatsApp (disponibilidad, tarifas, estado de su reserva, política
de cancelación) usando la API de Claude. El modelo no accede a la base de datos: solicita **herramientas de solo
lectura** definidas por la aplicación, que consultan los mismos servicios de la API REST. Identifica al huésped por
su número de teléfono, responde solo con datos del sistema y deriva al recepcionista lo que no pueda resolver.

Para probarlo sin cuenta de Meta:

```
export ANTHROPIC_API_KEY='sk-ant-...'
export ASISTENTE_SIMULADOR=true     # solo desarrollo
cd backend && mvn spring-boot:run
```

Luego abrir **Consultor virtual** en la interfaz. La arquitectura, las variables de entorno, las decisiones de
seguridad y la conexión con WhatsApp real están en [`docs/asistente-ia.md`](docs/asistente-ia.md).

## Pendiente

- Autenticación con JWT y control de acceso por rol (hoy el usuario que registra la reserva se elige en el formulario)
- Modificación de reservas y administración de habitaciones y tarifas
