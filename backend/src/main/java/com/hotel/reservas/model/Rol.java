package com.hotel.reservas.model;

/**
 * RECEPCIONISTA: operación diaria (reservas, huéspedes, consulta de habitaciones).
 * ADMINISTRADOR: administrador del hotel (habitaciones, tipos, tarifas, reportes y cuentas de recepcionistas).
 * ADMIN_GENERAL: administrador general del programa (todo lo anterior, cuentas de cualquier rol y estado del sistema).
 */
public enum Rol {
    RECEPCIONISTA,
    ADMINISTRADOR,
    ADMIN_GENERAL
}
