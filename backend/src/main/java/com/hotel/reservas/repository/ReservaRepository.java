package com.hotel.reservas.repository;

import java.time.LocalDate;
import java.util.List;

import com.hotel.reservas.entity.Reserva;
import com.hotel.reservas.enums.EstadoReserva;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository
        extends JpaRepository<Reserva, Integer> {

    @Query("""
        select case when count(r) > 0
        then true else false end
        from Reserva r
        where r.habitacion.idHabitacion = :idHab
        and r.estado = :confirmada
        and r.fechaEntrada < :salida
        and r.fechaSalida > :entrada
        and r.idReserva <> :excluirId
        """)
    boolean existeTraslape(
        @Param("idHab") Integer idHab,
        @Param("entrada") LocalDate entrada,
        @Param("salida") LocalDate salida,
        @Param("excluirId") Integer excluirId,
        @Param("confirmada") EstadoReserva confirmada
    );

    List<Reserva> findByHabitacion_IdHabitacion(
        Integer idHabitacion
    );
}
