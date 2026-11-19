package com.hotel.reservas.repository;

import com.hotel.reservas.model.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Integer>, JpaSpecificationExecutor<Auditoria> {

    /** Sirve para no repetir un aviso ya enviado (p. ej. el recordatorio de llegada). */
    boolean existsByAccionAndEntidadAndEntidadIdAndDetalleStartingWithAndFechaAfter(
            String accion, String entidad, String entidadId, String detalleInicia, LocalDateTime desde);
}
