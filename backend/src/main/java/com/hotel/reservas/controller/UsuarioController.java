package com.hotel.reservas.controller;

import com.hotel.reservas.dto.UsuarioAdminResponse;
import com.hotel.reservas.dto.UsuarioRequest;
import com.hotel.reservas.security.UsuarioAutenticado;
import com.hotel.reservas.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Gestión de cuentas: administrador del hotel (solo recepcionistas) y administrador general (todos los roles). */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN_GENERAL')")
public class UsuarioController {

    private final UsuarioService service;

    @GetMapping
    public List<UsuarioAdminResponse> listar(@AuthenticationPrincipal UsuarioAutenticado actual) {
        return service.listar(actual.rol());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioAdminResponse crear(@Valid @RequestBody UsuarioRequest request,
                                      @AuthenticationPrincipal UsuarioAutenticado actual) {
        return service.crear(request, actual.rol());
    }

    @PutMapping("/{id}")
    public UsuarioAdminResponse actualizar(@PathVariable Integer id, @Valid @RequestBody UsuarioRequest request,
                                           @AuthenticationPrincipal UsuarioAutenticado actual) {
        return service.actualizar(id, request, actual.id(), actual.rol());
    }
}
