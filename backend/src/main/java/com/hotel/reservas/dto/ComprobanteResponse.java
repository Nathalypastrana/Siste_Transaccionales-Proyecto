package com.hotel.reservas.dto;

import com.hotel.reservas.model.ComprobanteReserva;
import com.hotel.reservas.model.EstadoReserva;
import com.hotel.reservas.model.Reserva;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record ComprobanteResponse(
        String numeroComprobante,
        LocalDateTime fechaGeneracion,
        Integer reservaId,
        EstadoReserva estadoReserva,
        String huesped,
        String documento,
        String habitacion,
        String tipoHabitacion,
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        long noches,
        BigDecimal tarifaNoche,
        BigDecimal valorTotal
) {
    public static ComprobanteResponse de(ComprobanteReserva c) {
        Reserva r = c.getReserva();
        return new ComprobanteResponse(
                c.getNumeroComprobante(),
                c.getFechaGeneracion(),
                r.getId(),
                r.getEstado(),
                r.getHuesped().getNombres() + " " + r.getHuesped().getApellidos(),
                r.getHuesped().getTipoDocumento() + " " + r.getHuesped().getNumeroDocumento(),
                r.getHabitacion().getNumero(),
                r.getHabitacion().getTipoHabitacion().getNombre(),
                r.getFechaEntrada(),
                r.getFechaSalida(),
                ChronoUnit.DAYS.between(r.getFechaEntrada(), r.getFechaSalida()),
                r.getTarifa().getValor(),
                r.getValorTotal());
    }
}
