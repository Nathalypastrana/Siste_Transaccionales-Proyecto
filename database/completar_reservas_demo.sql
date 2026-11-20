/* COMPLETA la carga de datos_demostracion.sql: solo reservas, comprobantes y estado de habitaciones.
   Úsalo una sola vez si la primera ejecución falló en la sección 5 (error "Incorrect syntax near '20'").
   Idempotente: si las reservas de demostración ya existen, no hace nada.
   Uso: sqlcmd -S localhost -U sa -C -d ReservasHotelDB -i completar_reservas_demo.sql */
USE ReservasHotelDB;
GO
IF EXISTS (SELECT 1 FROM Reserva r JOIN Huesped h ON h.idHuesped = r.idHuesped WHERE h.numeroDocumento = '1056669073')
   OR NOT EXISTS (SELECT 1 FROM Huesped WHERE numeroDocumento = '1056669073')
BEGIN
    PRINT 'Reservas de demostración ya cargadas (o faltan los huéspedes): no se hace nada.';
    SET NOEXEC ON;
END
GO
SET NOCOUNT ON;

/* 5. Reservas: el usuario que las registra es la primera cuenta activa (o la primera existente) */
DECLARE @u INT = (SELECT TOP 1 idUsuario FROM Usuario ORDER BY activo DESC, idUsuario);
INSERT INTO Reserva (idHuesped, idHabitacion, idUsuario, idTarifa, fechaEntrada, fechaSalida, fechaReserva, estado, valorTotal)
SELECT hu.idHuesped, hb.idHabitacion, @u, tf.idTarifa, CAST(v.ent AS DATE), CAST(v.sal AS DATE),
       DATEADD(DAY, -(3 + (hu.idHuesped % 20)), CAST(v.ent AS DATETIME)) + CAST('10:30' AS DATETIME), v.est, v.total
