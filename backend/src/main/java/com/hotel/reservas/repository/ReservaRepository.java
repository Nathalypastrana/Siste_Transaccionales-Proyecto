package com.hotel.reservas.repository;

import com.hotel.reservas.model.EstadoReserva;
import com.hotel.reservas.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Integer>, JpaSpecificationExecutor<Reserva> {

    /**
     * Cuenta reservas activas (PENDIENTE o CONFIRMADA) de una habitación que se cruzan con el rango.
     * Dos rangos se cruzan cuando entrada1 < salida2 y salida1 > entrada2
     * (la fecha de salida no cuenta como noche ocupada).
     */
    @Query("""
            select count(r) from Reserva r
            where r.habitacion.id = :habitacionId
              and r.estado in :estados
              and r.fechaEntrada < :salida
              and r.fechaSalida > :entrada
            """)
    long contarCruces(@Param("habitacionId") Integer habitacionId,
                      @Param("entrada") LocalDate entrada,
                      @Param("salida") LocalDate salida,
                      @Param("estados") Collection<EstadoReserva> estados);

    @Query("""
            select r from Reserva r
            join fetch r.huesped
            join fetch r.habitacion
            order by r.id desc
            """)
    List<Reserva> listarConDetalle();

    @Query("""
            select r from Reserva r
            join fetch r.huesped
            join fetch r.habitacion
            where r.huesped.id = :huespedId
            order by r.fechaEntrada desc
            """)
    List<Reserva> listarPorHuesped(@Param("huespedId") Integer huespedId);

    /** Igual que contarCruces, pero ignorando una reserva (la que se está modificando). */
    @Query("""
            select count(r) from Reserva r
            where r.habitacion.id = :habitacionId
              and r.id <> :excluirId
              and r.estado in :estados
              and r.fechaEntrada < :salida
              and r.fechaSalida > :entrada
            """)
    long contarCrucesExcluyendo(@Param("habitacionId") Integer habitacionId,
                                @Param("entrada") LocalDate entrada,
                                @Param("salida") LocalDate salida,
                                @Param("estados") Collection<EstadoReserva> estados,
                                @Param("excluirId") Integer excluirId);

    /** Reservas activas de una habitación cuyo periodo incluye la fecha (entrada <= fecha < salida). */
    @Query("""
            select count(r) from Reserva r
            where r.habitacion.id = :habitacionId
              and r.estado in :estados
              and r.fechaEntrada <= :fecha
              and r.fechaSalida > :fecha
            """)
    long contarActivasEn(@Param("habitacionId") Integer habitacionId,
                         @Param("fecha") LocalDate fecha,
                         @Param("estados") Collection<EstadoReserva> estados);

    /** Bloquea la fila de la reserva mientras dura la transacción (modificar / cancelar). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.id = :id")
    java.util.Optional<Reserva> findByIdForUpdate(@Param("id") Integer id);

    List<Reserva> findByEstadoAndFechaSalidaLessThanEqual(EstadoReserva estado, LocalDate fecha);

    /** Reservas (de cualquier estado) que se cruzan con el periodo [desde, hasta). */
    @Query("""
            select r from Reserva r
            join fetch r.habitacion h
            join fetch h.tipoHabitacion
            where r.fechaEntrada < :hasta and r.fechaSalida > :desde
            """)
    List<Reserva> listarEnPeriodo(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /** Reservas activas de la habitación que todavía no terminan (salida posterior a la fecha dada). */
    @Query("""
            select count(r) from Reserva r
            where r.habitacion.id = :habitacionId
              and r.estado in :estados
              and r.fechaSalida > :fecha
            """)
    long contarVigentesDe(@Param("habitacionId") Integer habitacionId,
                          @Param("fecha") LocalDate fecha,
                          @Param("estados") Collection<EstadoReserva> estados);
}
