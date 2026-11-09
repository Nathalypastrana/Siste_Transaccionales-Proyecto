package com.hotel.reservas.service;

import java.time.LocalDate;
import java.time.ZoneId;

/** Validar fechas (CU-08): reglas comunes para consultar, crear y modificar reservas. */
public final class ValidadorFechas {

    /** El hotel opera en Bogotá: "hoy" se calcula en esa zona y no en la del servidor. */
    public static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private ValidadorFechas() {
    }

    public static LocalDate hoy() {
        return LocalDate.now(ZONA);
    }

    public static void validar(LocalDate entrada, LocalDate salida) {
        if (entrada == null || salida == null) {
            throw new IllegalArgumentException("Debes indicar la fecha de entrada y la de salida.");
        }
        if (entrada.isBefore(hoy())) {
            throw new IllegalArgumentException("La fecha de entrada no puede ser anterior a hoy.");
        }
        if (!salida.isAfter(entrada)) {
            throw new IllegalArgumentException("La fecha de salida debe ser posterior a la de entrada.");
        }
    }
}
