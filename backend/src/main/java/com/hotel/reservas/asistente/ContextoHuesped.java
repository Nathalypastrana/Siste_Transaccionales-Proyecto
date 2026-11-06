package com.hotel.reservas.asistente;

import com.hotel.reservas.dto.HuespedResponse;

/**
 * Quién escribe. El huésped se identifica por su número de teléfono, nunca por lo que diga el
 * mensaje; así el modelo solo puede consultar los datos de la persona que está escribiendo.
 *
 * @param telefono     número de WhatsApp (solo dígitos)
 * @param nombrePerfil nombre del perfil de WhatsApp, si lo envía la plataforma
 * @param huesped      huésped registrado con ese teléfono, o null si no existe
 */
public record ContextoHuesped(String telefono, String nombrePerfil, HuespedResponse huesped) {

    public String nombreParaMostrar() {
        if (huesped != null) {
            return huesped.nombres() + " " + huesped.apellidos();
        }
        return (nombrePerfil == null || nombrePerfil.isBlank()) ? "huésped no identificado" : nombrePerfil;
    }
}
