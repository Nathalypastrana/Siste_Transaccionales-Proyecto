/*
  DATOS DE REFERENCIA PARA LA PRUEBA DE RENDIMIENTO (RNF 05)
  Crea 100 habitaciones (P-001 ... P-100) y 10 000 reservas FINALIZADAS en el pasado (año 2023),
  sin cruces de fechas, para medir el sistema con el volumen que define el documento.
  - Requiere que ya existan: un tipo de habitación con tarifa, un huésped y un usuario (datos_prueba.sql).
  - Es idempotente: si las habitaciones P-xxx ya existen, no hace nada.
  - Para quitar estos datos: ejecutar al final del archivo el bloque "LIMPIEZA" (comentado).
*/
USE ReservasHotelDB;
GO

IF EXISTS (SELECT 1 FROM Habitacion WHERE numero LIKE 'P-%')
BEGIN
    PRINT 'Los datos de rendimiento ya existen; no se vuelve a insertar nada.';
    RETURN;
END

DECLARE @huesped INT = (SELECT MIN(idHuesped) FROM Huesped);
DECLARE @usuario INT = (SELECT MIN(idUsuario) FROM Usuario);
DECLARE @tipo INT = (SELECT TOP 1 t.idTipoHabitacion FROM TipoHabitacion t
                     WHERE EXISTS (SELECT 1 FROM Tarifa x WHERE x.idTipoHabitacion = t.idTipoHabitacion)
                     ORDER BY t.idTipoHabitacion);

IF @huesped IS NULL OR @usuario IS NULL OR @tipo IS NULL
BEGIN
    RAISERROR('Faltan datos base: se necesita al menos un huésped, un usuario y un tipo de habitación con tarifa.', 16, 1);
    RETURN;
END

BEGIN TRANSACTION;

WITH n AS (SELECT TOP 100 ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS i FROM sys.all_objects)
INSERT INTO Habitacion (numero, piso, capacidad, estado, idTipoHabitacion)
SELECT 'P-' + RIGHT('000' + CAST(i AS VARCHAR(3)), 3), 1 + (i / 10), 2, 'DISPONIBLE', @tipo
FROM n;

WITH k AS (SELECT TOP 100 ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) - 1 AS k FROM sys.all_objects)
INSERT INTO Reserva (idHuesped, idHabitacion, idUsuario, idTarifa, fechaEntrada, fechaSalida, estado, valorTotal)
SELECT @huesped, h.idHabitacion, @usuario, tf.idTarifa,
       DATEADD(DAY, k.k * 3, '2023-01-01'), DATEADD(DAY, k.k * 3 + 2, '2023-01-01'),
       'FINALIZADA', tf.valor * 2
FROM Habitacion h
CROSS JOIN k
CROSS APPLY (SELECT TOP 1 idTarifa, valor FROM Tarifa
             WHERE idTipoHabitacion = h.idTipoHabitacion ORDER BY idTarifa) tf
WHERE h.numero LIKE 'P-%';

COMMIT;

SELECT (SELECT COUNT(*) FROM Habitacion) AS habitaciones, (SELECT COUNT(*) FROM Reserva) AS reservas;
GO

/* LIMPIEZA (descomentar para borrar los datos de rendimiento)
DELETE c FROM Comprobante c JOIN Reserva r ON r.idReserva = c.idReserva JOIN Habitacion h ON h.idHabitacion = r.idHabitacion WHERE h.numero LIKE 'P-%';
DELETE r FROM Reserva r JOIN Habitacion h ON h.idHabitacion = r.idHabitacion WHERE h.numero LIKE 'P-%';
DELETE FROM Habitacion WHERE numero LIKE 'P-%';
*/
