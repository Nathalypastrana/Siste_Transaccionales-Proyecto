package com.hotel.reservas.service;

import com.hotel.reservas.dto.TipoHabitacionRequest;
import com.hotel.reservas.dto.TipoHabitacionResponse;
import com.hotel.reservas.exception.ConflictoReservaException;
import com.hotel.reservas.exception.RecursoNoEncontradoException;
import com.hotel.reservas.model.TipoHabitacion;
import com.hotel.reservas.repository.TipoHabitacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Gestionar tipos de habitación (RF 10 / CU-03). */
@Service
@RequiredArgsConstructor
public class TipoHabitacionService {

    private final TipoHabitacionRepository repository;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<TipoHabitacionResponse> listar() {
        return repository.findAllByOrderByNombreAsc().stream().map(TipoHabitacionResponse::de).toList();
    }

    @Transactional
    public TipoHabitacionResponse crear(TipoHabitacionRequest req) {
        String nombre = req.nombre().trim();
        if (repository.findByNombreIgnoreCase(nombre).isPresent()) {
            throw new ConflictoReservaException("Ya existe un tipo de habitación llamado \"" + nombre + "\".");
        }
        TipoHabitacion t = new TipoHabitacion();
        aplicar(t, req, nombre);
        t = repository.save(t);
        auditoria.registrar("TIPO_CREADO", "TipoHabitacion", t.getId(), nombre + ", capacidad " + t.getCapacidad());
        return TipoHabitacionResponse.de(t);
    }

    @Transactional
    public TipoHabitacionResponse actualizar(Integer id, TipoHabitacionRequest req) {
        TipoHabitacion t = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de habitación no encontrado."));
        String nombre = req.nombre().trim();
        repository.findByNombreIgnoreCase(nombre).ifPresent(otro -> {
            if (!otro.getId().equals(id)) {
                throw new ConflictoReservaException("Ya existe un tipo de habitación llamado \"" + nombre + "\".");
            }
        });
        String antes = t.getNombre() + ", capacidad " + t.getCapacidad();
        aplicar(t, req, nombre);
        auditoria.registrar("TIPO_ACTUALIZADO", "TipoHabitacion", id, "Antes: " + antes + ". Ahora: " + nombre
                + ", capacidad " + t.getCapacidad());
        return TipoHabitacionResponse.de(t);
    }

    private void aplicar(TipoHabitacion t, TipoHabitacionRequest req, String nombre) {
        t.setNombre(nombre);
        t.setDescripcion(req.descripcion() == null || req.descripcion().isBlank() ? null : req.descripcion().trim());
        t.setCapacidad(req.capacidad());
    }
}
