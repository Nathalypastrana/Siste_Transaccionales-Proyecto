package com.hotel.reservas.dto;

import com.hotel.reservas.model.EstadoReserva;

import java.time.LocalDate;

/** Filtros del historial. desde/hasta: reservas cuya estadía se cruza con ese rango de fechas. */
public record FiltroReservas(Integer huespedId, Integer habitacionId, EstadoReserva estado,
                             LocalDate desde, LocalDate hasta, Integer limite) {
}
