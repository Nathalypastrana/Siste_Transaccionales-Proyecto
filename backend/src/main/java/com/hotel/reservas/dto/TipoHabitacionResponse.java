package com.hotel.reservas.dto;

import com.hotel.reservas.model.TipoHabitacion;

public record TipoHabitacionResponse(Integer id, String nombre, String descripcion, Integer capacidad) {
    public static TipoHabitacionResponse de(TipoHabitacion t) {
        return new TipoHabitacionResponse(t.getId(), t.getNombre(), t.getDescripcion(), t.getCapacidad());
    }
}
