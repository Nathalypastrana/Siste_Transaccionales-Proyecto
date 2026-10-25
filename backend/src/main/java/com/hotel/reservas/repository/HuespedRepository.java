package com.hotel.reservas.repository;

import com.hotel.reservas.model.Huesped;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HuespedRepository extends JpaRepository<Huesped, Integer> {

    Optional<Huesped> findByTipoDocumentoAndNumeroDocumento(String tipoDocumento, String numeroDocumento);

    List<Huesped> findAllByOrderByApellidosAscNombresAsc();
}
