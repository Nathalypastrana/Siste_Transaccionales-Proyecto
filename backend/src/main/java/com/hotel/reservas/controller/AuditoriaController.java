package com.hotel.reservas.controller;

import com.hotel.reservas.dto.AuditoriaResponse;
import com.hotel.reservas.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Consulta del registro de auditoría: solo el administrador general. */
@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN_GENERAL')")
public class AuditoriaController {

    private final AuditoriaService service;

    @GetMapping
    public List<AuditoriaResponse> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Integer limite) {
        return service.listar(desde, hasta, accion, texto, limite);
    }
}
