package com.hotel.reservas.asistente.modelo;

/** Un mensaje de la conversación. rol: "user" (huésped) o "assistant" (consultor). */
public record Turno(String rol, String texto) {
}
