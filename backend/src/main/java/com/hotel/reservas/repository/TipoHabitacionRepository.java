package com.hotel.reservas.repository;

import com.hotel.reservas.model.TipoHabitacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TipoHabitacionRepository extends JpaRepository<TipoHabitacion, Integer> {

    List<TipoHabitacion> findAllByOrderByNombreAsc();

    Optional<TipoHabitacion> findByNombreIgnoreCase(String nombre);
}
