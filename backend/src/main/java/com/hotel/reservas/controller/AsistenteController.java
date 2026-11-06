package com.hotel.reservas.controller;

import com.hotel.reservas.asistente.AsistenteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Simulador del canal de WhatsApp para probar y demostrar el consultor sin una cuenta de Meta.
 * Ejecuta exactamente el mismo flujo que el webhook (AsistenteService), pero devuelve la respuesta por HTTP.
 *
 * Está deshabilitado por defecto: permite consultar reservas a partir de un número de teléfono sin
 * autenticación, por lo que solo debe activarse en desarrollo (ASISTENTE_SIMULADOR=true).
 */
@RestController
@RequestMapping("/api/asistente")
@RequiredArgsConstructor
public class AsistenteController {

    public record SimulacionRequest(
            @NotBlank(message = "El teléfono es obligatorio") String telefono,
            @NotBlank(message = "El mensaje es obligatorio") String mensaje) {
    }

    private final AsistenteService asistente;

    @Value("${asistente.simulador-habilitado:false}")
    private boolean simuladorHabilitado;

    @PostMapping("/simular")
    public ResponseEntity<Object> simular(@Valid @RequestBody SimulacionRequest req) {
        if (!simuladorHabilitado) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ProblemDetail.forStatusAndDetail(
                    HttpStatus.FORBIDDEN,
                    "El simulador está deshabilitado. Inicia el backend con ASISTENTE_SIMULADOR=true."));
        }
        String respuesta = asistente.responder(req.telefono(), null, req.mensaje());
        return ResponseEntity.ok(Map.of("respuesta", respuesta));
    }
}
