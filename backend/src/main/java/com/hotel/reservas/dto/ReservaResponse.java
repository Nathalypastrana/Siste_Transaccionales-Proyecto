package com.hotel.reservas.dto;

import com.hotel.reservas.model.EstadoReserva;
import com.hotel.reservas.model.Reserva;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservaResponse(
        Integer id,
        Integer huespedId,
        String huespedNombre,
        Integer habitacionId,
        String habitacionNumero,
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        EstadoReserva estado,
        BigDecimal valorTotal
) {
    public static ReservaResponse de(Reserva r) {
        return new ReservaResponse(
                r.getId(),
                r.getHuesped().getId(),
                r.getHuesped().getNombres() + " " + r.getHuesped().getApellidos(),
                r.getHabitacion().getId(),
                r.getHabitacion().getNumero(),
                r.getFechaEntrada(),
                r.getFechaSalida(),
                r.getEstado(),
                r.getValorTotal());
    }
}
