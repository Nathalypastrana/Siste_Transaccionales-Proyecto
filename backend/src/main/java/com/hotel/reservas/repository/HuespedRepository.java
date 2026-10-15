package com.hotel.reservas.repository;

import com.hotel.reservas.entity.Huesped;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HuespedRepository
        extends JpaRepository<Huesped, Integer> {

    boolean existsByTipoDocumentoAndNumeroDocumento(
        String tipoDocumento,
        String numeroDocumento
    );
}
