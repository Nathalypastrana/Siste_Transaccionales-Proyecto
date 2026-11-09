package com.hotel.reservas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestablecerRequest(
        @NotBlank(message = "El enlace no es válido") @Size(max = 200) String token,
        @NotBlank(message = "Escribe la nueva contraseña") @Size(max = 100) String password
) {
}
