package com.hotel.reservas.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.hotel.reservas.entity.Habitacion;
import com.hotel.reservas.enums.EstadoHabitacion;
import com.hotel.reservas.enums.EstadoReserva;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HabitacionRepository
        extends JpaRepository<Habitacion, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Habitacion h where h.idHabitacion = :id")
    Optional<Habitacion> findByIdForUpdate(
        @Param("id") Integer id
    );

    @Query("""
        select h from Habitacion h
        where h.estado <> :mantenimiento
        and not exists (
            select r.idReserva from Reserva r
            where r.habitacion = h
            and r.estado = :confirmada
            and r.fechaEntrada < :salida
            and r.fechaSalida > :entrada
        )
        """)
    List<Habitacion> disponibles(
        @Param("entrada") LocalDate entrada,
        @Param("salida") LocalDate salida,
        @Param("mantenimiento") EstadoHabitacion mantenimiento,
        @Param("confirmada") EstadoReserva confirmada
    );
}
