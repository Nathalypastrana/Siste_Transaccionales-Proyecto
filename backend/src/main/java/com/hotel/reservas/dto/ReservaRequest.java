package com.hotel.reservas.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** El usuario que registra la reserva sale del token JWT, no del cuerpo de la petición. */
public record ReservaRequest(
        @NotNull(message = "Selecciona un huésped") Integer huespedId,
        @NotNull(message = "Selecciona una habitación") Integer habitacionId,
        @NotNull(message = "La fecha de entrada es obligatoria") LocalDate fechaEntrada,
        @NotNull(message = "La fecha de salida es obligatoria") LocalDate fechaSalida
) {
}
