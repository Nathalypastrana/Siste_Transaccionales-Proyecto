package com.hotel.reservas.asistente;

import com.hotel.reservas.whatsapp.WhatsAppClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Avisa al recepcionista cuando el consultor no puede resolver una consulta. */
@Slf4j
@Component
@RequiredArgsConstructor
public class Recepcion {

    private final WhatsAppClient whatsApp;

    @Value("${asistente.numero-recepcion:}")
    private String numeroRecepcion;

    public void notificar(String telefonoHuesped, String nombreHuesped, String motivo) {
        log.warn("Consulta derivada a recepción. Huésped: {} ({}). Motivo: {}",
                nombreHuesped, com.hotel.reservas.util.Telefonos.enmascarar(telefonoHuesped), motivo);
        if (numeroRecepcion == null || numeroRecepcion.isBlank()) {
            return;
        }
        try {
            whatsApp.enviar(numeroRecepcion,
                    "Consulta derivada por el consultor virtual.\nHuésped: " + nombreHuesped
                            + "\nTeléfono: " + telefonoHuesped + "\nMotivo: " + motivo);
        } catch (Exception e) {
            log.error("No se pudo avisar a recepción: {}", e.getMessage());
        }
    }
}
