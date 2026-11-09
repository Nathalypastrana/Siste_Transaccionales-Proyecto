package com.hotel.reservas.repository;

import com.hotel.reservas.model.AutorizacionDatos;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AutorizacionDatosRepository extends JpaRepository<AutorizacionDatos, Integer> {

    Optional<AutorizacionDatos> findByTelefono(String telefono);
}
