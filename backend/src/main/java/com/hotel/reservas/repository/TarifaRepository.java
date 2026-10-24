package com.hotel.reservas.repository;

import com.hotel.reservas.model.Tarifa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TarifaRepository extends JpaRepository<Tarifa, Integer> {

    /** Tarifas activas del tipo de habitación que cubren la fecha, la más reciente primero. */
    @Query("""
            select t from Tarifa t
            where t.tipoHabitacion.id = :tipoId
              and t.activa = true
              and :fecha between t.fechaInicio and t.fechaFin
            order by t.fechaInicio desc
            """)
    List<Tarifa> findVigentes(@Param("tipoId") Integer tipoId, @Param("fecha") LocalDate fecha);
}
