/* Consultas de evidencia: demuestran que lo registrado desde la interfaz quedó guardado en SQL Server.
   Parámetro opcional: -v RESERVA_ID=<número que muestra la interfaz>  (si no se da, se usa la última reserva).
   Solo lectura (SELECT): no modifica nada. */
SET NOCOUNT ON;
USE ReservasHotelDB;

PRINT '=== 0. Fecha y hora del servidor de base de datos ===';
SELECT @@SERVERNAME AS servidor, DB_NAME() AS base_de_datos, SYSDATETIME() AS ahora;

DECLARE @id INT = TRY_CONVERT(INT, '$(RESERVA_ID)');
IF @id IS NULL SELECT @id = MAX(idReserva) FROM Reserva;

PRINT '=== 1. Últimas 5 reservas (huésped, habitación y usuario que la registró) ===';
SELECT TOP 5 r.idReserva, h.nombres + ' ' + h.apellidos AS huesped, hab.numero AS habitacion,
       r.fechaEntrada, r.fechaSalida, r.estado, r.valorTotal, r.fechaReserva, u.correo AS registrada_por
FROM Reserva r
JOIN Huesped h ON h.idHuesped = r.idHuesped
JOIN Habitacion hab ON hab.idHabitacion = r.idHabitacion
JOIN Usuario u ON u.idUsuario = r.idUsuario
ORDER BY r.idReserva DESC;

PRINT '=== 2. Reserva consultada (la que se ve en la interfaz) ===';
SELECT r.idReserva, h.numeroDocumento, h.nombres, h.apellidos, hab.numero AS habitacion, hab.estado AS estado_habitacion,
       r.fechaEntrada, r.fechaSalida, r.estado, r.valorTotal, t.valor AS tarifa_noche
FROM Reserva r
JOIN Huesped h ON h.idHuesped = r.idHuesped
JOIN Habitacion hab ON hab.idHabitacion = r.idHabitacion
JOIN Tarifa t ON t.idTarifa = r.idTarifa
WHERE r.idReserva = @id;

PRINT '=== 3. Comprobante de esa reserva ===';
SELECT idComprobante, idReserva, numeroComprobante, fechaGeneracion FROM Comprobante WHERE idReserva = @id;

PRINT '=== 4. Auditoría de esa reserva (cada operación deja huella) ===';
SELECT idAuditoria, fecha, correo, rol, accion, entidad, entidadId, detalle
FROM Auditoria WHERE entidad = 'Reserva' AND entidadId = CONVERT(VARCHAR(40), @id) ORDER BY idAuditoria;

PRINT '=== 5. Últimas 15 acciones de auditoría (incluye notificaciones enviadas) ===';
SELECT TOP 15 idAuditoria, fecha, correo, accion, entidad, entidadId, LEFT(detalle, 80) AS detalle FROM Auditoria ORDER BY idAuditoria DESC;

PRINT '=== 6. Habitaciones y su estado ===';
SELECT hab.numero, hab.piso, hab.capacidad, hab.estado, t.nombre AS tipo FROM Habitacion hab JOIN TipoHabitacion t ON t.idTipoHabitacion = hab.idTipoHabitacion ORDER BY hab.numero;

PRINT '=== 7. Últimas tarifas ===';
SELECT TOP 5 t.idTarifa, tp.nombre AS tipo, t.valor, t.fechaInicio, t.fechaFin, t.activa FROM Tarifa t JOIN TipoHabitacion tp ON tp.idTipoHabitacion = t.idTipoHabitacion ORDER BY t.idTarifa DESC;

PRINT '=== 8. Usuarios: la contraseña se guarda cifrada (solo se muestran los 4 primeros caracteres del hash) ===';
SELECT idUsuario, correo, rol, activo,
       CASE WHEN password LIKE '$2%' THEN 'BCrypt' ELSE 'TEXTO PLANO' END AS almacenamiento,
       LEFT(password, 4) AS prefijo, LEN(password) AS longitud
FROM Usuario;

PRINT '=== 9. Integridad: reservas activas que se cruzan en la misma habitación (debe ser 0) ===';
SELECT COUNT(*) AS cruces
FROM Reserva a JOIN Reserva b ON a.idHabitacion = b.idHabitacion AND a.idReserva < b.idReserva
 AND a.fechaEntrada < b.fechaSalida AND b.fechaEntrada < a.fechaSalida
WHERE a.estado NOT IN ('CANCELADA','FINALIZADA') AND b.estado NOT IN ('CANCELADA','FINALIZADA');

PRINT '=== 10. Totales por tabla ===';
SELECT 'Huesped' AS tabla, COUNT(*) AS filas FROM Huesped UNION ALL SELECT 'Habitacion', COUNT(*) FROM Habitacion
UNION ALL SELECT 'Reserva', COUNT(*) FROM Reserva UNION ALL SELECT 'Comprobante', COUNT(*) FROM Comprobante
UNION ALL SELECT 'Usuario', COUNT(*) FROM Usuario UNION ALL SELECT 'Auditoria', COUNT(*) FROM Auditoria;
GO
