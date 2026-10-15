package com.hotel.reservas.repository;

import com.hotel.reservas.entity.Comprobante;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComprobanteRepository
        extends JpaRepository<Comprobante, Integer> {
}
