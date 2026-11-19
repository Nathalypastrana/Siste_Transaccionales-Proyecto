package com.hotel.reservas.whatsapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Envío de mensajes por la WhatsApp Business Platform (Cloud API de Meta). */
@Slf4j
@Component
public class WhatsAppClient {

    private static final int LIMITE_TEXTO = 4000; // WhatsApp admite hasta 4096 caracteres

    private final ObjectMapper mapper;
    private final RestClient http;
    private final String accessToken;
    private final String phoneNumberId;

    public WhatsAppClient(ObjectMapper mapper,
                          @Value("${whatsapp.access-token:}") String accessToken,
                          @Value("${whatsapp.phone-number-id:}") String phoneNumberId,
                          @Value("${whatsapp.graph-url:https://graph.facebook.com/v21.0}") String graphUrl) {
        this.mapper = mapper;
        this.accessToken = accessToken;
        this.phoneNumberId = phoneNumberId;

        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(5_000);
        fabrica.setReadTimeout(15_000);
        this.http = RestClient.builder().requestFactory(fabrica).baseUrl(graphUrl).build();
    }

    public boolean configurado() {
        return !accessToken.isBlank() && !phoneNumberId.isBlank();
    }

    /** Envía un mensaje de texto. Si WhatsApp no está configurado, solo deja registro en el log. */
    public void enviar(String destino, String texto) {
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
