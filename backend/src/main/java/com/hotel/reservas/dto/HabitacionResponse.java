package com.hotel.reservas.dto;

import com.hotel.reservas.enums.EstadoHabitacion;

public record HabitacionResponse(
    Integer idHabitacion,
    String numero,
    Integer piso,
    EstadoHabitacion estado,
    String tipo
) {}
