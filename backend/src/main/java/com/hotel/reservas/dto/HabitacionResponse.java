package com.hotel.reservas.dto;

import com.hotel.reservas.model.EstadoHabitacion;

import java.math.BigDecimal;

public record HabitacionResponse(
        Integer id,
        String numero,
        Integer piso,
        Integer capacidad,
        EstadoHabitacion estado,
        String tipo,
        BigDecimal precioNoche
) {
}
