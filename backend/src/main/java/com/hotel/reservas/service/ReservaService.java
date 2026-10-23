package com.hotel.reservas.service;

import com.hotel.reservas.dto.ReservaRequest;
import com.hotel.reservas.dto.ReservaResponse;
import com.hotel.reservas.exception.ConflictoReservaException;
import com.hotel.reservas.exception.RecursoNoEncontradoException;
import com.hotel.reservas.model.*;
import com.hotel.reservas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final HabitacionRepository habitacionRepository;
    private final HuespedRepository huespedRepository;
    private final TarifaRepository tarifaRepository;
    private final ComprobanteReservaRepository comprobanteRepository;

    /**
     * Crea una reserva en una sola transacción (atomicidad):
     * 1. bloquea la habitación (aislamiento),
     * 2. verifica que no haya cruces de fechas (consistencia),
     * 3. guarda la reserva y su comprobante (todo o nada).
     */
    @Transactional
    public ReservaResponse crear(ReservaRequest req) {
        if (!req.fechaSalida().isAfter(req.fechaEntrada())) {
            throw new IllegalArgumentException("La fecha de salida debe ser posterior a la de entrada.");
        }

        Habitacion habitacion = habitacionRepository.findByIdForUpdate(req.habitacionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Habitación no encontrada."));

        if (habitacion.getEstado() != EstadoHabitacion.DISPONIBLE) {
            throw new ConflictoReservaException("La habitación no está disponible para reservar.");
        }

        Huesped huesped = huespedRepository.findById(req.huespedId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Huésped no encontrado."));

        long cruces = reservaRepository.contarCruces(
                habitacion.getId(), req.fechaEntrada(), req.fechaSalida(), EstadoReserva.CONFIRMADA);
        if (cruces > 0) {
            throw new ConflictoReservaException("La habitación ya está reservada en esas fechas.");
        }

        Tarifa tarifa = tarifaRepository
                .findVigente(habitacion.getTipoHabitacion().getId(), req.fechaEntrada())
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay tarifa vigente para esa fecha."));

        long noches = ChronoUnit.DAYS.between(req.fechaEntrada(), req.fechaSalida());
        BigDecimal total = tarifa.getPrecioNoche().multiply(BigDecimal.valueOf(noches));

        Reserva reserva = new Reserva();
        reserva.setHuesped(huesped);
        reserva.setHabitacion(habitacion);
        reserva.setFechaEntrada(req.fechaEntrada());
        reserva.setFechaSalida(req.fechaSalida());
        reserva.setTotal(total);
        reserva = reservaRepository.save(reserva);

        ComprobanteReserva comprobante = new ComprobanteReserva();
        comprobante.setReserva(reserva);
        comprobante.setNumero("CR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        comprobante.setTotal(total);
        comprobanteRepository.save(comprobante);

        return ReservaResponse.de(reserva);
    }

    @Transactional
    public ReservaResponse cancelar(Long id) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada."));
        if (reserva.getEstado() != EstadoReserva.CONFIRMADA) {
            throw new ConflictoReservaException("Solo se pueden cancelar reservas confirmadas.");
        }
        if (!reserva.getFechaEntrada().isAfter(LocalDate.now())) {
            throw new ConflictoReservaException("No se puede cancelar una reserva que ya inició.");
        }
        reserva.setEstado(EstadoReserva.CANCELADA);
        return ReservaResponse.de(reserva);
    }

    @Transactional(readOnly = true)
    public ReservaResponse obtener(Long id) {
        return reservaRepository.findById(id)
                .map(ReservaResponse::de)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada."));
    }
}
