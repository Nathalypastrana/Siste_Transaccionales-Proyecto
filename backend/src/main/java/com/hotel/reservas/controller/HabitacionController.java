package com.hotel.reservas.controller;

import com.hotel.reservas.dto.CambioEstadoRequest;
import com.hotel.reservas.dto.HabitacionRequest;
import com.hotel.reservas.dto.HabitacionResponse;
import com.hotel.reservas.service.HabitacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/habitaciones")
@RequiredArgsConstructor
public class HabitacionController {

    private final HabitacionService habitacionService;

    @GetMapping
    public List<HabitacionResponse> listar() {
        return habitacionService.listar();
    }

    @GetMapping("/disponibles")
    public List<HabitacionResponse> disponibles(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entrada,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salida) {
        return habitacionService.disponibles(entrada, salida);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN_GENERAL')")
    public HabitacionResponse crear(@Valid @RequestBody HabitacionRequest request) {
        return habitacionService.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN_GENERAL')")
    public HabitacionResponse actualizar(@PathVariable Integer id, @Valid @RequestBody HabitacionRequest request) {
        return habitacionService.actualizar(id, request);
    }

    /** Cambiar solo el estado (p. ej. mantenimiento): administrador del hotel y general. */
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN_GENERAL')")
    public HabitacionResponse cambiarEstado(@PathVariable Integer id, @Valid @RequestBody CambioEstadoRequest request) {
        return habitacionService.cambiarEstado(id, request.estado());
    }
}
