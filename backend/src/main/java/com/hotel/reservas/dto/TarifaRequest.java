package com.hotel.reservas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TarifaRequest(
        @NotNull(message = "Selecciona el tipo de habitación") Integer tipoHabitacionId,
        @NotNull(message = "El valor es obligatorio") @DecimalMin(value = "0.01", message = "El valor de la tarifa debe ser mayor que cero") BigDecimal valor,
        @NotNull(message = "La fecha de inicio es obligatoria") LocalDate fechaInicio,
        @NotNull(message = "La fecha de fin es obligatoria") LocalDate fechaFin,
        Boolean activa
) {
}
