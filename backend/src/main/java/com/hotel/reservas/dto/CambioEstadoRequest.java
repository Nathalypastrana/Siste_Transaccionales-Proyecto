package com.hotel.reservas.dto;

import com.hotel.reservas.model.EstadoHabitacion;
import jakarta.validation.constraints.NotNull;

public record CambioEstadoRequest(@NotNull(message = "Selecciona el estado") EstadoHabitacion estado) {
}
