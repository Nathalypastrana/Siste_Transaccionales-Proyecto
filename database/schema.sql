IF DB_ID('ReservasHotelDB') IS NULL
BEGIN
    CREATE DATABASE ReservasHotelDB;
END

GO

USE ReservasHotelDB;

GO

/* ELIMINAR TABLAS EXISTENTES */

IF OBJECT_ID('Comprobante', 'U') IS NOT NULL
    DROP TABLE Comprobante;

IF OBJECT_ID('Reserva', 'U') IS NOT NULL
    DROP TABLE Reserva;

IF OBJECT_ID('Tarifa', 'U') IS NOT NULL
    DROP TABLE Tarifa;

IF OBJECT_ID('Huesped', 'U') IS NOT NULL
    DROP TABLE Huesped;

IF OBJECT_ID('Usuario', 'U') IS NOT NULL
    DROP TABLE Usuario;

IF OBJECT_ID('Habitacion', 'U') IS NOT NULL
    DROP TABLE Habitacion;

IF OBJECT_ID('TipoHabitacion', 'U') IS NOT NULL
    DROP TABLE TipoHabitacion;

GO

/* TABLA: TipoHabitacion */
CREATE TABLE TipoHabitacion (
    idTipoHabitacion INT IDENTITY(1,1) PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(255) NULL,
    capacidad INT NOT NULL CHECK (capacidad > 0)
);

GO

/* TABLA: Habitacion */
CREATE TABLE Habitacion (
    idHabitacion INT IDENTITY(1,1) PRIMARY KEY,
    numero VARCHAR(10) NOT NULL UNIQUE,
    piso INT NOT NULL,
    capacidad INT NOT NULL CHECK (capacidad > 0),
    estado VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE',
    idTipoHabitacion INT NOT NULL,

    CONSTRAINT CK_Habitacion_Estado
        CHECK (estado IN (
            'DISPONIBLE',
            'RESERVADA',
            'OCUPADA',
            'MANTENIMIENTO'
        )),

    CONSTRAINT FK_Habitacion_TipoHabitacion
        FOREIGN KEY (idTipoHabitacion)
        REFERENCES TipoHabitacion(idTipoHabitacion)
);

GO

/* TABLA: Usuario */
CREATE TABLE Usuario (
    idUsuario INT IDENTITY(1,1) PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    correo VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    activo BIT NOT NULL DEFAULT 1,

    CONSTRAINT CK_Usuario_Rol
        CHECK (rol IN (
            'RECEPCIONISTA',
            'ADMINISTRADOR'
        ))
);

GO

/* TABLA: Huesped */
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

GO

/* TABLA: Tarifa */
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

GO

/* TABLA: Reserva */
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
        FOREIGN KEY (idHuesped)
        REFERENCES Huesped(idHuesped),

    CONSTRAINT FK_Reserva_Habitacion
        FOREIGN KEY (idHabitacion)
        REFERENCES Habitacion(idHabitacion),

    CONSTRAINT FK_Reserva_Usuario
        FOREIGN KEY (idUsuario)
        REFERENCES Usuario(idUsuario),

    CONSTRAINT FK_Reserva_Tarifa
        FOREIGN KEY (idTarifa)
        REFERENCES Tarifa(idTarifa),

    CONSTRAINT CK_Reserva_Fechas
        CHECK (fechaSalida > fechaEntrada),

    CONSTRAINT CK_Reserva_Estado
        CHECK (estado IN (
            'PENDIENTE',
            'CONFIRMADA',
            'CANCELADA',
            'FINALIZADA'
        ))
);

GO

/* ÍNDICE PARA DISPONIBILIDAD Y CONCURRENCIA */
CREATE INDEX IX_Reserva_Habitacion_Fechas
    ON Reserva(
        idHabitacion,
        fechaEntrada,
        fechaSalida
    )

    INCLUDE (estado);

GO

/* TABLA: Comprobante */
CREATE TABLE Comprobante (
    idComprobante INT IDENTITY(1,1) PRIMARY KEY,
    idReserva INT NOT NULL UNIQUE,
    fechaGeneracion DATETIME NOT NULL DEFAULT GETDATE(),
    numeroComprobante VARCHAR(30) NOT NULL UNIQUE,

    CONSTRAINT FK_Comprobante_Reserva
        FOREIGN KEY (idReserva)
        REFERENCES Reserva(idReserva)
);

GO
