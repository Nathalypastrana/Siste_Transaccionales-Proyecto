package com.hotel.reservas.service;

import com.hotel.reservas.dto.ReporteOcupacion;
import com.hotel.reservas.dto.ReporteOcupacion.FilaTipo;
import com.hotel.reservas.model.EstadoHabitacion;
import com.hotel.reservas.model.EstadoReserva;
import com.hotel.reservas.model.Habitacion;
import com.hotel.reservas.model.Reserva;
import com.hotel.reservas.repository.HabitacionRepository;
import com.hotel.reservas.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Reportes de ocupación (RF 13 / CU-18). Solo lectura. */
@Service
@RequiredArgsConstructor
public class ReporteService {

    private static final long MAX_DIAS = 366;

    private final ReservaRepository reservaRepository;
    private final HabitacionRepository habitacionRepository;

    @Transactional(readOnly = true)
    public ReporteOcupacion ocupacion(LocalDate desde, LocalDate hasta, Integer tipoId) {
        if (desde == null || hasta == null || !hasta.isAfter(desde)) {
            throw new IllegalArgumentException("El periodo es inválido: la fecha final debe ser posterior a la inicial.");
        }
        long dias = ChronoUnit.DAYS.between(desde, hasta);
        if (dias > MAX_DIAS) {
            throw new IllegalArgumentException("El periodo máximo del reporte es de " + MAX_DIAS + " días.");
        }

        List<Habitacion> todas = habitacionRepository.findAll().stream()
                .filter(h -> tipoId == null || h.getTipoHabitacion().getId().equals(tipoId))
                .toList();
        List<Habitacion> operativas = todas.stream()
                .filter(h -> h.getEstado() != EstadoHabitacion.MANTENIMIENTO)
                .toList();
        long enMantenimiento = todas.size() - operativas.size();
        Set<Integer> idsOperativas = new HashSet<>();
        operativas.forEach(h -> idsOperativas.add(h.getId()));

        List<Reserva> reservas = reservaRepository.listarEnPeriodo(desde, hasta).stream()
                .filter(r -> tipoId == null || r.getHabitacion().getTipoHabitacion().getId().equals(tipoId))
                .toList();

        Map<EstadoReserva, Long> porEstado = new EnumMap<>(EstadoReserva.class);
        for (EstadoReserva e : EstadoReserva.values()) {
            porEstado.put(e, 0L);
        }
        Map<String, long[]> porTipo = new TreeMap<>();   // tipo -> {habitaciones, nochesOcupadas}
        for (Habitacion h : operativas) {
            porTipo.computeIfAbsent(h.getTipoHabitacion().getNombre(), k -> new long[2])[0]++;
        }
        Set<Integer> habitacionesConReservas = new HashSet<>();
        long nochesOcupadas = 0;

        for (Reserva r : reservas) {
            porEstado.merge(r.getEstado(), 1L, Long::sum);
            if (r.getEstado() == EstadoReserva.CANCELADA) {
                continue;   // una reserva cancelada no ocupa la habitación
            }
            LocalDate ini = r.getFechaEntrada().isAfter(desde) ? r.getFechaEntrada() : desde;
            LocalDate fin = r.getFechaSalida().isBefore(hasta) ? r.getFechaSalida() : hasta;
            long noches = Math.max(0, ChronoUnit.DAYS.between(ini, fin));
            if (!idsOperativas.contains(r.getHabitacion().getId())) {
                continue;
            }
            nochesOcupadas += noches;
            habitacionesConReservas.add(r.getHabitacion().getId());
            porTipo.computeIfAbsent(r.getHabitacion().getTipoHabitacion().getNombre(), k -> new long[2])[1] += noches;
        }

        long nochesDisponibles = operativas.size() * dias;
        List<FilaTipo> filas = porTipo.entrySet().stream()
                .map(e -> new FilaTipo(e.getKey(), e.getValue()[0], e.getValue()[0] * dias, e.getValue()[1],
                        porcentaje(e.getValue()[1], e.getValue()[0] * dias)))
                .toList();

        Map<String, Long> estados = new LinkedHashMap<>();
        porEstado.forEach((k, v) -> estados.put(k.name(), v));
        return new ReporteOcupacion(desde, hasta, dias, operativas.size(), enMantenimiento, nochesDisponibles,
                nochesOcupadas, porcentaje(nochesOcupadas, nochesDisponibles),
                habitacionesConReservas.size(), operativas.size() - habitacionesConReservas.size(),
                estados, filas);
    }

    private static BigDecimal porcentaje(long parte, long total) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return BigDecimal.valueOf(parte * 100.0 / total).setScale(1, RoundingMode.HALF_UP);
    }
}
