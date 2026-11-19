package com.hotel.reservas.service;

import com.hotel.reservas.dto.AlertaTarifa;
import com.hotel.reservas.model.EstadoHabitacion;
import com.hotel.reservas.model.Habitacion;
import com.hotel.reservas.model.Tarifa;
import com.hotel.reservas.model.TipoHabitacion;
import com.hotel.reservas.repository.HabitacionRepository;
import com.hotel.reservas.repository.TarifaRepository;
import com.hotel.reservas.repository.TipoHabitacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Detecta tipos de habitación que se quedarán sin tarifa vigente: sin tarifa no se puede reservar, y es un fallo
 * silencioso. Revisa los próximos N días (por defecto 15) solo para tipos que tienen habitaciones en servicio.
 */
@Service
@RequiredArgsConstructor
public class AlertaTarifaService {

    private final TipoHabitacionRepository tipoRepository;
    private final HabitacionRepository habitacionRepository;
    private final TarifaRepository tarifaRepository;

    @Value("${notificaciones.dias-alerta-tarifa:15}")
    private int diasPorDefecto;

    @Transactional(readOnly = true)
    public List<AlertaTarifa> calcular() {
        return calcular(diasPorDefecto);
    }

    @Transactional(readOnly = true)
    public List<AlertaTarifa> calcular(int dias) {
        LocalDate hoy = ValidadorFechas.hoy();
        Set<Integer> tiposEnServicio = habitacionRepository.findAll().stream()
                .filter(h -> h.getEstado() != EstadoHabitacion.MANTENIMIENTO)
                .map(h -> h.getTipoHabitacion().getId())
                .collect(Collectors.toSet());
        List<Tarifa> activas = tarifaRepository.findAll().stream()
                .filter(t -> Boolean.TRUE.equals(t.getActiva())).toList();

        List<AlertaTarifa> alertas = new ArrayList<>();
        for (TipoHabitacion tipo : tipoRepository.findAllByOrderByNombreAsc()) {
            if (!tiposEnServicio.contains(tipo.getId())) {
                continue;
            }
            for (int i = 0; i < dias; i++) {
                LocalDate dia = hoy.plusDays(i);
                boolean cubierto = activas.stream().anyMatch(t -> t.getTipoHabitacion().getId().equals(tipo.getId())
                        && !dia.isBefore(t.getFechaInicio()) && !dia.isAfter(t.getFechaFin()));
                if (!cubierto) {
                    alertas.add(new AlertaTarifa(tipo.getNombre(), dia, ChronoUnit.DAYS.between(hoy, dia), i == 0));
                    break;
                }
            }
        }
        return alertas;
    }
}
