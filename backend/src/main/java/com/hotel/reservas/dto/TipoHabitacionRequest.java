package com.hotel.reservas.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TipoHabitacionRequest(
        @NotBlank(message = "El nombre del tipo es obligatorio") @Size(max = 50, message = "El nombre admite hasta 50 caracteres") String nombre,
        @Size(max = 255, message = "La descripción admite hasta 255 caracteres") String descripcion,
        @NotNull(message = "La capacidad es obligatoria") @Min(value = 1, message = "La capacidad debe ser mayor que cero") Integer capacidad
) {
}
