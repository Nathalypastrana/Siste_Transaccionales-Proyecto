package com.hotel.reservas.whatsapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Envío de mensajes de WhatsApp. Proveedor según WHATSAPP_PROVEEDOR:
 *  - meta   (por defecto): WhatsApp Business Platform, Cloud API de Meta.
 *  - twilio: API de WhatsApp de Twilio (por ejemplo, su Sandbox, que no exige verificar el negocio ante Meta).
 * El resto del sistema (notificaciones, consultor, recepción) usa esta clase y no depende del proveedor.
 */
@Slf4j
@Component
public class WhatsAppClient {

    private static final int LIMITE_TEXTO = 4000; // WhatsApp admite hasta 4096 caracteres

    private final ObjectMapper mapper;
    private final RestClient http;
    private final String accessToken;
    private final String phoneNumberId;
    private final TwilioWhatsApp twilio;
    private final boolean usaTwilio;

    public WhatsAppClient(ObjectMapper mapper,
                          TwilioWhatsApp twilio,
                          @Value("${whatsapp.proveedor:meta}") String proveedor,
                          @Value("${whatsapp.access-token:}") String accessToken,
                          @Value("${whatsapp.phone-number-id:}") String phoneNumberId,
                          @Value("${whatsapp.graph-url:https://graph.facebook.com/v21.0}") String graphUrl) {
        this.mapper = mapper;
        this.accessToken = accessToken;
        this.phoneNumberId = phoneNumberId;
        this.twilio = twilio;
        this.usaTwilio = "twilio".equalsIgnoreCase(proveedor.trim());

        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(5_000);
        fabrica.setReadTimeout(15_000);
        this.http = RestClient.builder().requestFactory(fabrica).baseUrl(graphUrl).build();
    }

    public boolean configurado() {
        return usaTwilio ? twilio.configurado() : !accessToken.isBlank() && !phoneNumberId.isBlank();
    }

    /** Nombre del proveedor en uso, para la pantalla Sistema y los registros. */
    public String proveedor() {
        return usaTwilio ? "twilio" : "meta";
    }

    /** Envía un mensaje de texto. Si WhatsApp no está configurado, solo deja registro en el log. */
    public void enviar(String destino, String texto) {
        if (usaTwilio) {
            twilio.enviar(destino, texto);
            return;
        }
        if (!configurado()) {
            log.warn("WhatsApp no está configurado; mensaje para {} no enviado.", com.hotel.reservas.util.Telefonos.enmascarar(destino));
            return;
        }
        String contenido = texto.length() > LIMITE_TEXTO ? texto.substring(0, LIMITE_TEXTO) : texto;

        ObjectNode cuerpo = mapper.createObjectNode();
        cuerpo.put("messaging_product", "whatsapp");
        cuerpo.put("to", destino);
        cuerpo.put("type", "text");
        cuerpo.putObject("text").put("body", contenido);

        http.post()
                .uri("/{id}/messages", phoneNumberId)
                .header("Authorization", "Bearer " + accessToken)
                .header("content-type", "application/json")
                .body(cuerpo)
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Envía un mensaje de plantilla aprobada (necesario para escribirle a un cliente que no ha escrito en las
     * últimas 24 horas). Los parámetros reemplazan {{1}}, {{2}}... del cuerpo. Lanza excepción si Meta rechaza.
     */
    public void enviarPlantilla(String destino, String plantilla, String idioma, java.util.List<String> parametros) {
        if (usaTwilio) {
            if (TwilioWhatsApp.esContentSid(plantilla)) { // plantilla de contenido de Twilio (HX...)
                twilio.enviarContenido(destino, plantilla, parametros);
                return;
            }
            // Sin ContentSid se envía el mismo contenido como texto libre (válido dentro de la ventana de 24 h).
            String nombre = parametros.isEmpty() ? "" : parametros.get(0);
            String resumen = parametros.size() > 1 ? parametros.get(1) : "";
            twilio.enviar(destino, "Hola " + nombre + ". Novedad de tu reserva: " + resumen + " Gracias por elegirnos.");
            return;
        }
        if (!configurado()) {
            log.warn("WhatsApp no está configurado; plantilla para {} no enviada.", com.hotel.reservas.util.Telefonos.enmascarar(destino));
            return;
        }
        ObjectNode cuerpo = mapper.createObjectNode();
        cuerpo.put("messaging_product", "whatsapp");
        cuerpo.put("to", destino);
        cuerpo.put("type", "template");
        ObjectNode t = cuerpo.putObject("template");
        t.put("name", plantilla);
        t.putObject("language").put("code", idioma);
        var cuerpoParametros = t.putArray("components").addObject().put("type", "body").putArray("parameters");
        for (String p : parametros) {
            cuerpoParametros.addObject().put("type", "text").put("text", p);
        }

        http.post()
                .uri("/{id}/messages", phoneNumberId)
                .header("Authorization", "Bearer " + accessToken)
                .header("content-type", "application/json")
                .body(cuerpo)
                .retrieve()
                .toBodilessEntity();
    }
}
