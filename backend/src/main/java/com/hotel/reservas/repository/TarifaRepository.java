package com.hotel.reservas.repository;

import com.hotel.reservas.model.Tarifa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface TarifaRepository extends JpaRepository<Tarifa, Long> {

    @Query("""
            select t from Tarifa t
            where t.tipoHabitacion.id = :tipoId
              and :fecha between t.fechaInicio and t.fechaFin
            order by t.fechaInicio desc
            """)
    Optional<Tarifa> findVigente(@Param("tipoId") Long tipoId, @Param("fecha") LocalDate fecha);
}
