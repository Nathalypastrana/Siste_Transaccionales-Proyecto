package com.hotel.reservas.dto;

import com.hotel.reservas.model.Auditoria;

import java.time.LocalDateTime;

public record AuditoriaResponse(Integer id, LocalDateTime fecha, String correo, String rol, String accion,
                                String entidad, String entidadId, String detalle, String ip) {
    public static AuditoriaResponse de(Auditoria a) {
        return new AuditoriaResponse(a.getId(), a.getFecha(), a.getCorreo(), a.getRol(), a.getAccion(),
                a.getEntidad(), a.getEntidadId(), a.getDetalle(), a.getIp());
    }
}
