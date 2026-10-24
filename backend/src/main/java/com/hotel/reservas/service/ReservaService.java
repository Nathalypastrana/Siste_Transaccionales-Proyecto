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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private static final Set<EstadoReserva> ESTADOS_ACTIVOS =
            EnumSet.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);

    private final ReservaRepository reservaRepository;
    private final HabitacionRepository habitacionRepository;
    private final HuespedRepository huespedRepository;
    private final UsuarioRepository usuarioRepository;
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

        if (habitacion.getEstado() == EstadoHabitacion.MANTENIMIENTO) {
            throw new ConflictoReservaException("La habitación está en mantenimiento.");
        }

        Huesped huesped = huespedRepository.findById(req.huespedId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Huésped no encontrado."));
        Usuario usuario = usuarioRepository.findById(req.usuarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));

        long cruces = reservaRepository.contarCruces(
                habitacion.getId(), req.fechaEntrada(), req.fechaSalida(), ESTADOS_ACTIVOS);
        if (cruces > 0) {
            throw new ConflictoReservaException("La habitación ya está reservada en esas fechas.");
        }

        List<Tarifa> tarifas = tarifaRepository
                .findVigentes(habitacion.getTipoHabitacion().getId(), req.fechaEntrada());
        if (tarifas.isEmpty()) {
            throw new RecursoNoEncontradoException("No hay tarifa vigente para esa fecha.");
        }
        Tarifa tarifa = tarifas.get(0);

        long noches = ChronoUnit.DAYS.between(req.fechaEntrada(), req.fechaSalida());
        BigDecimal total = tarifa.getValor().multiply(BigDecimal.valueOf(noches));

        Reserva reserva = new Reserva();
        reserva.setHuesped(huesped);
        reserva.setHabitacion(habitacion);
        reserva.setUsuario(usuario);
        reserva.setTarifa(tarifa);
        reserva.setFechaEntrada(req.fechaEntrada());
        reserva.setFechaSalida(req.fechaSalida());
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reserva.setValorTotal(total);
        reserva = reservaRepository.save(reserva);

        ComprobanteReserva comprobante = new ComprobanteReserva();
        comprobante.setReserva(reserva);
        comprobante.setNumeroComprobante("CR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        comprobanteRepository.save(comprobante);

        return ReservaResponse.de(reserva);
    }

    @Transactional
    public ReservaResponse cancelar(Integer id) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada."));
        if (!ESTADOS_ACTIVOS.contains(reserva.getEstado())) {
            throw new ConflictoReservaException("Solo se pueden cancelar reservas pendientes o confirmadas.");
        }
        if (!reserva.getFechaEntrada().isAfter(LocalDate.now())) {
            throw new ConflictoReservaException("No se puede cancelar una reserva que ya inició.");
        }
        reserva.setEstado(EstadoReserva.CANCELADA);
        return ReservaResponse.de(reserva);
    }

    @Transactional(readOnly = true)
    public ReservaResponse obtener(Integer id) {
        return reservaRepository.findById(id)
                .map(ReservaResponse::de)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada."));
    }
}
