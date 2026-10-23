# Backend - Sistema transaccional de reservas de hotel

Spring Boot 3 + Java 17 + Spring Data JPA (Hibernate) + SQL Server + Spring Security.

## Estructura

```
backend/
├── db/schema.sql                      # Tablas, PK, FK, restricciones y datos de prueba
├── pom.xml
└── src/main/java/com/hotel/reservas/
    ├── model/        # Entidades JPA (TipoHabitacion, Habitacion, Usuario, Huesped, Tarifa, Reserva, ComprobanteReserva)
    ├── repository/   # Acceso a datos (Spring Data JPA)
    ├── service/      # Lógica de negocio y transacciones (ReservaService)
    ├── controller/   # API REST (ReservaController)
    ├── dto/          # Objetos de entrada/salida
    ├── exception/    # Excepciones y manejo global de errores
    └── config/       # Seguridad (SecurityConfig)
```

## Cómo ejecutarlo

1. Crear la base `hotel_reservas` en SQL Server y ejecutar `db/schema.sql`.
2. Definir variables de entorno `DB_URL`, `DB_USER` y `DB_PASSWORD` (o editar `application.properties`).
3. `mvn spring-boot:run`

## Endpoints iniciales

| Método | Ruta                          | Descripción                 |
|--------|-------------------------------|-----------------------------|
| POST   | `/api/reservas`               | Crear reserva               |
| GET    | `/api/reservas/{id}`          | Consultar reserva           |
| PATCH  | `/api/reservas/{id}/cancelar` | Cancelar reserva            |

Ejemplo:

```json
POST /api/reservas
{ "huespedId": 1, "habitacionId": 1, "fechaEntrada": "2026-11-10", "fechaSalida": "2026-11-12" }
```

## Transacciones y concurrencia (ACID)

`ReservaService.crear` corre en una sola transacción: bloquea la fila de la habitación
(`PESSIMISTIC_WRITE`), verifica que no haya cruces de fechas y guarda reserva y comprobante.
Si dos solicitudes llegan a la vez para la misma habitación y fechas, la segunda espera y luego
es rechazada con `409 Conflict`. Cualquier error revierte todo (rollback).

## Pendiente

- Autenticación JWT y roles
- CRUD de huéspedes, habitaciones y tarifas
- Modificación de reservas
- Pruebas de concurrencia (varias solicitudes simultáneas sobre la misma habitación)
