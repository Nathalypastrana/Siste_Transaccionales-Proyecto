/*
  ¡ATENCIÓN! SCRIPT DESTRUCTIVO - SOLO PARA DESARROLLO
  Elimina TODAS las tablas de ReservasHotelDB y con ellas todos los datos
  (huéspedes, reservas, comprobantes...). No usar en una base con información real.

  Para volver a dejar la base lista, ejecutar después:
    1) schema.sql
    2) datos_prueba.sql
*/

IF DB_ID('ReservasHotelDB') IS NULL
BEGIN
    PRINT 'La base ReservasHotelDB no existe; no hay nada que eliminar.';
    RETURN;
END

GO

USE ReservasHotelDB;

GO

IF OBJECT_ID('AutorizacionDatos', 'U') IS NOT NULL DROP TABLE AutorizacionDatos;
IF OBJECT_ID('Auditoria', 'U') IS NOT NULL DROP TABLE Auditoria;
IF OBJECT_ID('RecuperacionClave', 'U') IS NOT NULL DROP TABLE RecuperacionClave;
IF OBJECT_ID('Comprobante', 'U') IS NOT NULL DROP TABLE Comprobante;
IF OBJECT_ID('Reserva', 'U') IS NOT NULL DROP TABLE Reserva;
IF OBJECT_ID('Tarifa', 'U') IS NOT NULL DROP TABLE Tarifa;
IF OBJECT_ID('Huesped', 'U') IS NOT NULL DROP TABLE Huesped;
IF OBJECT_ID('Usuario', 'U') IS NOT NULL DROP TABLE Usuario;
IF OBJECT_ID('Habitacion', 'U') IS NOT NULL DROP TABLE Habitacion;
IF OBJECT_ID('TipoHabitacion', 'U') IS NOT NULL DROP TABLE TipoHabitacion;

GO
