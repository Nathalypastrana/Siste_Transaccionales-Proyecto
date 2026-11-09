package com.hotel.reservas.dto;

public record LoginResponse(String token, long expiraEnSegundos, UsuarioResponse usuario) {
}
