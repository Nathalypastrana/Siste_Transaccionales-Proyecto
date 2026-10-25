package com.hotel.reservas.service;

import com.hotel.reservas.dto.HabitacionResponse;
import com.hotel.reservas.model.EstadoHabitacion;
import com.hotel.reservas.model.EstadoReserva;
import com.hotel.reservas.model.Habitacion;
import com.hotel.reservas.model.Tarifa;
import com.hotel.reservas.repository.HabitacionRepository;
import com.hotel.reservas.repository.TarifaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HabitacionService {

    private final HabitacionRepository habitacionRepository;
    private final TarifaRepository tarifaRepository;

    @Transactional(readOnly = true)
    public List<HabitacionResponse> listar() {
        LocalDate hoy = LocalDate.now();
        return habitacionRepository.findAllByOrderByNumeroAsc().stream()
                .map(h -> aRespuesta(h, hoy))
                .toList();
    }

    /** Habitaciones libres para el rango de fechas, con la tarifa vigente a la fecha de entrada. */
    @Transactional(readOnly = true)
    public List<HabitacionResponse> disponibles(LocalDate entrada, LocalDate salida) {
        if (!salida.isAfter(entrada)) {
            throw new IllegalArgumentException("La fecha de salida debe ser posterior a la de entrada.");
        }
        return habitacionRepository
                .findDisponibles(entrada, salida,
                        EnumSet.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA),
                        EstadoHabitacion.MANTENIMIENTO)
                .stream()
                .map(h -> aRespuesta(h, entrada))
                .toList();
    }

    private HabitacionResponse aRespuesta(Habitacion h, LocalDate fecha) {
        List<Tarifa> tarifas = tarifaRepository.findVigentes(h.getTipoHabitacion().getId(), fecha);
        return new HabitacionResponse(
                h.getId(),
                h.getNumero(),
                h.getPiso(),
                h.getCapacidad(),
                h.getEstado(),
                h.getTipoHabitacion().getNombre(),
                tarifas.isEmpty() ? null : tarifas.get(0).getValor());
    }
}
