package com.hotel.reservas.whatsapp;

import com.hotel.reservas.asistente.AsistenteService;
import com.hotel.reservas.util.Telefonos;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Webhooks de WhatsApp vía Twilio (WHATSAPP_PROVEEDOR=twilio).
 *
 *  POST /api/whatsapp/twilio         -> mensaje entrante (webhook -> consultor -> respuesta por WhatsApp).
 *  POST /api/whatsapp/twilio/estado  -> estado de entrega de los mensajes enviados (queued, sent, delivered, read, failed).
 *
 * Solo se aceptan peticiones con la firma X-Twilio-Signature válida (HMAC-SHA1 con el Auth Token).
 * La respuesta del consultor se devuelve como TwiML en la misma petición (Twilio espera hasta 15 s).
 */
@Slf4j
@RestController
public class TwilioWebhookController {

    private static final String TWIML_VACIO = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response></Response>";
    /** Twilio espera la respuesta del webhook hasta 15 s; se deja margen. */
    private static final int ESPERA_MAXIMA_SEGUNDOS = 12;
    private static final int LIMITE_TEXTO = 1600;

    private final AsistenteService asistente;
    private final TwilioWhatsApp twilio;

    private final ExecutorService ejecutor = Executors.newFixedThreadPool(4);
    /** Twilio reintenta si no recibe respuesta a tiempo; este conjunto evita contestar dos veces el mismo mensaje. */
    private final Set<String> procesados = ConcurrentHashMap.newKeySet();

    public TwilioWebhookController(AsistenteService asistente, TwilioWhatsApp twilio) {
        this.asistente = asistente;
        this.twilio = twilio;
    }

    @PostMapping(path = TwilioWhatsApp.RUTA_MENSAJES, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> recibir(@RequestParam MultiValueMap<String, String> parametros,
                                          @RequestHeader(name = "X-Twilio-Signature", required = false) String firma) {
        if (!twilio.firmaValida(TwilioWhatsApp.RUTA_MENSAJES, parametros, firma)) {
            log.warn("Webhook de Twilio rechazado: firma ausente o inválida.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Firma inválida");
        }

        String id = parametros.getFirst("MessageSid");
        String de = valor(parametros.getFirst("From")).replaceAll("\\D", ""); // "whatsapp:+573001112233" -> "573001112233"
        String texto = valor(parametros.getFirst("Body"));
        String nombre = valor(parametros.getFirst("ProfileName"));

        if (id == null || de.isBlank() || !procesados.add(id)) {
            return twiml(null); // repetido o sin remitente: no se responde dos veces
        }
        if (procesados.size() > 5000) {
            procesados.clear();
        }

        // La respuesta va en el TwiML de este mismo webhook (respuesta dentro de la conversación). Así funciona también
        // con el número de prueba de Twilio, que no permite mensajes de texto libre iniciados por la API (error 21654).
        String respuesta;
        Future<String> tarea = ejecutor.submit(() -> atender(de, nombre, texto));
        try {
            respuesta = tarea.get(ESPERA_MAXIMA_SEGUNDOS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            tarea.cancel(true);
            log.warn("El consultor tardó más de {} s en responder a {}.", ESPERA_MAXIMA_SEGUNDOS, Telefonos.enmascarar(de));
            respuesta = "Estamos revisando tu consulta. Si no recibes respuesta pronto, un recepcionista te contactará.";
        } catch (Exception e) {
            log.error("Error al atender el mensaje de {}: {}", Telefonos.enmascarar(de), e.getMessage(), e);
            respuesta = "En este momento no puedo responder. Un recepcionista te contactará.";
        }
        return twiml(respuesta);
    }

    @PostMapping(path = TwilioWhatsApp.RUTA_ESTADOS, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> estado(@RequestParam MultiValueMap<String, String> parametros,
                                         @RequestHeader(name = "X-Twilio-Signature", required = false) String firma) {
        if (!twilio.firmaValida(TwilioWhatsApp.RUTA_ESTADOS, parametros, firma)) {
            log.warn("Estado de Twilio rechazado: firma ausente o inválida.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Firma inválida");
        }
        String destino = Telefonos.enmascarar(parametros.getFirst("To"));
        String estado = valor(parametros.getFirst("MessageStatus"));
        String codigo = parametros.getFirst("ErrorCode");
        if ("failed".equals(estado) || "undelivered".equals(estado)) {
            log.warn("WhatsApp (Twilio): mensaje a {} NO entregado: estado {} (código {}).", destino, estado, codigo);
        } else {
            log.info("WhatsApp (Twilio): mensaje a {} -> {}", destino, estado);
        }
        return ResponseEntity.ok().build();
    }

    private String atender(String de, String nombre, String texto) {
        return texto.isBlank()
                ? "Por ahora solo puedo leer mensajes de texto. ¿En qué puedo ayudarte con tu reserva?"
                : asistente.responder(de, nombre.isBlank() ? null : nombre, texto);
    }

    /** Respuesta TwiML: con mensaje, Twilio se lo envía al huésped; sin mensaje, no envía nada. */
    private static ResponseEntity<String> twiml(String mensaje) {
        String cuerpo = (mensaje == null || mensaje.isBlank())
                ? TWIML_VACIO
                : "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response><Message>"
                  + escaparXml(mensaje.length() > LIMITE_TEXTO ? mensaje.substring(0, LIMITE_TEXTO) : mensaje)
                  + "</Message></Response>";
        return ResponseEntity.ok().contentType(new MediaType("text", "xml", StandardCharsets.UTF_8)).body(cuerpo);
    }

    private static String escaparXml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static String valor(String s) {
        return s == null ? "" : s.trim();
    }

    @PreDestroy
    void cerrar() {
        ejecutor.shutdown();
    }
}
