package com.hotel.reservas.dto;

import com.hotel.reservas.model.EstadoHabitacion;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HabitacionRequest(
        @NotBlank(message = "El número de habitación es obligatorio") @Size(max = 10, message = "El número admite hasta 10 caracteres") String numero,
        @NotNull(message = "El piso es obligatorio") Integer piso,
        @NotNull(message = "La capacidad es obligatoria") @Min(value = 1, message = "La capacidad debe ser mayor que cero") Integer capacidad,
        EstadoHabitacion estado,
        @NotNull(message = "Selecciona el tipo de habitación") Integer tipoHabitacionId
) {
}
