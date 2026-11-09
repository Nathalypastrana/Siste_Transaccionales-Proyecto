/*
  SCRIPT DE CREACIÓN - ReservasHotelDB
  Es NO DESTRUCTIVO: solo crea la base, las tablas y el índice si no existen.
  Se puede ejecutar varias veces sin perder datos.
  Para borrar y reconstruir todo (solo desarrollo) usar reiniciar_base.sql.
*/

IF DB_ID('ReservasHotelDB') IS NULL
BEGIN
    CREATE DATABASE ReservasHotelDB;
END

GO

USE ReservasHotelDB;

GO

/* TABLA: TipoHabitacion */
IF OBJECT_ID('TipoHabitacion', 'U') IS NULL
BEGIN
    CREATE TABLE TipoHabitacion (
        idTipoHabitacion INT IDENTITY(1,1) PRIMARY KEY,
        nombre VARCHAR(50) NOT NULL UNIQUE,
        descripcion VARCHAR(255) NULL,
        capacidad INT NOT NULL CHECK (capacidad > 0)
    );
END

GO

/* TABLA: Habitacion */
IF OBJECT_ID('Habitacion', 'U') IS NULL
BEGIN
    CREATE TABLE Habitacion (
        idHabitacion INT IDENTITY(1,1) PRIMARY KEY,
        numero VARCHAR(10) NOT NULL UNIQUE,
        piso INT NOT NULL,
        capacidad INT NOT NULL CHECK (capacidad > 0),
        estado VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE',
        idTipoHabitacion INT NOT NULL,

        CONSTRAINT CK_Habitacion_Estado
            CHECK (estado IN ('DISPONIBLE', 'RESERVADA', 'OCUPADA', 'MANTENIMIENTO')),

        CONSTRAINT FK_Habitacion_TipoHabitacion
            FOREIGN KEY (idTipoHabitacion)
            REFERENCES TipoHabitacion(idTipoHabitacion)
    );
END

GO

/* TABLA: Usuario */
IF OBJECT_ID('Usuario', 'U') IS NULL
BEGIN
    CREATE TABLE Usuario (
        idUsuario INT IDENTITY(1,1) PRIMARY KEY,
        nombre VARCHAR(100) NOT NULL,
        correo VARCHAR(100) NOT NULL UNIQUE,
        password VARCHAR(255) NOT NULL,
        rol VARCHAR(20) NOT NULL,
        activo BIT NOT NULL DEFAULT 1,

        CONSTRAINT CK_Usuario_Rol
            CHECK (rol IN ('RECEPCIONISTA', 'ADMINISTRADOR', 'ADMIN_GENERAL'))
    );
END

GO

/* TABLA: Huesped */
IF OBJECT_ID('Huesped', 'U') IS NULL
BEGIN
    CREATE TABLE Huesped (
        idHuesped INT IDENTITY(1,1) PRIMARY KEY,
        tipoDocumento VARCHAR(20) NOT NULL,
        numeroDocumento VARCHAR(30) NOT NULL,
        nombres VARCHAR(100) NOT NULL,
        apellidos VARCHAR(100) NOT NULL,
        telefono VARCHAR(20) NULL,
        correo VARCHAR(100) NULL,

        CONSTRAINT UQ_Huesped_Documento
            UNIQUE (tipoDocumento, numeroDocumento)
    );
END

GO

/* TABLA: Tarifa */
IF OBJECT_ID('Tarifa', 'U') IS NULL
BEGIN
    CREATE TABLE Tarifa (
        idTarifa INT IDENTITY(1,1) PRIMARY KEY,
        valor DECIMAL(12,2) NOT NULL CHECK (valor > 0),
        fechaInicio DATE NOT NULL,
        fechaFin DATE NOT NULL,
        activa BIT NOT NULL DEFAULT 1,
        idTipoHabitacion INT NOT NULL,

        CONSTRAINT FK_Tarifa_TipoHabitacion
            FOREIGN KEY (idTipoHabitacion)
            REFERENCES TipoHabitacion(idTipoHabitacion),

        CONSTRAINT CK_Tarifa_Fechas
            CHECK (fechaFin > fechaInicio)
    );
END

GO

