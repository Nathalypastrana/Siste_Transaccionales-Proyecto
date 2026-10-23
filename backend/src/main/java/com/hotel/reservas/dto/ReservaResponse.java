package com.hotel.reservas.dto;

import com.hotel.reservas.model.EstadoReserva;
import com.hotel.reservas.model.Reserva;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservaResponse(
        Long id,
        Long huespedId,
        Long habitacionId,
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        EstadoReserva estado,
        BigDecimal total
) {
    public static ReservaResponse de(Reserva r) {
        return new ReservaResponse(
                r.getId(),
                r.getHuesped().getId(),
                r.getHabitacion().getId(),
                r.getFechaEntrada(),
                r.getFechaSalida(),
                r.getEstado(),
                r.getTotal());
    }
}
