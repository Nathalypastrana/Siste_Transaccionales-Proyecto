package com.hotel.reservas.notificacion;

import com.hotel.reservas.dto.AlertaTarifa;
import com.hotel.reservas.dto.ReporteOcupacion;
import com.hotel.reservas.model.EstadoReserva;
import com.hotel.reservas.model.Reserva;
import com.hotel.reservas.model.Rol;
import com.hotel.reservas.model.Usuario;
import com.hotel.reservas.repository.ComprobanteReservaRepository;
import com.hotel.reservas.repository.ReservaRepository;
import com.hotel.reservas.repository.UsuarioRepository;
import com.hotel.reservas.service.AlertaTarifaService;
import com.hotel.reservas.service.AuditoriaService;
import com.hotel.reservas.service.CorreoService;
import com.hotel.reservas.service.ReporteService;
import com.hotel.reservas.service.ValidadorFechas;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Avisos que no nacen de una acción del usuario sino del calendario:
 * - recordatorio de llegada al huésped (la víspera de su entrada);
 * - resumen diario al administrador (llegadas, salidas, ocupación y tarifas por vencer).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AvisosProgramadosService {

    private static final List<EstadoReserva> ACTIVAS = List.of(EstadoReserva.CONFIRMADA, EstadoReserva.PENDIENTE);
    private static final List<EstadoReserva> CON_SALIDA = List.of(EstadoReserva.CONFIRMADA, EstadoReserva.FINALIZADA);

    private final ReservaRepository reservaRepository;
    private final ComprobanteReservaRepository comprobanteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ReporteService reporteService;
    private final AlertaTarifaService alertaTarifaService;
    private final NotificacionService notificaciones;
    private final CorreoService correo;
    private final AuditoriaService auditoria;

    @Value("${notificaciones.nombre-hotel:Hotel Reservas}")
    private String hotel;

    /**
     * Recordatorio a los huéspedes que llegan mañana. Si no se fuerza, no repite el aviso a una reserva
     * a la que ya se le envió en las últimas 20 horas (p. ej. si el servidor se reinicia).
     *
     * @return cuántos recordatorios se procesaron
     */
    public int recordatoriosDeLlegada(boolean forzar) {
        LocalDate manana = ValidadorFechas.hoy().plusDays(1);
        int enviados = 0;
        for (Reserva r : reservaRepository.entrantesEl(manana, ACTIVAS)) {
            if (!forzar && auditoria.avisoYaEnviado(EventoReserva.Tipo.RECORDATORIO.name(), r.getId(), 20)) {
                continue;
            }
            String codigo = comprobanteRepository.findByReservaId(r.getId())
                    .map(c -> c.getNumeroComprobante()).orElse(null);
            notificaciones.notificar(new EventoReserva(EventoReserva.Tipo.RECORDATORIO, r.getId(),
                    r.getHuesped().getNombres(), r.getHuesped().getTelefono(), r.getHuesped().getCorreo(),
                    r.getHabitacion().getNumero(), r.getFechaEntrada(), r.getFechaSalida(), r.getValorTotal(), codigo));
            enviados++;
        }
        log.info("Recordatorios de llegada procesados para {}: {}", manana, enviados);
        return enviados;
    }

    /** Envía el resumen del día por correo a los administradores activos. Devuelve cuántos correos se enviaron. */
    public int resumenDiario() {
        String texto = construirResumen();
        List<Usuario> destinatarios = usuarioRepository.findByActivoTrueOrderByNombreAsc().stream()
                .filter(u -> u.getRol() == Rol.ADMINISTRADOR || u.getRol() == Rol.ADMIN_GENERAL)
                .toList();
        for (Usuario u : destinatarios) {
            correo.enviar(u.getCorreo(), "Resumen del día - " + hotel + " - " + ValidadorFechas.hoy(),
                    "Hola " + u.getNombre() + ",\n\n" + texto);
        }
        auditoria.registrarComo(null, null, "SISTEMA", "RESUMEN_DIARIO", "Sistema", null,
                "Enviado a " + destinatarios.size() + " administrador(es)");
        return destinatarios.size();
    }

    String construirResumen() {
        LocalDate hoy = ValidadorFechas.hoy();
        ReporteOcupacion ocupacion = reporteService.ocupacion(hoy, hoy.plusDays(1), null);
        List<Reserva> llegadas = reservaRepository.entrantesEl(hoy, ACTIVAS);
        List<Reserva> salidas = reservaRepository.salientesEl(hoy, CON_SALIDA);
        int llegadasManana = reservaRepository.entrantesEl(hoy.plusDays(1), ACTIVAS).size();
        List<AlertaTarifa> alertas = alertaTarifaService.calcular();

        StringBuilder sb = new StringBuilder();
        sb.append("Resumen de ").append(hotel).append(" - ").append(hoy).append("\n\n");
        sb.append("Ocupación de hoy: ").append(ocupacion.porcentajeOcupacion()).append("% (")
                .append(ocupacion.nochesOcupadas()).append(" de ").append(ocupacion.nochesDisponibles())
                .append(" habitaciones).\n");
        sb.append("Llegadas hoy (").append(llegadas.size()).append("): ").append(lista(llegadas)).append("\n");
        sb.append("Salidas hoy (").append(salidas.size()).append("): ").append(lista(salidas)).append("\n");
        sb.append("Llegadas mañana: ").append(llegadasManana).append("\n");
        if (!alertas.isEmpty()) {
            sb.append("\nATENCIÓN - tarifas por vencer:\n");
            for (AlertaTarifa a : alertas) {
                sb.append("- ").append(a.tipo()).append(a.sinTarifaHoy()
                        ? ": ya no tiene tarifa vigente hoy."
                        : ": sin tarifa desde el " + a.primeraFechaSinTarifa() + " (en " + a.diasRestantes() + " días).")
                        .append("\n");
            }
            sb.append("Sin tarifa no se pueden crear reservas en esas fechas. Actualízala en Administración > Tarifas.\n");
        }
        return sb.toString();
    }

    private static String lista(List<Reserva> reservas) {
        return reservas.isEmpty() ? "ninguna"
                : reservas.stream().map(r -> r.getHuesped().getNombres() + " (hab. " + r.getHabitacion().getNumero() + ")")
                        .collect(Collectors.joining(", "));
    }
}
