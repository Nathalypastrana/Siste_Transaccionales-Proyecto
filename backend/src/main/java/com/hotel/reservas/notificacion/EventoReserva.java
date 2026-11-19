package com.hotel.reservas.notificacion;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Hecho ocurrido sobre una reserva que hay que notificar al huésped. Lleva una copia de los datos (no entidades)
 * porque se procesa después de que termina la transacción.
 */
public record EventoReserva(
        Tipo tipo,
        Integer reservaId,
        String nombres,
        String telefono,
        String correo,
        String habitacion,
        LocalDate entrada,
        LocalDate salida,
        BigDecimal total,
        String comprobante
) {
    public enum Tipo { CREADA, MODIFICADA, CANCELADA, RECORDATORIO }
}