/* TABLA: Reserva */
IF OBJECT_ID('Reserva', 'U') IS NULL
BEGIN
    CREATE TABLE Reserva (
        idReserva INT IDENTITY(1,1) PRIMARY KEY,
        idHuesped INT NOT NULL,
        idHabitacion INT NOT NULL,
        idUsuario INT NOT NULL,
        idTarifa INT NOT NULL,
        fechaEntrada DATE NOT NULL,
        fechaSalida DATE NOT NULL,
        fechaReserva DATETIME NOT NULL DEFAULT GETDATE(),
        estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
        valorTotal DECIMAL(12,2) NOT NULL CHECK (valorTotal >= 0),

        CONSTRAINT FK_Reserva_Huesped
            FOREIGN KEY (idHuesped) REFERENCES Huesped(idHuesped),

        CONSTRAINT FK_Reserva_Habitacion
            FOREIGN KEY (idHabitacion) REFERENCES Habitacion(idHabitacion),

        CONSTRAINT FK_Reserva_Usuario
            FOREIGN KEY (idUsuario) REFERENCES Usuario(idUsuario),

        CONSTRAINT FK_Reserva_Tarifa
            FOREIGN KEY (idTarifa) REFERENCES Tarifa(idTarifa),

        CONSTRAINT CK_Reserva_Fechas
            CHECK (fechaSalida > fechaEntrada),

        CONSTRAINT CK_Reserva_Estado
            CHECK (estado IN ('PENDIENTE', 'CONFIRMADA', 'CANCELADA', 'FINALIZADA'))
    );
END

GO

/* ÍNDICE PARA DISPONIBILIDAD Y CONCURRENCIA */
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_Reserva_Habitacion_Fechas'
      AND object_id = OBJECT_ID('Reserva')
)
BEGIN
    CREATE INDEX IX_Reserva_Habitacion_Fechas
        ON Reserva(idHabitacion, fechaEntrada, fechaSalida)
        INCLUDE (estado);
END

GO

/* TABLA: Comprobante */
IF OBJECT_ID('Comprobante', 'U') IS NULL
BEGIN
    CREATE TABLE Comprobante (
        idComprobante INT IDENTITY(1,1) PRIMARY KEY,
        idReserva INT NOT NULL UNIQUE,
        fechaGeneracion DATETIME NOT NULL DEFAULT GETDATE(),
        numeroComprobante VARCHAR(30) NOT NULL UNIQUE,

        CONSTRAINT FK_Comprobante_Reserva
            FOREIGN KEY (idReserva) REFERENCES Reserva(idReserva)
    );
END

GO

/* TABLA: AutorizacionDatos
   Autorización del tratamiento de datos personales en el consultor virtual (Ley 1581 de 2012):
   guarda cuándo aceptó la persona y qué versión del aviso vio. El teléfono son sus últimos 10 dígitos. */
IF OBJECT_ID('AutorizacionDatos', 'U') IS NULL
BEGIN
    CREATE TABLE AutorizacionDatos (
        idAutorizacion INT IDENTITY(1,1) PRIMARY KEY,
        telefono VARCHAR(10) NOT NULL UNIQUE,
        fechaAutorizacion DATETIME NOT NULL DEFAULT GETDATE(),
        versionAviso VARCHAR(20) NOT NULL,
        revocada BIT NOT NULL DEFAULT 0,
        fechaRevocacion DATETIME NULL
    );
END

GO

GO

/* MIGRACION (fase 9): permitir el rol ADMIN_GENERAL en bases creadas antes. Idempotente. */
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_Usuario_Rol'
           AND definition NOT LIKE '%ADMIN_GENERAL%')
BEGIN
    ALTER TABLE Usuario DROP CONSTRAINT CK_Usuario_Rol;
    ALTER TABLE Usuario ADD CONSTRAINT CK_Usuario_Rol
        CHECK (rol IN ('RECEPCIONISTA', 'ADMINISTRADOR', 'ADMIN_GENERAL'));
END
GO

/* TABLA: Auditoria (fase 9) - quién hizo qué y cuándo. Solo se inserta; nunca guarda contraseñas ni tokens. */
IF OBJECT_ID('Auditoria', 'U') IS NULL
BEGIN
    CREATE TABLE Auditoria (
        idAuditoria INT IDENTITY(1,1) PRIMARY KEY,
        fecha DATETIME NOT NULL DEFAULT GETDATE(),
        idUsuario INT NULL,
        correo VARCHAR(100) NULL,
        rol VARCHAR(20) NULL,
        accion VARCHAR(40) NOT NULL,
        entidad VARCHAR(40) NULL,
        entidadId VARCHAR(40) NULL,
        detalle VARCHAR(500) NULL,
        ip VARCHAR(45) NULL
    );
    CREATE INDEX IX_Auditoria_fecha ON Auditoria (fecha DESC);
END
GO

/* TABLA: RecuperacionClave (fase 9) - enlaces de recuperación de contraseña (solo se guarda el hash del token). */
IF OBJECT_ID('RecuperacionClave', 'U') IS NULL
BEGIN
    CREATE TABLE RecuperacionClave (
        idRecuperacion INT IDENTITY(1,1) PRIMARY KEY,
        idUsuario INT NOT NULL,
        tokenHash VARCHAR(64) NOT NULL UNIQUE,
        creada DATETIME NOT NULL DEFAULT GETDATE(),
        expira DATETIME NOT NULL,
        usado BIT NOT NULL DEFAULT 0
    );
    CREATE INDEX IX_RecuperacionClave_usuario ON RecuperacionClave (idUsuario, creada);
END
GO
