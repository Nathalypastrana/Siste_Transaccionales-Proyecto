package com.hotel.reservas.repository;

import com.hotel.reservas.model.RecuperacionClave;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RecuperacionClaveRepository extends JpaRepository<RecuperacionClave, Integer> {

    Optional<RecuperacionClave> findByTokenHash(String tokenHash);

    List<RecuperacionClave> findByUsuarioIdAndUsadoFalse(Integer usuarioId);

    long countByUsuarioIdAndCreadaAfter(Integer usuarioId, LocalDateTime desde);
}
