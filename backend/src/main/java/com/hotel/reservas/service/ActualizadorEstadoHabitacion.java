package com.hotel.reservas.service;

import com.hotel.reservas.model.EstadoHabitacion;
import com.hotel.reservas.model.EstadoReserva;
import com.hotel.reservas.model.Habitacion;
import com.hotel.reservas.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

/**
 * Actualizar estado de habitación (CU-15). El estado refleja la situación a la fecha de hoy:
 * RESERVADA si hay una reserva vigente cuyo periodo incluye hoy, DISPONIBLE si no. OCUPADA y
 * MANTENIMIENTO se conservan. La disponibilidad por fechas NO sale de este estado sino de las reservas.
 * Debe llamarse dentro de la transacción que modificó las reservas.
 */
@Component
@RequiredArgsConstructor
public class ActualizadorEstadoHabitacion {

    static final Set<EstadoReserva> ESTADOS_ACTIVOS = EnumSet.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);

    private final ReservaRepository reservaRepository;

    public void recalcular(Habitacion habitacion) {
        if (habitacion.getEstado() == EstadoHabitacion.MANTENIMIENTO
                || habitacion.getEstado() == EstadoHabitacion.OCUPADA) {
            return;
        }
        LocalDate hoy = ValidadorFechas.hoy();
        boolean vigente = reservaRepository.contarActivasEn(habitacion.getId(), hoy, ESTADOS_ACTIVOS) > 0;
        habitacion.setEstado(vigente ? EstadoHabitacion.RESERVADA : EstadoHabitacion.DISPONIBLE);
    }
}
