package com.hotel.reservas.notificacion;

import com.hotel.reservas.service.AuditoriaService;
import com.hotel.reservas.service.CorreoService;
import com.hotel.reservas.util.Telefonos;
import com.hotel.reservas.whatsapp.WhatsAppClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Avisa al huésped cuando se crea, modifica o cancela una reserva, por correo, por WhatsApp o por ambos
 * (NOTIFICACIONES_CANALES). Solo se envía cuando la transacción de la reserva ya se confirmó (no se avisa de
 * reservas que se deshicieron) y sin bloquear la respuesta al recepcionista. Cada intento queda en la auditoría.
 *
 * WhatsApp: fuera de la ventana de 24 h desde el último mensaje del cliente, Meta solo permite plantillas
 * aprobadas; por eso, si WHATSAPP_PLANTILLA_RESERVA está definida, se usa la plantilla.
 */
@Slf4j
@Service
public class NotificacionService {

    private final CorreoService correo;
    private final WhatsAppClient whatsapp;
    private final AuditoriaService auditoria;
    private final Set<String> canales;
    private final String plantilla;
    private final String idiomaPlantilla;
    private final String indicativo;
    private final String hotel;

    public NotificacionService(CorreoService correo, WhatsAppClient whatsapp, AuditoriaService auditoria,
                               @Value("${notificaciones.canales:correo,whatsapp}") String canales,
                               @Value("${whatsapp.plantilla-reserva:}") String plantilla,
                               @Value("${whatsapp.idioma-plantilla:es}") String idiomaPlantilla,
                               @Value("${notificaciones.indicativo-pais:57}") String indicativo,
                               @Value("${notificaciones.nombre-hotel:Hotel Reservas}") String hotel) {
        this.correo = correo;
        this.whatsapp = whatsapp;
        this.auditoria = auditoria;
        this.canales = Set.of(canales.toLowerCase().replace(" ", "").split(","));
        this.plantilla = plantilla;
        this.idiomaPlantilla = idiomaPlantilla;
        this.indicativo = indicativo;
        this.hotel = hotel;
    }

