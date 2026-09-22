USE ReservasHotelDB;
GO

/* CONSULTAR DATOS DE PRUEBA */
SELECT * FROM TipoHabitacion;
SELECT * FROM Habitacion;
SELECT * FROM Usuario;
SELECT * FROM Huesped;
SELECT * FROM Tarifa;
SELECT * FROM Reserva;
SELECT * FROM Comprobante;
GO

/*  VERIFICAR RESTRICCIONES */
SELECT
    TABLE_NAME,
    CONSTRAINT_NAME,
    CONSTRAINT_TYPE
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
WHERE TABLE_NAME IN (
    'TipoHabitacion',
    'Habitacion',
    'Usuario',
    'Huesped',
    'Tarifa',
    'Reserva',
    'Comprobante'
)
ORDER BY TABLE_NAME, CONSTRAINT_TYPE;
GO

/* VERIFICAR CLAVES FORÁNEAS */
SELECT
    fk.name AS NombreClaveForanea,
    OBJECT_NAME(fk.parent_object_id) AS TablaOrigen,
    COL_NAME(
        fkc.parent_object_id,
        fkc.parent_column_id
    ) AS ColumnaOrigen,
    OBJECT_NAME(fk.referenced_object_id) AS TablaDestino,
    COL_NAME(
        fkc.referenced_object_id,
        fkc.referenced_column_id
    ) AS ColumnaDestino
FROM sys.foreign_keys AS fk
INNER JOIN sys.foreign_key_columns AS fkc
    ON fk.object_id = fkc.constraint_object_id
ORDER BY TablaOrigen;
GO

/* La siguiente inserción DEBE ser rechazada porque fechaSalida es anterior a fechaEntrada */
INSERT INTO Reserva
(
    idHuesped,
    idHabitacion,
    idUsuario,
    idTarifa,
    fechaEntrada,
    fechaSalida,
    estado,
    valorTotal
)
VALUES
(
    1,
    1,
    2,
    1,
    '2026-10-10',
    '2026-10-05',
    'PENDIENTE',
    100000
);
GO