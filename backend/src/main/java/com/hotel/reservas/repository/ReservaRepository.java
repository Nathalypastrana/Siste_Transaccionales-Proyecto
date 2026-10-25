package com.hotel.reservas.repository;

import com.hotel.reservas.model.EstadoReserva;
import com.hotel.reservas.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

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
}
