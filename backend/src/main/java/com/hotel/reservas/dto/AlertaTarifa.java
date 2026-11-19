package com.hotel.reservas.dto;

import java.time.LocalDate;

/** Un tipo de habitación al que le falta tarifa vigente dentro de la ventana revisada. */
public record AlertaTarifa(String tipo, LocalDate primeraFechaSinTarifa, long diasRestantes, boolean sinTarifaHoy) {
}
