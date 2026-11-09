package com.hotel.reservas.service;

import com.hotel.reservas.dto.ComprobanteResponse;
import com.hotel.reservas.dto.FiltroReservas;
import com.hotel.reservas.dto.ModificarReservaRequest;
import com.hotel.reservas.dto.ReservaRequest;
import com.hotel.reservas.dto.ReservaResponse;
import com.hotel.reservas.exception.ConflictoReservaException;
import com.hotel.reservas.exception.RecursoNoEncontradoException;
import com.hotel.reservas.model.ComprobanteReserva;
import com.hotel.reservas.model.EstadoHabitacion;
import com.hotel.reservas.model.EstadoReserva;
import com.hotel.reservas.model.Habitacion;
import com.hotel.reservas.model.Huesped;
import com.hotel.reservas.model.Reserva;
import com.hotel.reservas.model.Tarifa;
import com.hotel.reservas.model.Usuario;
import com.hotel.reservas.repository.ComprobanteReservaRepository;
import com.hotel.reservas.repository.HabitacionRepository;
import com.hotel.reservas.repository.HuespedRepository;
import com.hotel.reservas.repository.ReservaRepository;
import com.hotel.reservas.repository.TarifaRepository;
import com.hotel.reservas.repository.UsuarioRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservaService {

    private static final Set<EstadoReserva> ESTADOS_ACTIVOS = ActualizadorEstadoHabitacion.ESTADOS_ACTIVOS;
    private static final int LIMITE_POR_DEFECTO = 200;
    private static final int LIMITE_MAXIMO = 1000;

    private final ReservaRepository reservaRepository;
    private final HabitacionRepository habitacionRepository;
    private final HuespedRepository huespedRepository;
    private final UsuarioRepository usuarioRepository;
    private final TarifaRepository tarifaRepository;
    private final ComprobanteReservaRepository comprobanteRepository;
    private final ActualizadorEstadoHabitacion estadoHabitaciones;
    private final AuditoriaService auditoria;

    /**
     * Crear reserva (RF 04 / CU-10) en una sola transacción (atomicidad):
     * 1. valida fechas, 2. bloquea la habitación (aislamiento), 3. verifica que no haya cruces (consistencia),
     * 4. aplica la tarifa vigente, 5. guarda reserva y comprobante, 6. recalcula el estado de la habitación.
     * Cualquier error revierte todo (ROLLBACK).
     */
    @Transactional
    public ReservaResponse crear(ReservaRequest req, Integer usuarioId) {
        ValidadorFechas.validar(req.fechaEntrada(), req.fechaSalida());

        Habitacion habitacion = habitacionRepository.findByIdForUpdate(req.habitacionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Habitación no encontrada."));
        if (habitacion.getEstado() == EstadoHabitacion.MANTENIMIENTO) {
            throw new ConflictoReservaException("La habitación está en mantenimiento.");
        }
        Huesped huesped = huespedRepository.findById(req.huespedId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Huésped no encontrado."));
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));

        long cruces = reservaRepository.contarCruces(
                habitacion.getId(), req.fechaEntrada(), req.fechaSalida(), ESTADOS_ACTIVOS);
        if (cruces > 0) {
            throw new ConflictoReservaException("La habitación ya está reservada en esas fechas.");
        }

        Tarifa tarifa = tarifaVigente(habitacion, req.fechaEntrada());
        long noches = ChronoUnit.DAYS.between(req.fechaEntrada(), req.fechaSalida());

        Reserva reserva = new Reserva();
        reserva.setHuesped(huesped);
        reserva.setHabitacion(habitacion);
        reserva.setUsuario(usuario);
        reserva.setTarifa(tarifa);
        reserva.setFechaEntrada(req.fechaEntrada());
        reserva.setFechaSalida(req.fechaSalida());
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reserva.setValorTotal(tarifa.getValor().multiply(BigDecimal.valueOf(noches)));
        reserva = reservaRepository.save(reserva);

        ComprobanteReserva comprobante = crearComprobante(reserva);
        estadoHabitaciones.recalcular(habitacion);
        auditoria.registrar("RESERVA_CREADA", "Reserva", reserva.getId(), "Hab " + habitacion.getNumero() + ", "
                + reserva.getFechaEntrada() + " a " + reserva.getFechaSalida() + ", total " + reserva.getValorTotal()
                + ", comprobante " + comprobante.getNumeroComprobante());
        return ReservaResponse.de(reserva);
    }

    /**
     * Modificar reserva (RF 05 / CU-11): cambia fechas y/o habitación. Bloquea la reserva y las habitaciones
     * involucradas (siempre en orden de id, para no generar interbloqueos), verifica disponibilidad
     * excluyendo la propia reserva y recalcula el valor. Si algo falla, la reserva original no se toca.
     */
    @Transactional
    public ReservaResponse modificar(Integer id, ModificarReservaRequest req) {
        Reserva reserva = reservaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada."));
        if (!ESTADOS_ACTIVOS.contains(reserva.getEstado())) {
            throw new ConflictoReservaException("Solo se pueden modificar reservas pendientes o confirmadas.");
        }
        if (!reserva.getFechaEntrada().isAfter(ValidadorFechas.hoy())) {
            throw new ConflictoReservaException("No se puede modificar una reserva que ya inició.");
        }

        Integer habitacionActualId = reserva.getHabitacion().getId();
        Integer habitacionNuevaId = req.habitacionId() != null ? req.habitacionId() : habitacionActualId;
        LocalDate entrada = req.fechaEntrada() != null ? req.fechaEntrada() : reserva.getFechaEntrada();
        LocalDate salida = req.fechaSalida() != null ? req.fechaSalida() : reserva.getFechaSalida();

        boolean cambiaHabitacion = !habitacionNuevaId.equals(habitacionActualId);
        boolean cambiaFechas = !entrada.equals(reserva.getFechaEntrada()) || !salida.equals(reserva.getFechaSalida());
        if (!cambiaHabitacion && !cambiaFechas) {
            throw new IllegalArgumentException("No hay cambios que guardar.");
        }
        ValidadorFechas.validar(entrada, salida);

        // Bloqueo de las habitaciones en orden de id
        Map<Integer, Habitacion> bloqueadas = new java.util.HashMap<>();
        for (Integer hid : new TreeSet<>(List.of(habitacionActualId, habitacionNuevaId))) {
            bloqueadas.put(hid, habitacionRepository.findByIdForUpdate(hid)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Habitación no encontrada.")));
        }
        Habitacion actual = bloqueadas.get(habitacionActualId);
        Habitacion nueva = bloqueadas.get(habitacionNuevaId);
        if (cambiaHabitacion && nueva.getEstado() == EstadoHabitacion.MANTENIMIENTO) {
            throw new ConflictoReservaException("La habitación seleccionada está en mantenimiento.");
        }

        long cruces = reservaRepository.contarCrucesExcluyendo(
                nueva.getId(), entrada, salida, ESTADOS_ACTIVOS, reserva.getId());
        if (cruces > 0) {
            throw new ConflictoReservaException("La habitación no está disponible en las nuevas fechas.");
        }

        boolean cambiaTipo = !nueva.getTipoHabitacion().getId().equals(actual.getTipoHabitacion().getId());
        Tarifa tarifa = reserva.getTarifa();
        if (cambiaTipo || !entrada.equals(reserva.getFechaEntrada())) {
            tarifa = tarifaVigente(nueva, entrada);
        }
        long noches = ChronoUnit.DAYS.between(entrada, salida);

        String antes = "Hab " + actual.getNumero() + ", " + reserva.getFechaEntrada() + " a " + reserva.getFechaSalida()
                + ", total " + reserva.getValorTotal();
        reserva.setHabitacion(nueva);
        reserva.setFechaEntrada(entrada);
        reserva.setFechaSalida(salida);
        reserva.setTarifa(tarifa);
        reserva.setValorTotal(tarifa.getValor().multiply(BigDecimal.valueOf(noches)));
        reservaRepository.saveAndFlush(reserva);

        estadoHabitaciones.recalcular(actual);
        if (cambiaHabitacion) {
            estadoHabitaciones.recalcular(nueva);
        }
        auditoria.registrar("RESERVA_MODIFICADA", "Reserva", reserva.getId(), "Antes: " + antes + ". Ahora: Hab "
                + nueva.getNumero() + ", " + entrada + " a " + salida + ", total " + reserva.getValorTotal());
        return ReservaResponse.de(reserva);
    }

    /** Cancelar reserva (RF 06 / CU-12): la reserva se conserva con estado CANCELADA y la habitación se libera. */
    @Transactional
    public ReservaResponse cancelar(Integer id) {
        Reserva reserva = reservaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada."));
        if (!ESTADOS_ACTIVOS.contains(reserva.getEstado())) {
            throw new ConflictoReservaException("Solo se pueden cancelar reservas pendientes o confirmadas.");
        }
        if (!reserva.getFechaEntrada().isAfter(ValidadorFechas.hoy())) {
            throw new ConflictoReservaException("No se puede cancelar una reserva que ya inició.");
        }
        reserva.setEstado(EstadoReserva.CANCELADA);
        reservaRepository.flush();
        estadoHabitaciones.recalcular(reserva.getHabitacion());
        auditoria.registrar("RESERVA_CANCELADA", "Reserva", reserva.getId(), "Hab "
                + reserva.getHabitacion().getNumero() + ", " + reserva.getFechaEntrada() + " a " + reserva.getFechaSalida());
        return ReservaResponse.de(reserva);
    }

    /** Historial (RF 12 / CU-17) con filtros opcionales; las más recientes primero. */
    @Transactional(readOnly = true)
    public List<ReservaResponse> listar(FiltroReservas f) {
        int limite = f.limite() == null ? LIMITE_POR_DEFECTO : Math.max(1, Math.min(f.limite(), LIMITE_MAXIMO));
        Specification<Reserva> spec = (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("huesped");
                root.fetch("habitacion");
            }
            List<Predicate> p = new ArrayList<>();
            if (f.huespedId() != null) {
                p.add(cb.equal(root.get("huesped").get("id"), f.huespedId()));
            }
            if (f.habitacionId() != null) {
                p.add(cb.equal(root.get("habitacion").get("id"), f.habitacionId()));
            }
            if (f.estado() != null) {
                p.add(cb.equal(root.get("estado"), f.estado()));
            }
            if (f.desde() != null) {
                p.add(cb.greaterThan(root.<LocalDate>get("fechaSalida"), f.desde()));
            }
            if (f.hasta() != null) {
                p.add(cb.lessThanOrEqualTo(root.<LocalDate>get("fechaEntrada"), f.hasta()));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        return reservaRepository.findAll(spec, PageRequest.of(0, limite, Sort.by(Sort.Direction.DESC, "id")))
                .getContent().stream()
                .map(ReservaResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> listarPorHuesped(Integer huespedId) {
        return reservaRepository.listarPorHuesped(huespedId).stream()
                .map(ReservaResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReservaResponse obtener(Integer id) {
        return reservaRepository.findById(id)
                .map(ReservaResponse::de)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada."));
    }

    /** Reserva del huésped identificada por el código de su comprobante (verificación del consultor virtual). */
    @Transactional(readOnly = true)
    public java.util.Optional<ReservaResponse> obtenerDeHuespedPorCodigo(Integer huespedId, String codigo) {
        return comprobanteRepository.findByNumeroComprobante(codigo.trim().toUpperCase())
                .map(ComprobanteReserva::getReserva)
                .filter(r -> r.getHuesped().getId().equals(huespedId))
                .map(ReservaResponse::de);
    }

    /** Generar comprobante (RF 07 / CU-16) para una reserva confirmada que aún no lo tiene. */
    @Transactional
    public ComprobanteResponse generarComprobante(Integer reservaId) {
        Reserva reserva = reservaRepository.findByIdForUpdate(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada."));
        if (reserva.getEstado() != EstadoReserva.CONFIRMADA) {
            throw new ConflictoReservaException("Solo se puede generar el comprobante de reservas confirmadas.");
        }
        if (comprobanteRepository.findByReservaId(reservaId).isPresent()) {
            throw new ConflictoReservaException("La reserva ya tiene un comprobante generado.");
        }
        ComprobanteReserva generado = crearComprobante(reserva);
        auditoria.registrar("COMPROBANTE_GENERADO", "Reserva", reservaId, generado.getNumeroComprobante());
        return ComprobanteResponse.de(generado);
    }

    @Transactional(readOnly = true)
    public ComprobanteResponse obtenerComprobante(Integer reservaId) {
        return comprobanteRepository.findByReservaId(reservaId)
                .map(ComprobanteResponse::de)
                .orElseThrow(() -> new RecursoNoEncontradoException("La reserva no tiene comprobante generado."));
    }

    /**
     * Proceso diario (y al arrancar): las reservas confirmadas cuya salida ya pasó quedan FINALIZADAS y el
     * estado de todas las habitaciones se recalcula a la fecha actual.
     */
    @Transactional
    public void actualizarEstadosDelDia() {
        LocalDate hoy = ValidadorFechas.hoy();
        List<Reserva> vencidas = reservaRepository
                .findByEstadoAndFechaSalidaLessThanEqual(EstadoReserva.CONFIRMADA, hoy);
        vencidas.forEach(r -> r.setEstado(EstadoReserva.FINALIZADA));
        reservaRepository.flush();
        habitacionRepository.findAll().forEach(estadoHabitaciones::recalcular);
        log.info("Estados actualizados: {} reservas finalizadas.", vencidas.size());
    }

    private Tarifa tarifaVigente(Habitacion habitacion, LocalDate fecha) {
        List<Tarifa> tarifas = tarifaRepository.findVigentes(habitacion.getTipoHabitacion().getId(), fecha);
        if (tarifas.isEmpty()) {
            throw new RecursoNoEncontradoException("No hay tarifa vigente para esa fecha.");
        }
        return tarifas.get(0);
    }

    private ComprobanteReserva crearComprobante(Reserva reserva) {
        ComprobanteReserva comprobante = new ComprobanteReserva();
        comprobante.setReserva(reserva);
        comprobante.setNumeroComprobante("CR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        return comprobanteRepository.save(comprobante);
    }
}
