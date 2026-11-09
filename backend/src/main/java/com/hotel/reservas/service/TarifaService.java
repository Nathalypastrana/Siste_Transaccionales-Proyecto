package com.hotel.reservas.service;

import com.hotel.reservas.dto.TarifaRequest;
import com.hotel.reservas.dto.TarifaResponse;
import com.hotel.reservas.exception.RecursoNoEncontradoException;
import com.hotel.reservas.model.Tarifa;
import com.hotel.reservas.model.TipoHabitacion;
import com.hotel.reservas.repository.TarifaRepository;
import com.hotel.reservas.repository.TipoHabitacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Gestionar tarifas (RF 08 / CU-05). Una tarifa nueva solo afecta a las reservas que se creen después. */
@Service
@RequiredArgsConstructor
public class TarifaService {

    private final TarifaRepository tarifaRepository;
    private final TipoHabitacionRepository tipoRepository;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<TarifaResponse> listar() {
        return tarifaRepository.findAllByOrderByTipoHabitacionNombreAscFechaInicioDesc().stream()
                .map(TarifaResponse::de)
                .toList();
    }

    @Transactional
    public TarifaResponse crear(TarifaRequest req) {
        Tarifa t = new Tarifa();
        aplicar(t, req);
        t = tarifaRepository.save(t);
        auditoria.registrar("TARIFA_CREADA", "Tarifa", t.getId(), descripcion(t));
        return TarifaResponse.de(t);
    }

    @Transactional
    public TarifaResponse actualizar(Integer id, TarifaRequest req) {
        Tarifa t = tarifaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tarifa no encontrada."));
        String antes = descripcion(t);
        aplicar(t, req);
        auditoria.registrar("TARIFA_ACTUALIZADA", "Tarifa", id, "Antes: " + antes + ". Ahora: " + descripcion(t));
        return TarifaResponse.de(t);
    }

    @Transactional
    public TarifaResponse cambiarEstado(Integer id, boolean activa) {
        Tarifa t = tarifaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tarifa no encontrada."));
        t.setActiva(activa);
        auditoria.registrar(activa ? "TARIFA_ACTIVADA" : "TARIFA_DESACTIVADA", "Tarifa", id, descripcion(t));
        return TarifaResponse.de(t);
    }

    private String descripcion(Tarifa t) {
        return t.getTipoHabitacion().getNombre() + " $" + t.getValor() + " del " + t.getFechaInicio() + " al "
                + t.getFechaFin() + (Boolean.TRUE.equals(t.getActiva()) ? " (activa)" : " (inactiva)");
    }

    private void aplicar(Tarifa t, TarifaRequest req) {
        if (!req.fechaFin().isAfter(req.fechaInicio())) {
            throw new IllegalArgumentException("La fecha de fin de la tarifa debe ser posterior a la de inicio.");
        }
        TipoHabitacion tipo = tipoRepository.findById(req.tipoHabitacionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("El tipo de habitación no existe."));
        t.setTipoHabitacion(tipo);
        t.setValor(req.valor());
        t.setFechaInicio(req.fechaInicio());
        t.setFechaFin(req.fechaFin());
        t.setActiva(req.activa() == null ? Boolean.TRUE : req.activa());
    }
}
