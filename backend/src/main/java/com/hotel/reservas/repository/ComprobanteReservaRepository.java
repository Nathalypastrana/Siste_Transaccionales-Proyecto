package com.hotel.reservas.repository;

import com.hotel.reservas.model.ComprobanteReserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComprobanteReservaRepository extends JpaRepository<ComprobanteReserva, Integer> {

    Optional<ComprobanteReserva> findByReservaId(Integer reservaId);

    Optional<ComprobanteReserva> findByNumeroComprobante(String numeroComprobante);
}
