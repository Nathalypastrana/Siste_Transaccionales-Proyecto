package com.hotel.reservas.controller;

import com.hotel.reservas.dto.TipoHabitacionRequest;
import com.hotel.reservas.dto.TipoHabitacionResponse;
import com.hotel.reservas.service.TipoHabitacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-habitacion")
@RequiredArgsConstructor
public class TipoHabitacionController {

    private final TipoHabitacionService service;

    @GetMapping
    public List<TipoHabitacionResponse> listar() {
        return service.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN_GENERAL')")
    public TipoHabitacionResponse crear(@Valid @RequestBody TipoHabitacionRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN_GENERAL')")
    public TipoHabitacionResponse actualizar(@PathVariable Integer id, @Valid @RequestBody TipoHabitacionRequest request) {
        return service.actualizar(id, request);
    }
}
