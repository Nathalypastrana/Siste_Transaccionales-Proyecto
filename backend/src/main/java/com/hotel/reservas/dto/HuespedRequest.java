package com.hotel.reservas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record HuespedRequest(
        @NotBlank(message = "El tipo de documento es obligatorio")
        @Size(max = 20, message = "El tipo de documento admite máximo 20 caracteres")
        String tipoDocumento,

        @NotBlank(message = "El número de documento es obligatorio")
        @Size(max = 30, message = "El número de documento admite máximo 30 caracteres")
        String numeroDocumento,

        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 100, message = "Los nombres admiten máximo 100 caracteres")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 100, message = "Los apellidos admiten máximo 100 caracteres")
        String apellidos,

        @Pattern(regexp = "^$|^[0-9+ ()-]{7,20}$", message = "El teléfono no es válido")
        String telefono,

        @Email(message = "El correo no es válido")
        @Size(max = 100, message = "El correo admite máximo 100 caracteres")
        String correo
) {
}
