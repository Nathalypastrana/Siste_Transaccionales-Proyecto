package com.hotel.reservas.dto;

import com.hotel.reservas.model.Rol;
import com.hotel.reservas.model.Usuario;

/** Nunca incluye la contraseña. */
public record UsuarioAdminResponse(Integer id, String nombre, String correo, Rol rol, Boolean activo) {
    public static UsuarioAdminResponse de(Usuario u) {
        return new UsuarioAdminResponse(u.getId(), u.getNombre(), u.getCorreo(), u.getRol(), u.getActivo());
    }
}
