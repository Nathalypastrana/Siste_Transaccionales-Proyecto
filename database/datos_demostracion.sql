/* DATOS DE DEMOSTRACIÓN (simulados): 50 huéspedes, hotel completo de 15 habitaciones y 5 tipos, tarifas 2026-2027 y reservas con comprobantes.
   Personas y contactos INVENTADOS: los teléfonos/correos no corresponden a nadie real a propósito.
   Se inserta directo en SQL, por lo que NO dispara notificaciones. Si luego reservas por la interfaz a estos huéspedes,
   el sistema intentaría escribirles: usa tu propio teléfono/correo o pon NOTIFICACIONES_CANALES vacío.
   Requisitos: schema.sql aplicado y al menos un usuario (el administrador general). Idempotente: si ya se cargó, no hace nada.
   Uso:  sqlcmd -S localhost -U sa -C -d ReservasHotelDB -i datos_demostracion.sql   (clave por SQLCMDPASSWORD) */
USE ReservasHotelDB;
GO
IF EXISTS (SELECT 1 FROM Huesped WHERE numeroDocumento = '1056669073') OR NOT EXISTS (SELECT 1 FROM Usuario)
BEGIN
    PRINT 'Datos de demostración ya cargados (o no hay usuarios): no se hace nada.';
    SET NOEXEC ON;
END
GO
SET NOCOUNT ON;

/* 1. Tipos de habitación */
INSERT INTO TipoHabitacion (nombre, descripcion, capacidad)
SELECT v.n, v.d, v.c FROM (VALUES
('Cabaña campesina','Cabaña independiente con chimenea y vista a la montaña',4),
('Glamping','Domo de glamping en medio de la naturaleza',2)
) v(n,d,c) WHERE NOT EXISTS (SELECT 1 FROM TipoHabitacion t WHERE t.nombre = v.n);
GO

/* 2. Habitaciones (101, 102, 201, 202 y 301 ya existen en datos_prueba.sql) */
INSERT INTO Habitacion (numero, piso, capacidad, estado, idTipoHabitacion)
SELECT v.num, v.piso, v.cap, v.est, t.idTipoHabitacion FROM (VALUES
('103',1,1,'DISPONIBLE','Sencilla'),
('104',1,1,'DISPONIBLE','Sencilla'),
('203',2,2,'DISPONIBLE','Doble'),
('204',2,2,'DISPONIBLE','Doble'),
('302',3,4,'DISPONIBLE','Familiar'),
('C01',1,4,'DISPONIBLE','Cabaña campesina'),
('C02',1,4,'DISPONIBLE','Cabaña campesina'),
('C03',1,4,'DISPONIBLE','Cabaña campesina'),
('G01',1,2,'DISPONIBLE','Glamping'),
('G02',1,2,'DISPONIBLE','Glamping')
) v(num,piso,cap,est,tipo)
JOIN TipoHabitacion t ON t.nombre = v.tipo
WHERE NOT EXISTS (SELECT 1 FROM Habitacion h WHERE h.numero = v.num);
GO

/* 3. Tarifas: los 5 tipos del hotel: 2026 (los 2 nuevos; los otros 3 ya la tienen) y 2027 (todos). AJUSTA LOS VALORES A LOS PRECIOS REALES */
INSERT INTO Tarifa (valor, fechaInicio, fechaFin, activa, idTipoHabitacion)
SELECT v.val, v.ini, v.fin, 1, t.idTipoHabitacion FROM (VALUES
('Cabaña campesina',320000.00,'2026-01-01','2026-12-31'),
('Glamping',260000.00,'2026-01-01','2026-12-31'),
('Sencilla',128000.00,'2027-01-01','2027-12-31'),
('Doble',192000.00,'2027-01-01','2027-12-31'),
('Familiar',298000.00,'2027-01-01','2027-12-31'),
('Cabaña campesina',340000.00,'2027-01-01','2027-12-31'),
('Glamping',275000.00,'2027-01-01','2027-12-31')
) v(tipo,val,ini,fin)
JOIN TipoHabitacion t ON t.nombre = v.tipo
WHERE NOT EXISTS (SELECT 1 FROM Tarifa x WHERE x.idTipoHabitacion = t.idTipoHabitacion AND x.fechaInicio = CAST(v.ini AS DATE));
GO

