package com.hotel.reservas.asistente;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hotel.reservas.asistente.modelo.Herramienta;
import com.hotel.reservas.dto.HabitacionResponse;
import com.hotel.reservas.dto.ReservaResponse;
import com.hotel.reservas.service.HabitacionService;
import com.hotel.reservas.service.ReservaService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Herramientas de SOLO LECTURA que el consultor puede usar. Reutilizan los mismos servicios
 * que atienden la API REST (HabitacionService, ReservaService), por lo que el modelo ve
 * exactamente los mismos datos y reglas que la interfaz web.
 *
 * Ninguna recibe el identificador del huésped como parámetro: el huésped sale del número de
 * WhatsApp que escribe (ContextoHuesped), de modo que no se puede consultar a otra persona.
 */
@Component
@RequiredArgsConstructor
public class HerramientasReservas {

    static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private final HabitacionService habitacionService;
    private final ReservaService reservaService;
    private final Recepcion recepcion;
    private final ObjectMapper mapper;

    @Value("${asistente.politica-cancelacion:Las reservas pendientes o confirmadas se pueden cancelar hasta antes de la fecha de entrada. Una vez iniciada la estadía no es posible cancelarla desde el sistema. Cualquier otra condición la define el recepcionista.}")
    private String politicaCancelacion;

    public List<Herramienta> para(ContextoHuesped ctx) {
        return List.of(disponibilidad(), misReservas(ctx), politica(), derivar(ctx));
    }

    private Herramienta disponibilidad() {
        return new Herramienta(
                "consultar_disponibilidad",
                "Consulta qué tipos de habitación están libres entre dos fechas, con su capacidad, la tarifa "
                        + "vigente por noche y el total estimado. Úsala para preguntas de disponibilidad y precios.",
                """
                {"type":"object","properties":{
                  "fecha_entrada":{"type":"string","description":"Fecha de entrada, formato AAAA-MM-DD"},
                  "fecha_salida":{"type":"string","description":"Fecha de salida, formato AAAA-MM-DD"},
                  "personas":{"type":"integer","description":"Número de personas (opcional, por defecto 1)"}
                },"required":["fecha_entrada","fecha_salida"]}
                """,
                args -> {
                    LocalDate entrada = LocalDate.parse(args.path("fecha_entrada").asText());
                    LocalDate salida = LocalDate.parse(args.path("fecha_salida").asText());
                    int personas = Math.max(1, args.path("personas").asInt(1));
                    if (entrada.isBefore(LocalDate.now(ZONA))) {
                        throw new IllegalArgumentException("La fecha de entrada no puede ser anterior a hoy.");
                    }
                    long noches = ChronoUnit.DAYS.between(entrada, salida);

                    List<HabitacionResponse> libres = habitacionService.disponibles(entrada, salida).stream()
                            .filter(h -> h.capacidad() >= personas && h.precioNoche() != null)
                            .toList();

                    Map<String, List<HabitacionResponse>> porTipo = new LinkedHashMap<>();
                    for (HabitacionResponse h : libres) {
                        porTipo.computeIfAbsent(h.tipo(), k -> new java.util.ArrayList<>()).add(h);
                    }

                    ObjectNode resultado = mapper.createObjectNode();
                    resultado.put("noches", noches);
                    resultado.put("personas", personas);
                    ArrayNode tipos = resultado.putArray("tipos_disponibles");
                    porTipo.forEach((tipo, hab) -> {
                        HabitacionResponse ref = hab.get(0);
                        BigDecimal total = ref.precioNoche().multiply(BigDecimal.valueOf(noches));
                        ObjectNode t = tipos.addObject();
                        t.put("tipo", tipo);
                        t.put("habitaciones_libres", hab.size());
                        t.put("capacidad_por_habitacion", ref.capacidad());
                        t.put("precio_por_noche_cop", ref.precioNoche());
                        t.put("total_estimado_cop", total);
                    });
                    if (tipos.isEmpty()) {
                        resultado.put("mensaje", "No hay habitaciones disponibles para esas fechas y esa cantidad de personas.");
                    }
                    return resultado.toString();
                });
    }

    private Herramienta misReservas(ContextoHuesped ctx) {
        return new Herramienta(
                "consultar_mis_reservas",
                "Consulta una reserva del huésped que está escribiendo (estado, fechas, habitación y total) a partir "
                        + "del código de la reserva (formato CR-XXXXXXXX, está en su comprobante). Sin código no se "
                        + "puede consultar: pídeselo al huésped. Úsala para preguntas sobre el estado de su reserva.",
                """
                {"type":"object","properties":{
                  "codigo_reserva":{"type":"string","description":"Código de la reserva, por ejemplo CR-AB12CD34"}
                },"required":["codigo_reserva"]}
                """,
                args -> {
                    String codigo = args.path("codigo_reserva").asText("").trim();
                    if (codigo.isEmpty()) {
                        return "{\"error\":\"Falta el código de la reserva. Pídeselo al huésped (aparece en su comprobante).\"}";
                    }
                    if (ctx.huesped() == null) {
                        return "{\"error\":\"No hay un huésped registrado con este número de WhatsApp, "
                                + "por lo que no se pueden consultar reservas.\"}";
                    }
                    // El código debe pertenecer a una reserva del huésped que escribe (identificado por su teléfono).
                    return reservaService.obtenerDeHuespedPorCodigo(ctx.huesped().id(), codigo)
                            .map(r -> {
                                ObjectNode o = mapper.createObjectNode();
                                o.put("habitacion", r.habitacionNumero());
                                o.put("fecha_entrada", r.fechaEntrada().toString());
                                o.put("fecha_salida", r.fechaSalida().toString());
                                o.put("estado", r.estado().name());
                                o.put("total_cop", r.valorTotal());
                                return o.toString();
                            })
                            .orElse("{\"error\":\"No se encontró una reserva con ese código asociada a este número.\"}");
                });
    }

    private Herramienta politica() {
        return new Herramienta(
                "consultar_politica_cancelacion",
                "Devuelve la política de cancelación vigente del hotel.",
                "{\"type\":\"object\",\"properties\":{}}",
                args -> politicaCancelacion);
    }

    private Herramienta derivar(ContextoHuesped ctx) {
        return new Herramienta(
                "derivar_a_recepcionista",
                "Pasa la conversación a un recepcionista humano. Úsala cuando no puedas resolver la consulta "
                        + "con las demás herramientas: cambios o cancelaciones de reserva, pagos, quejas, "
                        + "solicitudes especiales o cualquier tema que no cubran.",
                """
                {"type":"object","properties":{
                  "motivo":{"type":"string","description":"Resumen breve de lo que necesita el huésped"}
                },"required":["motivo"]}
                """,
                args -> {
                    recepcion.notificar(ctx.telefono(), ctx.nombreParaMostrar(), args.path("motivo").asText("sin motivo"));
                    return "{\"estado\":\"derivado\",\"mensaje\":\"Un recepcionista fue notificado y contactará al huésped.\"}";
                });
    }
}
