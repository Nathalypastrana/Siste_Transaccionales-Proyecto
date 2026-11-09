package com.hotel.reservas.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.reservas.asistente.AsistenteService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Webhook de la WhatsApp Business Platform (Cloud API).
 *
 *  GET  /api/whatsapp/webhook  -> verificación inicial que hace Meta al registrar la URL.
 *  POST /api/whatsapp/webhook  -> mensajes entrantes (flujo: webhook -> consultor -> respuesta por WhatsApp).
 *
 * El POST solo se acepta si trae la firma X-Hub-Signature-256 válida, calculada con el secreto de la app.
 * Meta exige responder 200 rápido, por eso el procesamiento (llamada al modelo) se hace en segundo plano.
 */
@Slf4j
@RestController
@RequestMapping("/api/whatsapp/webhook")
public class WhatsAppWebhookController {

    private final ObjectMapper mapper;
    private final AsistenteService asistente;
    private final WhatsAppClient whatsApp;
    private final String verifyToken;
    private final String appSecret;

    private final ExecutorService ejecutor = Executors.newFixedThreadPool(4);
    /** Meta reintenta si tarda en recibir el 200; este conjunto evita responder dos veces el mismo mensaje. */
    private final Set<String> procesados = ConcurrentHashMap.newKeySet();

    public WhatsAppWebhookController(ObjectMapper mapper,
                                     AsistenteService asistente,
                                     WhatsAppClient whatsApp,
                                     @Value("${whatsapp.verify-token:}") String verifyToken,
                                     @Value("${whatsapp.app-secret:}") String appSecret) {
        this.mapper = mapper;
        this.asistente = asistente;
        this.whatsApp = whatsApp;
        this.verifyToken = verifyToken;
        this.appSecret = appSecret;
    }

    @GetMapping
    public ResponseEntity<String> verificar(
            @RequestParam(name = "hub.mode", required = false) String modo,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String desafio) {
        boolean valido = "subscribe".equals(modo)
                && !verifyToken.isBlank()
                && token != null
                && MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), verifyToken.getBytes(StandardCharsets.UTF_8));
        if (!valido || desafio == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verificación rechazada");
        }
        return ResponseEntity.ok(desafio);
    }

    @PostMapping
    public ResponseEntity<String> recibir(
            @RequestBody byte[] cuerpo,
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String firma) {
        if (!firmaValida(cuerpo, firma)) {
            log.warn("Webhook de WhatsApp rechazado: firma ausente o inválida.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Firma inválida");
        }

        try {
            JsonNode raiz = mapper.readTree(cuerpo);
            for (JsonNode entrada : raiz.path("entry")) {
                for (JsonNode cambio : entrada.path("changes")) {
                    procesarValor(cambio.path("value"));
                }
            }
        } catch (Exception e) {
            log.error("No se pudo interpretar el webhook de WhatsApp: {}", e.getMessage());
        }
        return ResponseEntity.ok("EVENT_RECEIVED");
    }

    private void procesarValor(JsonNode valor) {
        Map<String, String> nombres = new HashMap<>();
        for (JsonNode contacto : valor.path("contacts")) {
            nombres.put(contacto.path("wa_id").asText(), contacto.path("profile").path("name").asText(""));
        }

        for (JsonNode mensaje : valor.path("messages")) {
            String id = mensaje.path("id").asText();
            String de = mensaje.path("from").asText();
            if (id.isBlank() || de.isBlank() || !procesados.add(id)) {
                continue;
            }
            if (procesados.size() > 5000) {
                procesados.clear();
            }

            String texto = "text".equals(mensaje.path("type").asText())
                    ? mensaje.path("text").path("body").asText("")
                    : null;
            String nombre = nombres.get(de);
            ejecutor.submit(() -> atender(de, nombre, texto));
        }
    }

    private void atender(String de, String nombre, String texto) {
        try {
            String respuesta = (texto == null || texto.isBlank())
                    ? "Por ahora solo puedo leer mensajes de texto. ¿En qué puedo ayudarte con tu reserva?"
                    : asistente.responder(de, nombre, texto);
            whatsApp.enviar(de, respuesta);
        } catch (Exception e) {
            log.error("Error al atender el mensaje de {}: {}", com.hotel.reservas.util.Telefonos.enmascarar(de), e.getMessage(), e);
        }
    }

    private boolean firmaValida(byte[] cuerpo, String firma) {
        if (appSecret.isBlank() || firma == null || !firma.startsWith("sha256=")) {
            return false; // sin secreto configurado se rechaza todo: nunca se acepta un webhook sin firmar
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String esperada = HexFormat.of().formatHex(mac.doFinal(cuerpo));
            return MessageDigest.isEqual(
                    esperada.getBytes(StandardCharsets.UTF_8),
                    firma.substring("sha256=".length()).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    @PreDestroy
    void cerrar() {
        ejecutor.shutdown();
    }
}
