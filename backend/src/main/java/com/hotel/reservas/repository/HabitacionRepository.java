package com.hotel.reservas.repository;

import com.hotel.reservas.model.Habitacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface HabitacionRepository extends JpaRepository<Habitacion, Long> {

    /**
     * Bloquea la fila de la habitación hasta que termine la transacción.
     * Dos reservas simultáneas sobre la misma habitación quedan en fila:
     * la segunda espera a que la primera confirme o revierta.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Habitacion h where h.id = :id")
    Optional<Habitacion> findByIdForUpdate(@Param("id") Long id);
}
