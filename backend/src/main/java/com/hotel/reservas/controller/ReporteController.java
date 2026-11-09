package com.hotel.reservas.controller;

import com.hotel.reservas.dto.ReporteOcupacion;
import com.hotel.reservas.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN_GENERAL')")
public class ReporteController {

    private final ReporteService service;

    /** Ocupación del periodo [desde, hasta). tipoId es opcional. */
    @GetMapping("/ocupacion")
    public ReporteOcupacion ocupacion(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer tipoId) {
        return service.ocupacion(desde, hasta, tipoId);
    }
}
