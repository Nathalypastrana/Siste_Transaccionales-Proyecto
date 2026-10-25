package com.hotel.reservas.controller;

import com.hotel.reservas.dto.HabitacionResponse;
import com.hotel.reservas.service.HabitacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
}
