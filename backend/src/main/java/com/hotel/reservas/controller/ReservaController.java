package com.hotel.reservas.controller;

import com.hotel.reservas.dto.ComprobanteResponse;
import com.hotel.reservas.dto.FiltroReservas;
import com.hotel.reservas.dto.ModificarReservaRequest;
import com.hotel.reservas.dto.ReservaRequest;
import com.hotel.reservas.dto.ReservaResponse;
import com.hotel.reservas.model.EstadoReserva;
import com.hotel.reservas.security.UsuarioAutenticado;
import com.hotel.reservas.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    /** Historial con filtros opcionales: huespedId, habitacionId, estado, desde, hasta, limite. */
    @GetMapping
    public List<ReservaResponse> listar(
            @RequestParam(required = false) Integer huespedId,
            @RequestParam(required = false) Integer habitacionId,
            @RequestParam(required = false) EstadoReserva estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer limite) {
        return reservaService.listar(new FiltroReservas(huespedId, habitacionId, estado, desde, hasta, limite));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse crear(@Valid @RequestBody ReservaRequest request,
                                 @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return reservaService.crear(request, usuario.id());
    }

    @GetMapping("/{id}")
    public ReservaResponse obtener(@PathVariable Integer id) {
        return reservaService.obtener(id);
    }

    @PatchMapping("/{id}")
    public ReservaResponse modificar(@PathVariable Integer id, @RequestBody ModificarReservaRequest request) {
        return reservaService.modificar(id, request);
    }

    @PatchMapping("/{id}/cancelar")
    public ReservaResponse cancelar(@PathVariable Integer id) {
        return reservaService.cancelar(id);
    }

    @PostMapping("/{id}/comprobante")
    @ResponseStatus(HttpStatus.CREATED)
    public ComprobanteResponse generarComprobante(@PathVariable Integer id) {
        return reservaService.generarComprobante(id);
    }

    @GetMapping("/{id}/comprobante")
    public ComprobanteResponse obtenerComprobante(@PathVariable Integer id) {
        return reservaService.obtenerComprobante(id);
    }
}
