package com.hotel.reservas.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ReservaRequest(
        @NotNull Long huespedId,
        @NotNull Long habitacionId,
        @NotNull @FutureOrPresent LocalDate fechaEntrada,
        @NotNull LocalDate fechaSalida
) {
}
