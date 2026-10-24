package com.hotel.reservas.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * usuarioId es el recepcionista que registra la reserva.
 * Cuando se implemente JWT, saldrá del token y se quitará de aquí.
 */
public record ReservaRequest(
        @NotNull Integer huespedId,
        @NotNull Integer habitacionId,
        @NotNull Integer usuarioId,
        @NotNull @FutureOrPresent LocalDate fechaEntrada,
        @NotNull LocalDate fechaSalida
) {
}
