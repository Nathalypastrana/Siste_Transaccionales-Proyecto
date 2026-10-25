package com.hotel.reservas.repository;

import com.hotel.reservas.model.EstadoHabitacion;
import com.hotel.reservas.model.EstadoReserva;
import com.hotel.reservas.model.Habitacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface HabitacionRepository extends JpaRepository<Habitacion, Integer> {

    /**
     * Bloquea la fila de la habitación hasta que termine la transacción.
     * Dos reservas simultáneas sobre la misma habitación quedan en fila:
     * la segunda espera a que la primera confirme o revierta.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Habitacion h where h.id = :id")
    Optional<Habitacion> findByIdForUpdate(@Param("id") Integer id);

    List<Habitacion> findAllByOrderByNumeroAsc();

    /** Habitaciones sin cruces de fechas con reservas activas y fuera de mantenimiento. */
    @Query("""
            select h from Habitacion h
            where h.estado <> :excluido
              and not exists (
                  select r.id from Reserva r
                  where r.habitacion = h
                    and r.estado in :estados
                    and r.fechaEntrada < :salida
                    and r.fechaSalida > :entrada)
            order by h.numero
            """)
    List<Habitacion> findDisponibles(@Param("entrada") LocalDate entrada,
                                     @Param("salida") LocalDate salida,
                                     @Param("estados") Collection<EstadoReserva> estados,
                                     @Param("excluido") EstadoHabitacion excluido);
}
