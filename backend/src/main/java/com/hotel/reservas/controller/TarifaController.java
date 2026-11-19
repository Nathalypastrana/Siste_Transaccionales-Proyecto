package com.hotel.reservas.controller;

import com.hotel.reservas.dto.TarifaRequest;
import com.hotel.reservas.dto.TarifaResponse;
import com.hotel.reservas.dto.AlertaTarifa;
import com.hotel.reservas.service.AlertaTarifaService;
import com.hotel.reservas.service.TarifaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tarifas")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN_GENERAL')")
public class TarifaController {

    private final TarifaService service;
    private final AlertaTarifaService alertas;

    /** Tipos de habitación que se quedarán sin tarifa vigente en los próximos días. */
    @GetMapping("/alertas")
    public List<AlertaTarifa> alertas() {
        return alertas.calcular();
    }

    @GetMapping
    public List<TarifaResponse> listar() {
        return service.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TarifaResponse crear(@Valid @RequestBody TarifaRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    public TarifaResponse actualizar(@PathVariable Integer id, @Valid @RequestBody TarifaRequest request) {
        return service.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    public TarifaResponse cambiarEstado(@PathVariable Integer id, @RequestParam boolean activa) {
        return service.cambiarEstado(id, activa);
    }
}
