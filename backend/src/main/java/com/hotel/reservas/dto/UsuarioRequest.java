package com.hotel.reservas.dto;

import com.hotel.reservas.model.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** En la creación la contraseña es obligatoria; al editar, si viene vacía se conserva la actual. */
public record UsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 100, message = "El nombre admite hasta 100 caracteres") String nombre,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no es válido") @Size(max = 100, message = "El correo admite hasta 100 caracteres") String correo,
        String password,
        @NotNull(message = "Selecciona el rol") Rol rol,
        Boolean activo
) {
}
