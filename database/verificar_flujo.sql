/*
  Consultas de verificación (solo lectura). Sirven como evidencia de que lo
  hecho desde la interfaz o la API quedó guardado en la base de datos.
*/
USE ReservasHotelDB;
GO

-- 1) Últimos huéspedes registrados
SELECT TOP 5 idHuesped, tipoDocumento, numeroDocumento, nombres, apellidos, correo
FROM Huesped
ORDER BY idHuesped DESC;

-- 2) Últimas reservas con su huésped, habitación y comprobante
SELECT TOP 10
    r.idReserva, h.nombres + ' ' + h.apellidos AS huesped, hb.numero AS habitacion,
    r.fechaEntrada, r.fechaSalida, r.estado, r.valorTotal, c.numeroComprobante
FROM Reserva r
JOIN Huesped h      ON h.idHuesped = r.idHuesped
JOIN Habitacion hb  ON hb.idHabitacion = r.idHabitacion
LEFT JOIN Comprobante c ON c.idReserva = r.idReserva
ORDER BY r.idReserva DESC;

-- 3) Reservas ACTIVAS que se superponen en la misma habitación.
--    El resultado DEBE ser 0 filas: si aparece alguna, falló el control de concurrencia.
SELECT a.idHabitacion,
       a.idReserva AS reservaA, a.fechaEntrada AS entradaA, a.fechaSalida AS salidaA,
       b.idReserva AS reservaB, b.fechaEntrada AS entradaB, b.fechaSalida AS salidaB
FROM Reserva a
JOIN Reserva b
  ON a.idHabitacion = b.idHabitacion
 AND a.idReserva < b.idReserva
 AND a.fechaEntrada < b.fechaSalida
 AND a.fechaSalida  > b.fechaEntrada
WHERE a.estado IN ('PENDIENTE', 'CONFIRMADA')
  AND b.estado IN ('PENDIENTE', 'CONFIRMADA');

-- 4) Reservas sin comprobante (también deben ser 0 filas para las reservas creadas por la API)
SELECT r.idReserva
FROM Reserva r
LEFT JOIN Comprobante c ON c.idReserva = r.idReserva
WHERE c.idReserva IS NULL AND r.estado IN ('PENDIENTE', 'CONFIRMADA');
