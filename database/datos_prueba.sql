USE ReservasHotelDB;
GO

/* TIPOS DE HABITACIÓN */
INSERT INTO TipoHabitacion (nombre, descripcion, capacidad)
VALUES
('Sencilla', 'Habitación para una persona', 1),
('Doble', 'Habitación para dos personas', 2),
('Familiar', 'Habitación para familias', 4);
GO

/* HABITACIONES */
INSERT INTO Habitacion
(numero, piso, capacidad, estado, idTipoHabitacion)
VALUES
('101', 1, 1, 'DISPONIBLE', 1),
('102', 1, 2, 'DISPONIBLE', 2),
('201', 2, 2, 'DISPONIBLE', 2),
('202', 2, 4, 'DISPONIBLE', 3),
('301', 3, 4, 'MANTENIMIENTO', 3);
GO

/* USUARIOS */
/* Cuentas de ejemplo INACTIVAS y con hash BCrypt de una clave aleatoria que nadie conoce: sirven solo para las
   llaves foráneas de las reservas de ejemplo. El acceso real se crea con ADMIN_CORREO / ADMIN_PASSWORD o desde
   Administración > Usuarios. Nunca guardes contraseñas en texto plano en el repositorio. */
INSERT INTO Usuario
(nombre, correo, password, rol, activo)
VALUES
('Administrador', 'admin@hotel.com', '$2b$10$DgbtflTZsKqK9x/jXjrAvegjhq.uzD6H33ODehT5jwr34RbBkj3Qi', 'ADMINISTRADOR', 0),
('Recepcionista', 'recepcion@hotel.com', '$2b$10$DgbtflTZsKqK9x/jXjrAvegjhq.uzD6H33ODehT5jwr34RbBkj3Qi', 'RECEPCIONISTA', 0);
GO

/* HUÉSPEDES */
INSERT INTO Huesped
(tipoDocumento, numeroDocumento, nombres, apellidos, telefono, correo)
VALUES
('CC', '1001001001', 'Laura', 'Pastrana', '3001112233', 'laura@email.com'),
('CC', '1002002002', 'Ariadna', 'Cardona', '3002223344', 'ariadna@email.com'),
('CC', '1003003003', 'Carlos', 'Gómez', '3003334455', 'carlos@email.com');
GO

/* TARIFAS */
INSERT INTO Tarifa
(valor, fechaInicio, fechaFin, activa, idTipoHabitacion)
VALUES
(120000.00, '2026-01-01', '2026-12-31', 1, 1),
(180000.00, '2026-01-01', '2026-12-31', 1, 2),
(280000.00, '2026-01-01', '2026-12-31', 1, 3);
GO

/* RESERVAS */
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
    1, 1, 2, 1,
    '2026-10-01', '2026-10-03',
    'CONFIRMADA', 240000.00
),
(
    2, 2, 2, 2,
    '2026-10-05', '2026-10-08',
    'PENDIENTE', 540000.00
),
(
    3, 4, 1, 3,
    '2026-10-10', '2026-10-12',
    'CONFIRMADA', 560000.00
);
GO

/* COMPROBANTES */
INSERT INTO Comprobante
(idReserva, numeroComprobante)
VALUES
(1, 'COMP-0001'),
(2, 'COMP-0002'),
(3, 'COMP-0003');
GO