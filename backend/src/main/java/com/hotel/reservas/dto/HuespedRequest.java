package com.hotel.reservas.dto;

import jakarta.validation.constraints.NotBlank;

public record HuespedRequest(
    @NotBlank String tipoDocumento,
    @NotBlank String numeroDocumento,
    @NotBlank String nombres,
    @NotBlank String apellidos,
    String telefono,
    String correo
) {}