package com.hotel.reservas.exception;

public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() {
        super("Correo o contraseña incorrectos, o la cuenta está inactiva.");
    }
}
