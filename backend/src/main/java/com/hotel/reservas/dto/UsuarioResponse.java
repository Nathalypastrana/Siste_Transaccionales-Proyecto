package com.hotel.reservas.dto;

import com.hotel.reservas.model.Rol;
import com.hotel.reservas.model.Usuario;

/** Nunca expone la contraseña. */
public record UsuarioResponse(Integer id, String nombre, Rol rol) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getRol());
    }
}
