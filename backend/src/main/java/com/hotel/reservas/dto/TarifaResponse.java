package com.hotel.reservas.dto;

import com.hotel.reservas.model.Tarifa;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TarifaResponse(Integer id, Integer tipoHabitacionId, String tipo, BigDecimal valor,
                             LocalDate fechaInicio, LocalDate fechaFin, Boolean activa) {
    public static TarifaResponse de(Tarifa t) {
        return new TarifaResponse(t.getId(), t.getTipoHabitacion().getId(), t.getTipoHabitacion().getNombre(),
                t.getValor(), t.getFechaInicio(), t.getFechaFin(), t.getActiva());
    }
}
