package com.hotel.reservas.controller;

import com.hotel.reservas.dto.HabitacionResponse;
import com.hotel.reservas.dto.TipoHabitacionResponse;
import com.hotel.reservas.model.EstadoHabitacion;
import com.hotel.reservas.service.HabitacionService;
import com.hotel.reservas.service.TipoHabitacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Información pública del hotel (sin sesión): solo tipos de habitación y su tarifa vigente. Sin datos personales. */
@RestController
@RequestMapping("/api/publico")
@RequiredArgsConstructor
public class PublicoController {

    public record TipoPublico(String nombre, String descripcion, Integer capacidad, BigDecimal precioNoche,
                              long habitaciones) {
    }

    private final HabitacionService habitacionService;
    private final TipoHabitacionService tipoService;

    @GetMapping("/habitaciones")
    public List<TipoPublico> habitaciones() {
        Map<String, List<HabitacionResponse>> porTipo = habitacionService.listar().stream()
                .filter(h -> h.estado() != EstadoHabitacion.MANTENIMIENTO)
                .collect(Collectors.groupingBy(HabitacionResponse::tipo));
        return tipoService.listar().stream()
                .filter(t -> porTipo.containsKey(t.nombre()))
                .map(t -> aPublico(t, porTipo.get(t.nombre())))
                .toList();
    }

    private TipoPublico aPublico(TipoHabitacionResponse t, List<HabitacionResponse> hs) {
        BigDecimal precio = hs.stream().map(HabitacionResponse::precioNoche)
                .filter(p -> p != null).findFirst().orElse(null);
        return new TipoPublico(t.nombre(), t.descripcion(), t.capacidad(), precio, hs.size());
    }
}
