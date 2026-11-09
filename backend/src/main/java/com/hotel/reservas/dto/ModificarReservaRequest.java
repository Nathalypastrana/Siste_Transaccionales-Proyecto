package com.hotel.reservas.dto;

import java.time.LocalDate;

/** Todos los campos son opcionales: solo se cambia lo que se envía. */
public record ModificarReservaRequest(Integer habitacionId, LocalDate fechaEntrada, LocalDate fechaSalida) {
}
