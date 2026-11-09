# Verificación del flujo integral y de la concurrencia

Flujo: **interfaz (React) → API (Spring Boot) → base de datos (SQL Server)**.

## 0. Preparación

La base se crea con `database/schema.sql`, que **no borra nada** y se puede ejecutar varias veces.
`database/reiniciar_base.sql` elimina todas las tablas y datos: solo usarlo en desarrollo.

```
docker cp database/schema.sql sqlserver:/tmp/schema.sql
docker exec sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P '<clave>' -C -i /tmp/schema.sql
```

Si ya tenías la base de fases anteriores, vuelve a ejecutar `schema.sql`: agrega la tabla `AutorizacionDatos` sin tocar datos.

Variables nuevas del backend: `JWT_SECRET` (mín. 32 caracteres), `ADMIN_CORREO` y `ADMIN_PASSWORD` (crean el primer administrador si no hay ninguno). Todo endpoint, salvo el login, exige sesión: **inicia sesión primero** en la interfaz.

Luego arrancar el backend (`backend/`, `mvn spring-boot:run`) y la interfaz (`frontend/`, `npm run dev`).

## 1. Flujo completo desde la interfaz

| Paso | En la interfaz (http://localhost:5173) | Qué se debe ver |
|------|----------------------------------------|-----------------|
| 1 | Huéspedes → registrar un huésped nuevo | Mensaje de éxito y el huésped en el listado |
| 2 | Nueva reserva → elegir ese huésped, usuario y fechas futuras | Solo aparecen habitaciones libres, con total calculado |
| 3 | Confirmar reserva | Mensaje "Reserva #N confirmada" |
| 4 | Reservas | La reserva aparece con estado CONFIRMADA |
| 5 | Repetir la reserva con la misma habitación y fechas | La habitación ya no se ofrece en la lista |
| 6 | Cancelar la reserva | Estado CANCELADA; la habitación vuelve a estar libre |

## 2. Comprobar que quedó en la base de datos

```
docker cp database/verificar_flujo.sql sqlserver:/tmp/verificar_flujo.sql
docker exec sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P '<clave>' -C -i /tmp/verificar_flujo.sql
```

Las consultas 1 y 2 muestran el huésped y la reserva creados desde la interfaz, con su comprobante.
La consulta 3 (reservas activas superpuestas) y la 4 (reservas sin comprobante) deben devolver **0 filas**.

## 3. Concurrencia: solicitudes simultáneas sobre la misma habitación

`ReservaService.crear` se ejecuta en una sola transacción: bloquea la fila de la habitación
(`PESSIMISTIC_WRITE`), verifica que no haya cruces de fechas con reservas PENDIENTE o CONFIRMADA y
guarda reserva y comprobante. Las solicitudes concurrentes quedan en fila y, al llegar su turno,
la verificación las rechaza con `409`.

Prueba (desde la raíz del repositorio, con el backend corriendo):

```
./pruebas/concurrencia.sh 10 4 2027-03-10 2027-03-12
```

Argumentos: número de solicitudes, id de habitación, fecha de entrada y de salida (usar fechas libres).
Resultado esperado: **1 respuesta 201 y 9 respuestas 409**, y el mensaje `RESULTADO: OK`.
Después, ejecutar `verificar_flujo.sql`: la consulta 3 debe seguir sin filas.

### Evidencia a adjuntar

- Captura de la salida de `concurrencia.sh` (un 201, el resto 409).
- Captura de `verificar_flujo.sql` con la reserva creada y la consulta 3 vacía.
- Capturas de la interfaz: huésped registrado, reserva creada y listada.

## 4. Rendimiento (RNF 05)

```
docker cp database/datos_rendimiento.sql sqlserver:/tmp/d.sql
docker exec sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P '<clave>' -C -d ReservasHotelDB -i /tmp/d.sql
API_CORREO=<admin> API_PASSWORD=<clave> python3 pruebas/rendimiento.py
```
Carga 100 habitaciones y 10.000 reservas históricas (idempotente; limpieza comentada al final del script). Las tarifas deben cubrir las fechas de prueba.
