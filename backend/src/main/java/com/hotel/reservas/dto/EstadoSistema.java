package com.hotel.reservas.dto;

import java.time.LocalDateTime;
import java.util.Map;

/** Resumen de estado del sistema para el administrador general (sin datos personales). */
public record EstadoSistema(
        LocalDateTime fechaHora,
        Map<String, Long> usuariosActivosPorRol,
        long usuariosInactivos,
        long habitaciones,
        String proveedorIa,
        boolean modeloConfigurado,
        boolean simuladorHabilitado,
        boolean whatsappConfigurado,
        String versionAviso,
        long autorizacionesVigentes,
        long autorizacionesRevocadas,
        boolean jwtSecretoDefinido
) {
}
