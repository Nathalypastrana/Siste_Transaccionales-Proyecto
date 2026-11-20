-- Requerido por los índices filtrados (UX_Huesped_Correo, UX_Huesped_Telefono): sqlcmd usa QUOTED_IDENTIFIER OFF por defecto
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

/* RESTRICCIONES DE UNICIDAD PARA HUÉSPEDES
   La base de datos garantiza (además del backend) que no existan dos huéspedes con:
     - el mismo número de documento (sin importar el tipo),
     - el mismo nombre completo (nombres + apellidos; la intercalación de SQL Server no distingue mayúsculas),
     - el mismo correo,
     - el mismo teléfono.
   Así la regla se cumple incluso si llegan dos registros al mismo tiempo (integridad transaccional).

   Antes de crear los índices: normaliza los datos existentes (teléfono solo dígitos, correo en minúsculas)
   y, si encuentra duplicados, los lista y NO cambia nada más: corrígelos desde la pantalla Huéspedes y vuelve a correrlo.
   Idempotente: se puede ejecutar varias veces.
   Uso: sqlcmd -S localhost -U sa -C -d ReservasHotelDB -i unicidad_huespedes.sql   (clave por SQLCMDPASSWORD) */
USE ReservasHotelDB;
GO
SET NOCOUNT ON;

/* 1. Normalizar datos existentes */
UPDATE Huesped
SET telefono = NULLIF(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(telefono)),
                   ' ', ''), '-', ''), '(', ''), ')', ''), '+', ''), '.', ''), '')
WHERE telefono IS NOT NULL;

UPDATE Huesped SET correo = NULLIF(LOWER(LTRIM(RTRIM(correo))), '') WHERE correo IS NOT NULL;
UPDATE Huesped SET nombres = LTRIM(RTRIM(nombres)), apellidos = LTRIM(RTRIM(apellidos));
GO

/* 2. Buscar duplicados */
DECLARE @duplicados INT = 0;

CREATE TABLE #dup (problema VARCHAR(40), valor NVARCHAR(210), cantidad INT);

INSERT INTO #dup
SELECT 'Documento repetido', numeroDocumento, COUNT(*)
FROM Huesped GROUP BY numeroDocumento HAVING COUNT(*) > 1;

INSERT INTO #dup
SELECT 'Nombre completo repetido', nombres + ' ' + apellidos, COUNT(*)
FROM Huesped GROUP BY nombres, apellidos HAVING COUNT(*) > 1;

INSERT INTO #dup
SELECT 'Correo repetido', correo, COUNT(*)
FROM Huesped WHERE correo IS NOT NULL GROUP BY correo HAVING COUNT(*) > 1;

INSERT INTO #dup
SELECT 'Teléfono repetido', telefono, COUNT(*)
FROM Huesped WHERE telefono IS NOT NULL GROUP BY telefono HAVING COUNT(*) > 1;

SELECT @duplicados = COUNT(*) FROM #dup;
IF @duplicados > 0
BEGIN
    PRINT 'Hay huéspedes duplicados: corrígelos desde la pantalla Huéspedes (botón Editar) y vuelve a ejecutar este script.';
    SELECT d.problema, h.idHuesped, h.tipoDocumento, h.numeroDocumento, h.nombres, h.apellidos, h.telefono, h.correo
    FROM #dup d
    JOIN Huesped h ON (d.problema = 'Documento repetido' AND h.numeroDocumento = d.valor)
                   OR (d.problema = 'Nombre completo repetido' AND h.nombres + ' ' + h.apellidos = d.valor)
                   OR (d.problema = 'Correo repetido' AND h.correo = d.valor)
                   OR (d.problema = 'Teléfono repetido' AND h.telefono = d.valor)
    ORDER BY d.problema, d.valor;
    DROP TABLE #dup;
    SET NOEXEC ON;
END
ELSE
    DROP TABLE #dup;
GO

/* 3. Índices únicos (los de correo y teléfono ignoran los valores vacíos) */
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_Huesped_NumeroDocumento')
    CREATE UNIQUE INDEX UX_Huesped_NumeroDocumento ON Huesped(numeroDocumento);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_Huesped_NombreCompleto')
    CREATE UNIQUE INDEX UX_Huesped_NombreCompleto ON Huesped(nombres, apellidos);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_Huesped_Correo')
    CREATE UNIQUE INDEX UX_Huesped_Correo ON Huesped(correo) WHERE correo IS NOT NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_Huesped_Telefono')
    CREATE UNIQUE INDEX UX_Huesped_Telefono ON Huesped(telefono) WHERE telefono IS NOT NULL;
PRINT 'Restricciones de unicidad de huéspedes aplicadas.';
GO
SET NOEXEC OFF;
GO
SELECT name AS indice, is_unique AS unico, filter_definition AS filtro
FROM sys.indexes WHERE object_id = OBJECT_ID('Huesped') AND name LIKE 'U%' ORDER BY name;
GO
