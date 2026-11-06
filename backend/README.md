# Backend - Sistema transaccional de reservas de hotel

Spring Boot 3 + Java 17 + Spring Data JPA (Hibernate) + SQL Server + Spring Security.

## Estructura

```
src/main/java/com/hotel/reservas/
├── model/        # Entidades JPA (TipoHabitacion, Habitacion, Usuario, Huesped, Tarifa, Reserva, ComprobanteReserva -> tabla Comprobante)
├── repository/   # Acceso a datos (Spring Data JPA)
├── service/      # Lógica de negocio y transacciones (Reserva, Habitacion, Huesped)
├── asistente/    # Consultor virtual: AsistenteService, herramientas y clientes del modelo (Claude, Gemini)
├── whatsapp/     # Webhook firmado y cliente de envío de WhatsApp Cloud API
├── controller/   # API REST
├── dto/          # Objetos de entrada/salida
├── exception/    # Excepciones y manejo global de errores
└── config/       # DatabaseConnection (módulo de conexión), SecurityConfig (seguridad y CORS)
```

La base de datos (`ReservasHotelDB`) se crea con `../database/schema.sql`; las entidades mapean
exactamente sus tablas y columnas (Hibernate no modifica el esquema, `ddl-auto=none`).

## Cómo ejecutarlo

1. Ejecutar `database/schema.sql` (crea la base `ReservasHotelDB` y las tablas) y luego `database/datos_prueba.sql`.
2. Definir `DB_USER` y `DB_PASSWORD` (o `DB_URL`) como variables de entorno. `DatabaseConnection` verifica la conexión al arrancar y deja el resultado en el log.
3. Desde `backend/`: `mvn spring-boot:run`

## Endpoints

| Método | Ruta                                             | Descripción                               |
|--------|--------------------------------------------------|-------------------------------------------|
| GET    | `/api/salud`                                     | Estado de la conexión con la base de datos |
| GET    | `/api/habitaciones`                              | Listar habitaciones con tarifa vigente    |
| GET    | `/api/habitaciones/disponibles?entrada=&salida=` | Habitaciones libres en un rango de fechas |
| GET    | `/api/huespedes`                                 | Listar huéspedes                          |
| POST   | `/api/huespedes`                                 | Registrar huésped (validado)              |
| GET    | `/api/usuarios`                                  | Usuarios activos (sin contraseña)         |
| GET    | `/api/reservas`                                  | Listar reservas                           |
| POST   | `/api/reservas`                                  | Crear reserva                             |
| GET    | `/api/reservas/{id}`                             | Consultar reserva                         |
| PATCH  | `/api/reservas/{id}/cancelar`                    | Cancelar reserva                          |
| GET/POST | `/api/whatsapp/webhook`                        | Webhook de WhatsApp (firma obligatoria)   |
| POST   | `/api/asistente/simular`                         | Simulador del consultor (`ASISTENTE_SIMULADOR=true`) |

La API admite peticiones desde la interfaz en `http://localhost:5173` (CORS).

```json
POST /api/reservas
{ "huespedId": 1, "habitacionId": 1, "usuarioId": 1, "fechaEntrada": "2026-11-10", "fechaSalida": "2026-11-12" }
```

## Transacciones y concurrencia (ACID)

`ReservaService.crear` corre en una sola transacción: bloquea la fila de la habitación
(`PESSIMISTIC_WRITE`), verifica que no haya cruces de fechas con reservas PENDIENTE o CONFIRMADA,
y guarda reserva y comprobante. Si dos solicitudes llegan a la vez para la misma habitación y fechas,
la segunda espera y luego es rechazada con `409 Conflict`. Cualquier error revierte todo (rollback).

## Consultor virtual (IA)

Ver `../docs/asistente-ia.md`. Variables: `ASISTENTE_PROVEEDOR` (`claude`|`gemini`), `GEMINI_API_KEY`, `GEMINI_MODELO`, `ANTHROPIC_API_KEY`, `ASISTENTE_SIMULADOR`, `WHATSAPP_VERIFY_TOKEN`,
`WHATSAPP_APP_SECRET`, `WHATSAPP_ACCESS_TOKEN`, `WHATSAPP_PHONE_NUMBER_ID`, `RECEPCION_WHATSAPP`.

## Pendiente

- Autenticación JWT y roles (RECEPCIONISTA / ADMINISTRADOR)
- CRUD de habitaciones y tarifas
- Modificación de reservas
