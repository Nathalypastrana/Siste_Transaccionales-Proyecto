package com.hotel.reservas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudRecuperacion(
        @NotBlank(message = "Escribe tu correo") @Email(message = "El correo no es válido") @Size(max = 100) String correo
) {
}
