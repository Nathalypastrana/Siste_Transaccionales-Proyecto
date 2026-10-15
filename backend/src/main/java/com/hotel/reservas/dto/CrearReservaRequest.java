package com.hotel.reservas.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;

public record CrearReservaRequest(
    @NotNull Integer huespedId,
    @NotNull Integer habitacionId,
    @NotNull Integer usuarioId,
    @NotNull LocalDate fechaEntrada,
    @NotNull LocalDate fechaSalida
) {}