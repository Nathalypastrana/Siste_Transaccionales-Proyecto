package com.hotel.reservas.scheduling;

import com.hotel.reservas.service.ReservaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Proceso diario de actualización de estados (CU-15), a las 00:05 hora de Bogotá y al arrancar. */
@Slf4j
@Component
@RequiredArgsConstructor
public class TareasProgramadas {

    private final ReservaService reservaService;

    @Scheduled(cron = "0 5 0 * * *", zone = "America/Bogota")
    public void diaria() {
        ejecutar();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        ejecutar();
    }

    private void ejecutar() {
        try {
            reservaService.actualizarEstadosDelDia();
        } catch (Exception e) {
            log.error("No se pudo actualizar el estado de las habitaciones: {}", e.getMessage());
        }
    }
}
