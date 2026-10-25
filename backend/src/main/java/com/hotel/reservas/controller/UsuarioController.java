package com.hotel.reservas.controller;

import com.hotel.reservas.dto.UsuarioResponse;
import com.hotel.reservas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Lista de usuarios activos para elegir quién registra la reserva (hasta que exista login). */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findByActivoTrueOrderByNombreAsc().stream()
                .map(UsuarioResponse::de)
                .toList();
    }
}
