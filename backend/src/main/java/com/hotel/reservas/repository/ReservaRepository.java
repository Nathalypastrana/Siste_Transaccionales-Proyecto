package com.hotel.reservas.repository;

import com.hotel.reservas.model.EstadoReserva;
import com.hotel.reservas.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /**
     * Cuenta reservas activas de una habitación que se cruzan con el rango pedido.
     * Dos rangos se cruzan cuando entrada1 < salida2 y salida1 > entrada2
     * (la fecha de salida no cuenta como noche ocupada).
     */
    @Query("""
            select count(r) from Reserva r
            where r.habitacion.id = :habitacionId
              and r.estado = :estado
              and r.fechaEntrada < :salida
              and r.fechaSalida > :entrada
            """)
    long contarCruces(@Param("habitacionId") Long habitacionId,
                      @Param("entrada") LocalDate entrada,
                      @Param("salida") LocalDate salida,
                      @Param("estado") EstadoReserva estado);
}
