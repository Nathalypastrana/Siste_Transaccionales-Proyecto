package com.hotel.reservas.scheduling;

import com.hotel.reservas.notificacion.AvisosProgramadosService;
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
    private final AvisosProgramadosService avisos;

    @Scheduled(cron = "0 5 0 * * *", zone = "America/Bogota")
    public void diaria() {
        ejecutar();
    }

    /** 7:00 a. m.: resumen del día (llegadas, salidas, ocupación y tarifas por vencer) para los administradores. */
    @Scheduled(cron = "0 0 7 * * *", zone = "America/Bogota")
    public void resumenDiario() {
        try {
            avisos.resumenDiario();
        } catch (Exception e) {
            log.error("No se pudo enviar el resumen diario: {}", e.getMessage());
        }
    }

    /** 9:00 a. m.: recordatorio de llegada a los huéspedes que entran mañana. */
    @Scheduled(cron = "0 0 9 * * *", zone = "America/Bogota")
    public void recordatorios() {
        try {
            avisos.recordatoriosDeLlegada(false);
        } catch (Exception e) {
            log.error("No se pudieron enviar los recordatorios de llegada: {}", e.getMessage());
        }
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
