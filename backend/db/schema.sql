-- Esquema inicial - Sistema transaccional de reservas de hotel (SQL Server)
-- Ejecutar sobre una base de datos vacía:  CREATE DATABASE hotel_reservas;  USE hotel_reservas;

CREATE TABLE tipo_habitacion (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    nombre      VARCHAR(50)  NOT NULL UNIQUE,
    descripcion VARCHAR(255) NULL,
    capacidad   INT          NOT NULL CHECK (capacidad > 0)
);

CREATE TABLE habitacion (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    numero              VARCHAR(10) NOT NULL UNIQUE,
    piso                INT         NOT NULL,
    estado              VARCHAR(30) NOT NULL DEFAULT 'DISPONIBLE'
                        CHECK (estado IN ('DISPONIBLE','MANTENIMIENTO','FUERA_DE_SERVICIO')),
    tipo_habitacion_id  BIGINT      NOT NULL,
    CONSTRAINT fk_habitacion_tipo FOREIGN KEY (tipo_habitacion_id) REFERENCES tipo_habitacion(id)
);

CREATE TABLE usuario (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    nombre        VARCHAR(100) NOT NULL,
    email         VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    rol           VARCHAR(20)  NOT NULL CHECK (rol IN ('ADMIN','RECEPCIONISTA')),
    activo        BIT          NOT NULL DEFAULT 1
);

CREATE TABLE huesped (
    id        BIGINT IDENTITY(1,1) PRIMARY KEY,
    nombre    VARCHAR(80)  NOT NULL,
    apellido  VARCHAR(80)  NOT NULL,
    documento VARCHAR(30)  NOT NULL UNIQUE,
    email     VARCHAR(120) NULL,
    telefono  VARCHAR(30)  NULL
);

CREATE TABLE tarifa (
    id                 BIGINT IDENTITY(1,1) PRIMARY KEY,
    tipo_habitacion_id BIGINT        NOT NULL,
    precio_noche       DECIMAL(12,2) NOT NULL CHECK (precio_noche > 0),
    fecha_inicio       DATE          NOT NULL,
    fecha_fin          DATE          NOT NULL,
    CONSTRAINT fk_tarifa_tipo FOREIGN KEY (tipo_habitacion_id) REFERENCES tipo_habitacion(id),
    CONSTRAINT ck_tarifa_fechas CHECK (fecha_fin >= fecha_inicio)
);

CREATE TABLE reserva (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    huesped_id     BIGINT        NOT NULL,
    habitacion_id  BIGINT        NOT NULL,
    usuario_id     BIGINT        NULL,
    fecha_entrada  DATE          NOT NULL,
    fecha_salida   DATE          NOT NULL,
    estado         VARCHAR(20)   NOT NULL DEFAULT 'CONFIRMADA'
                   CHECK (estado IN ('CONFIRMADA','CANCELADA','FINALIZADA')),
    total          DECIMAL(12,2) NOT NULL,
    fecha_creacion DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_reserva_huesped    FOREIGN KEY (huesped_id)    REFERENCES huesped(id),
    CONSTRAINT fk_reserva_habitacion FOREIGN KEY (habitacion_id) REFERENCES habitacion(id),
    CONSTRAINT fk_reserva_usuario    FOREIGN KEY (usuario_id)    REFERENCES usuario(id),
    CONSTRAINT ck_reserva_fechas CHECK (fecha_salida > fecha_entrada)
);

CREATE INDEX ix_reserva_habitacion_fechas ON reserva (habitacion_id, fecha_entrada, fecha_salida);

CREATE TABLE comprobante_reserva (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    reserva_id    BIGINT        NOT NULL UNIQUE,
    numero        VARCHAR(30)   NOT NULL UNIQUE,
    fecha_emision DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    total         DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_comprobante_reserva FOREIGN KEY (reserva_id) REFERENCES reserva(id)
);

-- Datos de prueba mínimos
INSERT INTO tipo_habitacion (nombre, descripcion, capacidad) VALUES
    ('Sencilla', 'Una cama sencilla', 1),
    ('Doble',    'Dos camas o una cama doble', 2),
    ('Suite',    'Suite con sala', 4);

INSERT INTO habitacion (numero, piso, tipo_habitacion_id) VALUES
    ('101', 1, 1), ('102', 1, 2), ('201', 2, 2), ('301', 3, 3);

INSERT INTO tarifa (tipo_habitacion_id, precio_noche, fecha_inicio, fecha_fin) VALUES
    (1,  90000, '2026-01-01', '2026-12-31'),
    (2, 140000, '2026-01-01', '2026-12-31'),
    (3, 280000, '2026-01-01', '2026-12-31');

INSERT INTO huesped (nombre, apellido, documento, email, telefono) VALUES
    ('Ana', 'Prueba', '1000000001', 'ana@example.com', '3000000000');
