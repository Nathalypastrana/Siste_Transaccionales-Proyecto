package com.hotel.reservas.asistente.intencion;

/** Las cinco necesidades que distingue el clasificador (orden = neuronas de salida y1..y5). */
public enum Intencion {
    DISPONIBILIDAD("consultar disponibilidad de habitaciones"),
    TARIFAS("consultar tarifas"),
    ESTADO_RESERVA("conocer el estado de una reserva"),
    POLITICA_CANCELACION("conocer la política de cancelación"),
    DERIVAR("hablar con recepción");

    private final String descripcion;

    Intencion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }
}