FROM (VALUES
('1056669073','C02','Cabaña campesina','2026-01-01','2026-11-18','2026-11-21','CONFIRMADA',960000.00),
('1067962320','103','Sencilla','2026-01-01','2026-11-18','2026-11-21','CONFIRMADA',360000.00),
('1064315788','C03','Cabaña campesina','2026-01-01','2026-11-18','2026-11-20','CONFIRMADA',640000.00),
('1094578218','202','Familiar','2026-01-01','2026-11-18','2026-11-21','CONFIRMADA',840000.00),
('1064708126','204','Doble','2026-01-01','2026-11-18','2026-11-21','CONFIRMADA',540000.00),
('1033689519','G01','Glamping','2026-01-01','2026-09-26','2026-09-30','FINALIZADA',1040000.00),
('1093727401','103','Sencilla','2026-01-01','2026-10-12','2026-10-16','FINALIZADA',480000.00),
('1001245123','101','Sencilla','2026-01-01','2026-10-28','2026-10-31','FINALIZADA',360000.00),
('1048309773','104','Sencilla','2026-01-01','2026-08-19','2026-08-20','FINALIZADA',120000.00),
('1066727300','302','Familiar','2026-01-01','2026-11-03','2026-11-07','FINALIZADA',1120000.00),
('1058847902','C03','Cabaña campesina','2026-01-01','2026-10-10','2026-10-11','FINALIZADA',320000.00),
('1008629549','201','Doble','2026-01-01','2026-10-03','2026-10-04','FINALIZADA',180000.00),
('1019643929','201','Doble','2026-01-01','2026-08-31','2026-09-04','FINALIZADA',720000.00),
('1073362139','201','Doble','2026-01-01','2026-10-08','2026-10-11','FINALIZADA',540000.00),
('1073343091','G02','Glamping','2026-01-01','2026-10-30','2026-11-03','FINALIZADA',1040000.00),
('1077079038','C01','Cabaña campesina','2026-01-01','2026-07-08','2026-07-11','FINALIZADA',960000.00),
('1017611550','101','Sencilla','2026-01-01','2026-09-13','2026-09-16','FINALIZADA',360000.00),
('1018165850','202','Familiar','2026-01-01','2026-06-03','2026-06-06','FINALIZADA',840000.00),
('1069797368','G01','Glamping','2026-01-01','2026-11-10','2026-11-11','FINALIZADA',260000.00),
('1006895485','204','Doble','2026-01-01','2026-10-16','2026-10-17','FINALIZADA',180000.00),
('1063890916','104','Sencilla','2026-01-01','2026-08-09','2026-08-13','FINALIZADA',480000.00),
('1062623684','103','Sencilla','2026-01-01','2026-08-23','2026-08-26','FINALIZADA',360000.00),
('1045153375','C01','Cabaña campesina','2026-01-01','2026-08-17','2026-08-19','FINALIZADA',640000.00),
('1004583896','201','Doble','2026-01-01','2026-08-26','2026-08-29','FINALIZADA',540000.00),
('1064940010','201','Doble','2026-01-01','2026-08-01','2026-08-03','FINALIZADA',360000.00),
('1021527272','C01','Cabaña campesina','2026-01-01','2026-10-15','2026-10-17','FINALIZADA',640000.00),
('1010557407','302','Familiar','2026-01-01','2026-07-06','2026-07-09','FINALIZADA',840000.00),
('1062265651','G02','Glamping','2026-01-01','2026-07-08','2026-07-11','CANCELADA',780000.00),
('1069703978','103','Sencilla','2026-01-01','2026-07-15','2026-07-16','CANCELADA',120000.00),
('1031049233','104','Sencilla','2026-01-01','2026-08-21','2026-08-23','CANCELADA',240000.00),
('1089979425','201','Doble','2026-01-01','2026-09-18','2026-09-22','CANCELADA',720000.00),
('1092228670','C01','Cabaña campesina','2026-01-01','2026-08-22','2026-08-26','CANCELADA',1280000.00),
('1098109198','101','Sencilla','2026-01-01','2026-12-23','2026-12-25','CONFIRMADA',240000.00),
('1013617158','102','Doble','2026-01-01','2026-12-08','2026-12-09','CONFIRMADA',180000.00),
('1047815518','101','Sencilla','2026-01-01','2026-12-28','2026-12-29','CONFIRMADA',120000.00),
('1008578530','203','Doble','2026-01-01','2026-11-27','2026-11-29','CONFIRMADA',360000.00),
('1001861671','G02','Glamping','2026-01-01','2026-12-12','2026-12-16','CONFIRMADA',1040000.00),
('1031571416','C03','Cabaña campesina','2026-01-01','2026-11-29','2026-12-03','CONFIRMADA',1280000.00),
('1047017385','104','Sencilla','2026-01-01','2026-12-07','2026-12-10','CONFIRMADA',360000.00),
('1081575609','G01','Glamping','2026-01-01','2026-12-27','2026-12-29','CONFIRMADA',520000.00),
('1091456455','C01','Cabaña campesina','2026-01-01','2026-12-15','2026-12-16','CONFIRMADA',320000.00),
('1003176036','202','Familiar','2026-01-01','2026-12-27','2026-12-28','PENDIENTE',280000.00),
('1042799687','201','Doble','2026-01-01','2026-12-13','2026-12-14','PENDIENTE',180000.00),
('1007535674','204','Doble','2026-01-01','2026-12-24','2026-12-25','PENDIENTE',180000.00),
('AB725950','103','Sencilla','2026-01-01','2026-12-07','2026-12-10','PENDIENTE',360000.00),
('AB350930','G01','Glamping','2027-01-01','2027-02-15','2027-02-17','CONFIRMADA',550000.00),
('AB367359','204','Doble','2027-01-01','2027-01-15','2027-01-16','CONFIRMADA',192000.00),
('AB376631','101','Sencilla','2027-01-01','2027-01-27','2027-01-31','CONFIRMADA',512000.00),
('AB457263','302','Familiar','2027-01-01','2027-01-23','2027-01-27','CONFIRMADA',1192000.00),
('812133','203','Doble','2027-01-01','2027-03-02','2027-03-03','CONFIRMADA',192000.00),
('1056669073','103','Sencilla','2027-01-01','2027-01-26','2027-01-27','CANCELADA',128000.00),
('1067962320','103','Sencilla','2027-01-01','2027-02-23','2027-02-25','CANCELADA',256000.00)
) v(doc,hab,tipo,tini,ent,sal,est,total)
JOIN Huesped hu ON hu.numeroDocumento = v.doc
JOIN Habitacion hb ON hb.numero = v.hab
JOIN TipoHabitacion tp ON tp.nombre = v.tipo
JOIN Tarifa tf ON tf.idTipoHabitacion = tp.idTipoHabitacion AND tf.fechaInicio = CAST(v.tini AS DATE);
GO

/* 6. Comprobantes para reservas confirmadas y finalizadas (formato CR-XXXXXXXX) */
INSERT INTO Comprobante (idReserva, numeroComprobante, fechaGeneracion)
SELECT r.idReserva, 'CR-' + UPPER(LEFT(CONVERT(VARCHAR(64), HASHBYTES('SHA2_256', 'demo-' + CAST(r.idReserva AS VARCHAR(10))), 2), 8)), r.fechaReserva
FROM Reserva r
WHERE r.estado IN ('CONFIRMADA','FINALIZADA') AND NOT EXISTS (SELECT 1 FROM Comprobante c WHERE c.idReserva = r.idReserva);
GO

/* 7. Estado actual de las habitaciones según las reservas de hoy (la tarea de las 00:05 lo mantiene después) */
UPDATE h SET estado = 'RESERVADA'
FROM Habitacion h
WHERE h.estado = 'DISPONIBLE' AND EXISTS (
    SELECT 1 FROM Reserva r WHERE r.idHabitacion = h.idHabitacion AND r.estado IN ('CONFIRMADA','PENDIENTE')
      AND CAST(GETDATE() AS DATE) >= r.fechaEntrada AND CAST(GETDATE() AS DATE) < r.fechaSalida);
GO

SET NOEXEC OFF;
GO
PRINT 'Resumen:';
SELECT (SELECT COUNT(*) FROM Huesped) AS huespedes, (SELECT COUNT(*) FROM Habitacion) AS habitaciones,
       (SELECT COUNT(*) FROM TipoHabitacion) AS tipos, (SELECT COUNT(*) FROM Reserva) AS reservas,
       (SELECT COUNT(*) FROM Comprobante) AS comprobantes;
GO
