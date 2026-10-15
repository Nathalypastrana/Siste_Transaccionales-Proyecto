package com.hotel.reservas.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;

public record ModificarReservaRequest(
    @NotNull LocalDate fechaEntrada,
    @NotNull LocalDate fechaSalida
) {}