/* 4. Huéspedes simulados (50) */
INSERT INTO Huesped (tipoDocumento, numeroDocumento, nombres, apellidos, telefono, correo)
VALUES
('CC','1056669073','Katherine','Acevedo Mesa','3017690306','katherine.acevedo75@yahoo.com'),
('CC','1067962320','Héctor','Torres Becerra','3049238128','hector.torres51@outlook.com'),
('CC','1064315788','Paula Andrea','Fonseca Villamil','3509690383','paula.fonseca4@hotmail.com'),
('CC','1094578218','Pedro','Díaz López','3502720946','pedro.diaz92@yahoo.com'),
('CC','1064708126','Jairo','Quintero Vargas','3053047042','jairo.quintero81@yahoo.com'),
('CC','1033689519','Hernán','Quintero Ariza','3507745788','hernan.quintero86@outlook.com'),
('CC','1093727401','Jhon Edison','Montañez Niño','3207743569','jhon.montanez56@hotmail.com'),
('CC','1001245123','Gloria','Medina Suárez','3003513960','gloria.medina84@yahoo.com'),
('CC','1048309773','Sofía','Páez Beltrán','3055363930','sofia.paez89@yahoo.com'),
('CC','1066727300','Mateo','Mesa Niño','3161564049','mateo.mesa38@outlook.com'),
('CC','1058847902','Camila','Mesa Ramírez','3505342466','camila.mesa58@hotmail.com'),
('CC','1008629549','Miguel Ángel','Becerra Quintero','3109362112','miguel.becerra48@gmail.com'),
('CC','1019643929','Andrés Felipe','Rojas Fonseca','3201235427','andres.rojas12@gmail.com'),
('CC','1073362139','Claudia','Cuervo Ospina','3014335171','claudia.cuervo32@gmail.com'),
('CC','1073343091','Fabián','Parra Martínez','3106993671','fabian.parra20@gmail.com'),
('CC','1077079038','Camilo','Salamanca Ospina','3203262652','camilo.salamanca70@hotmail.com'),
('CC','1017611550','Tatiana','Quintero Ospina','3049467819','tatiana.quintero38@gmail.com'),
('CC','1018165850','Cristian','Fonseca Castro','3046133900','cristian.fonseca65@yahoo.com'),
('CC','1069797368','Diego','Rincón Vargas','3057308337','diego.rincon2@gmail.com'),
('CC','1006895485','Nicolás','Rincón García','3156929115','nicolas.rincon99@gmail.com'),
('CC','1063890916','Oscar','Gómez Niño','3156094766','oscar.gomez58@outlook.com'),
('CC','1062623684','Mónica','Torres Beltrán','3043864434','monica.torres20@hotmail.com'),
('CC','1045153375','Luis Alberto','Becerra Parra','3204914569','luis.becerra87@gmail.com'),
('CC','1004583896','María Fernanda','Montañez Barón','3053908563','maria.montanez6@gmail.com'),
('CC','1064940010','Diana Carolina','Cuervo Rojas','3003166605','diana.cuervo83@hotmail.com'),
('CC','1021527272','Ricardo','Páez Díaz','3027542635','ricardo.paez35@yahoo.com'),
('CC','1010557407','Luisa Fernanda','Ariza Fonseca','3166115528','luisa.ariza24@outlook.com'),
('CC','1062265651','Carolina','Moreno Cárdenas','3207974916','carolina.moreno37@yahoo.com'),
('CC','1069703978','Santiago','Moreno Salamanca','3208458863','santiago.moreno16@outlook.com'),
('CC','1031049233','Adriana','Ramírez Ortiz','3002562287','adriana.ramirez59@outlook.com'),
('CC','1089979425','Jorge Eduardo','Fonseca Ariza','3021456084','jorge.fonseca46@hotmail.com'),
('CC','1092228670','Johana','Cely Montañez','3044632979','johana.cely15@outlook.com'),
('CC','1098109198','Sebastián','Montañez Villamil','3055944247','sebastian.montanez73@outlook.com'),
('CC','1013617158','Juan Carlos','Montañez Díaz','3102617839','juan.montanez56@hotmail.com'),
('CC','1047815518','Iván','Ramírez López','3022783754','ivan.ramirez65@outlook.com'),
('CC','1008578530','Daniela','Suárez Ospina','3106278142','daniela.suarez21@outlook.com'),
('CC','1001861671','Valeria','Cárdenas García','3165312449','valeria.cardenas54@gmail.com'),
('CC','1031571416','Sandra Milena','López Ramírez','3006199572','sandra.lopez16@gmail.com'),
('CC','1047017385','Juliana','Montañez Villamil','3158651160','juliana.montanez78@gmail.com'),
('CC','1081575609','Laura Valentina','Barón Salamanca','3058762311','laura.baron55@gmail.com'),
('CC','1091456455','Ángela','Acevedo Niño','3107527130','angela.acevedo38@outlook.com'),
('CC','1003176036','Yenny','López Becerra','3106545676','yenny.lopez38@gmail.com'),
('CC','1042799687','Lina Marcela','Bernal Fonseca','3041340075','lina.bernal70@outlook.com'),
('CC','1007535674','Wilson','Rojas Montañez','3013719991','wilson.rojas80@gmail.com'),
('PA','AB725950','Daniel','Torres Martínez','3101483086','daniel.torres55@hotmail.com'),
('PA','AB350930','Martha','Cely Fonseca','3164683694','martha.cely17@hotmail.com'),
('PA','AB367359','Liliana','Ariza Gómez','3024829496','liliana.ariza3@gmail.com'),
('PA','AB376631','Natalia','Suárez Ariza','3153326285','natalia.suarez81@gmail.com'),
('PA','AB457263','Alejandro','Moreno Montañez','3506326731','alejandro.moreno8@gmail.com'),
('CE','812133','Julián','Pardo Quintero','3503530789','julian.pardo96@yahoo.com');
GO

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
