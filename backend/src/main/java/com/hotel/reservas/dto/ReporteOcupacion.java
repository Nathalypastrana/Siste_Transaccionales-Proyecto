package com.hotel.reservas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Reporte de ocupación del periodo [desde, hasta): la fecha de salida no cuenta como noche. */
public record ReporteOcupacion(
        LocalDate desde,
        LocalDate hasta,
        long dias,
        long totalHabitaciones,
        long habitacionesEnMantenimiento,
        long nochesDisponibles,
        long nochesOcupadas,
        BigDecimal porcentajeOcupacion,
        long habitacionesConReservas,
        long habitacionesSinReservas,
        Map<String, Long> reservasPorEstado,
        List<FilaTipo> porTipo
) {
    public record FilaTipo(String tipo, long habitaciones, long nochesDisponibles, long nochesOcupadas,
                           BigDecimal porcentajeOcupacion) {
    }
}
