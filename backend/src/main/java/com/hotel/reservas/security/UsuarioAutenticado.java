package com.hotel.reservas.security;

import com.hotel.reservas.model.Rol;

/** Usuario que hizo la petición, tomado del token JWT (y verificado contra la base de datos). */
public record UsuarioAutenticado(Integer id, String correo, Rol rol) {
}