    public Set<String> canalesActivos() {
        return canales;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alConfirmarse(EventoReserva e) {
        notificar(e);
    }

    /** Envía el aviso por los canales activos. También lo usan los recordatorios programados. */
    public void notificar(EventoReserva e) {
        try {
            if (canales.contains("correo")) {
                notificarCorreo(e);
            }
            if (canales.contains("whatsapp")) {
                notificarWhatsApp(e);
            }
        } catch (Exception ex) {
            // Nunca debe afectar a la operación de la reserva, que ya se confirmó.
            log.error("Error al notificar la reserva {}: {}", e.reservaId(), ex.getMessage());
        }
    }

    private void notificarCorreo(EventoReserva e) {
        if (e.correo() == null || e.correo().isBlank()) {
            registrar(e, "NOTIFICACION_OMITIDA", "correo: el huésped no tiene correo registrado");
            return;
        }
        correo.enviar(e.correo().trim(), asunto(e), cuerpoCorreo(e)).thenAccept(enviado ->
                registrar(e, enviado ? "NOTIFICACION_ENVIADA" : "NOTIFICACION_FALLIDA",
                        "correo a " + enmascararCorreo(e.correo())
                                + (enviado ? "" : " (SMTP no configurado o error de envío)")));
    }

    private void notificarWhatsApp(EventoReserva e) {
        String destino = Telefonos.paraWhatsApp(e.telefono(), indicativo);
        if (destino.isEmpty()) {
            registrar(e, "NOTIFICACION_OMITIDA", "whatsapp: el huésped no tiene teléfono registrado");
            return;
        }
        if (!whatsapp.configurado()) {
            registrar(e, "NOTIFICACION_FALLIDA", "whatsapp a " + Telefonos.enmascarar(destino) + " (WhatsApp no configurado)");
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                if (plantilla.isBlank()) {
                    whatsapp.enviar(destino, "Hola " + primerNombre(e) + ". " + resumen(e));
                } else {
                    whatsapp.enviarPlantilla(destino, plantilla, idiomaPlantilla, List.of(primerNombre(e), resumen(e)));
                }
                registrar(e, "NOTIFICACION_ENVIADA", "whatsapp a " + Telefonos.enmascarar(destino)
                        + (plantilla.isBlank() ? " (texto libre)" : " (plantilla " + plantilla + ")"));
            } catch (Exception ex) {
                log.warn("No se pudo enviar el WhatsApp de la reserva {} a {}: {}", e.reservaId(),
                        Telefonos.enmascarar(destino), ex.getMessage());
                registrar(e, "NOTIFICACION_FALLIDA", "whatsapp a " + Telefonos.enmascarar(destino)
                        + ": " + recortar(ex.getMessage()));
            }
        });
    }

    private void registrar(EventoReserva e, String accion, String detalle) {
        auditoria.registrarComo(null, null, "SISTEMA", accion, "Reserva", e.reservaId(),
                e.tipo() + " - " + detalle);
    }

    /** Texto en una sola línea (las plantillas de WhatsApp no admiten saltos de línea en las variables). */
    static String resumen(EventoReserva e) {
        String datos = "habitación " + e.habitacion() + ", del " + e.entrada() + " al " + e.salida()
                + ", total " + moneda(e) + ". Código de reserva: " + (e.comprobante() == null ? "—" : e.comprobante()) + ".";
        return switch (e.tipo()) {
            case CREADA -> "Tu reserva #" + e.reservaId() + " fue confirmada: " + datos;
            case MODIFICADA -> "Tu reserva #" + e.reservaId() + " fue modificada. Ahora es: " + datos;
            case CANCELADA -> "Tu reserva #" + e.reservaId() + " (habitación " + e.habitacion() + ", del " + e.entrada()
                    + " al " + e.salida() + ") fue cancelada.";
            case RECORDATORIO -> "Te esperamos mañana, " + e.entrada() + ". Tu reserva #" + e.reservaId() + ": "
                    + "habitación " + e.habitacion() + " hasta el " + e.salida() + ", total " + moneda(e)
                    + ". Código de reserva: " + (e.comprobante() == null ? "—" : e.comprobante()) + ".";
        };
    }

    private String asunto(EventoReserva e) {
        return switch (e.tipo()) {
            case CREADA -> "Reserva confirmada #" + e.reservaId() + " - " + hotel;
            case MODIFICADA -> "Reserva modificada #" + e.reservaId() + " - " + hotel;
            case CANCELADA -> "Reserva cancelada #" + e.reservaId() + " - " + hotel;
            case RECORDATORIO -> "Te esperamos mañana - reserva #" + e.reservaId() + " - " + hotel;
        };
    }

    private String cuerpoCorreo(EventoReserva e) {
        return "Hola " + primerNombre(e) + ",\n\n" + resumen(e) + "\n\n"
                + "Si tienes dudas, responde a este correo o comunícate con la recepción de " + hotel + ".\n\n"
                + "Gracias por elegirnos.";
    }

    private static String primerNombre(EventoReserva e) {
        String n = e.nombres() == null ? "" : e.nombres().trim();
        int i = n.indexOf(' ');
        return n.isEmpty() ? "huésped" : (i > 0 ? n.substring(0, i) : n);
    }

    private static String moneda(EventoReserva e) {
        return e.total() == null ? "—" : NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")).format(e.total());
    }

    private static String enmascararCorreo(String c) {
        int i = c.indexOf('@');
        return i <= 1 ? "***" : c.charAt(0) + "***" + c.substring(i);
    }

    private static String recortar(String s) {
        if (s == null) {
            return "";
        }
        String limpio = s.replaceAll("\\s+", " ");
        return limpio.length() > 200 ? limpio.substring(0, 200) : limpio;
    }
}
