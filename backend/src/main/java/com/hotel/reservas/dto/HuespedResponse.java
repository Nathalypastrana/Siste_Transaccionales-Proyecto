package com.hotel.reservas.dto;

import com.hotel.reservas.model.Huesped;

public record HuespedResponse(
        Integer id,
        String tipoDocumento,
        String numeroDocumento,
        String nombres,
        String apellidos,
        String telefono,
        String correo
) {
    public static HuespedResponse de(Huesped h) {
        return new HuespedResponse(h.getId(), h.getTipoDocumento(), h.getNumeroDocumento(),
                h.getNombres(), h.getApellidos(), h.getTelefono(), h.getCorreo());
    }
}
