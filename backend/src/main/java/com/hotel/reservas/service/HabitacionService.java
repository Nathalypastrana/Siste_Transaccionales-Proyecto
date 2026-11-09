package com.hotel.reservas.service;

import com.hotel.reservas.dto.HabitacionRequest;
import com.hotel.reservas.dto.HabitacionResponse;
import com.hotel.reservas.exception.ConflictoReservaException;
import com.hotel.reservas.exception.RecursoNoEncontradoException;
import com.hotel.reservas.model.EstadoHabitacion;
import com.hotel.reservas.model.EstadoReserva;
import com.hotel.reservas.model.Habitacion;
import com.hotel.reservas.model.Tarifa;
import com.hotel.reservas.model.TipoHabitacion;
import com.hotel.reservas.repository.HabitacionRepository;
import com.hotel.reservas.repository.ReservaRepository;
import com.hotel.reservas.repository.TarifaRepository;
import com.hotel.reservas.repository.TipoHabitacionRepository;
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
    private final TipoHabitacionRepository tipoRepository;
    private final ReservaRepository reservaRepository;
    private final ActualizadorEstadoHabitacion estadoHabitaciones;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<HabitacionResponse> listar() {
        LocalDate hoy = ValidadorFechas.hoy();
        return habitacionRepository.findAllByOrderByNumeroAsc().stream()
                .map(h -> aRespuesta(h, hoy))
                .toList();
    }

    /** Habitaciones libres para el rango de fechas, con la tarifa vigente a la fecha de entrada. */
    @Transactional(readOnly = true)
    public List<HabitacionResponse> disponibles(LocalDate entrada, LocalDate salida) {
        ValidadorFechas.validar(entrada, salida);
        return habitacionRepository
                .findDisponibles(entrada, salida,
                        EnumSet.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA),
                        EstadoHabitacion.MANTENIMIENTO)
                .stream()
                .map(h -> aRespuesta(h, entrada))
                .toList();
    }

    /** Registrar habitación (RF 09 / CU-04). */
    @Transactional
    public HabitacionResponse crear(HabitacionRequest req) {
        String numero = req.numero().trim();
        if (habitacionRepository.findByNumero(numero).isPresent()) {
            throw new ConflictoReservaException("Ya existe una habitación con el número " + numero + ".");
        }
        Habitacion h = new Habitacion();
        aplicar(h, req, numero);
        h = habitacionRepository.save(h);
        estadoHabitaciones.recalcular(h);
        auditoria.registrar("HABITACION_CREADA", "Habitacion", h.getId(), "Hab " + h.getNumero() + ", tipo "
                + h.getTipoHabitacion().getNombre() + ", piso " + h.getPiso() + ", capacidad " + h.getCapacidad());
        return aRespuesta(h, ValidadorFechas.hoy());
    }

    /**
     * Actualizar habitación (RF 09 / CU-04). Pasar a MANTENIMIENTO se rechaza si la habitación tiene reservas
     * vigentes (pendientes o confirmadas que aún no terminan).
     */
    @Transactional
    public HabitacionResponse actualizar(Integer id, HabitacionRequest req) {
        Habitacion h = habitacionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Habitación no encontrada."));
        String numero = req.numero().trim();
        habitacionRepository.findByNumero(numero).ifPresent(otra -> {
            if (!otra.getId().equals(id)) {
                throw new ConflictoReservaException("Ya existe una habitación con el número " + numero + ".");
            }
        });
        EstadoHabitacion nuevoEstado = req.estado() != null ? req.estado() : h.getEstado();
        if (nuevoEstado == EstadoHabitacion.MANTENIMIENTO && h.getEstado() != EstadoHabitacion.MANTENIMIENTO) {
            long vigentes = reservaRepository.contarVigentesDe(id, ValidadorFechas.hoy(),
                    EnumSet.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA));
            if (vigentes > 0) {
                throw new ConflictoReservaException("No se puede pasar a mantenimiento: la habitación tiene "
                        + vigentes + " reserva(s) vigente(s). Resuélvelas primero.");
            }
        }
        String antes = "Hab " + h.getNumero() + ", tipo " + h.getTipoHabitacion().getNombre() + ", piso " + h.getPiso()
                + ", capacidad " + h.getCapacidad() + ", estado " + h.getEstado();
        aplicar(h, req, numero);
        habitacionRepository.flush();
        estadoHabitaciones.recalcular(h);
        auditoria.registrar("HABITACION_ACTUALIZADA", "Habitacion", id, "Antes: " + antes + ". Ahora: Hab "
                + h.getNumero() + ", tipo " + h.getTipoHabitacion().getNombre() + ", piso " + h.getPiso()
                + ", capacidad " + h.getCapacidad() + ", estado " + h.getEstado());
        return aRespuesta(h, ValidadorFechas.hoy());
    }

    /** Cambia solo el estado. Reutiliza las reglas de actualizar (p. ej. mantenimiento con reservas vigentes). */
    @Transactional
    public HabitacionResponse cambiarEstado(Integer id, EstadoHabitacion estado) {
        Habitacion h = habitacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Habitación no encontrada."));
        return actualizar(id, new HabitacionRequest(h.getNumero(), h.getPiso(), h.getCapacidad(), estado,
                h.getTipoHabitacion().getId()));
    }

    private void aplicar(Habitacion h, HabitacionRequest req, String numero) {
        TipoHabitacion tipo = tipoRepository.findById(req.tipoHabitacionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("El tipo de habitación no existe."));
        h.setNumero(numero);
        h.setPiso(req.piso());
        h.setCapacidad(req.capacidad());
        h.setTipoHabitacion(tipo);
        if (req.estado() != null) {
            h.setEstado(req.estado());
        }
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
                tarifas.isEmpty() ? null : tarifas.get(0).getValor(),
                h.getTipoHabitacion().getId());
    }
}
