package com.hotel.reservas.repository;

import com.hotel.reservas.entity.TipoHabitacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoHabitacionRepository
        extends JpaRepository<TipoHabitacion, Integer> {
}
