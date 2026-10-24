package com.hotel.reservas.controller;

import com.hotel.reservas.dto.ReservaRequest;
import com.hotel.reservas.dto.ReservaResponse;
import com.hotel.reservas.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse crear(@Valid @RequestBody ReservaRequest request) {
        return reservaService.crear(request);
    }

    @GetMapping("/{id}")
    public ReservaResponse obtener(@PathVariable Integer id) {
        return reservaService.obtener(id);
    }

    @PatchMapping("/{id}/cancelar")
    public ReservaResponse cancelar(@PathVariable Integer id) {
        return reservaService.cancelar(id);
    }
}
