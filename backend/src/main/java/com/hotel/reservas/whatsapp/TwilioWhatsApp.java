package com.hotel.reservas.whatsapp;

import com.hotel.reservas.util.Telefonos;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * WhatsApp a través de Twilio (proveedor oficial de WhatsApp). Se usa con WHATSAPP_PROVEEDOR=twilio, por ejemplo con el
 * Sandbox de Twilio, que no exige verificar el negocio ante Meta.
 *
 * Envío:  POST https://api.twilio.com/2010-04-01/Accounts/{AccountSid}/Messages.json (autenticación básica SID:token).
 * Firma:  Twilio firma cada webhook con X-Twilio-Signature = Base64(HMAC-SHA1(authToken, URL + parámetros ordenados)).
 *         Documentación: https://www.twilio.com/docs/usage/security#validating-requests
 */
@Slf4j
@Component
public class TwilioWhatsApp {

    /** Ruta del webhook de mensajes entrantes y de los estados de entrega (deben coincidir con lo configurado en Twilio). */
    public static final String RUTA_MENSAJES = "/api/whatsapp/twilio";
    public static final String RUTA_ESTADOS = "/api/whatsapp/twilio/estado";

    private static final int LIMITE_TEXTO = 1600; // máximo de Twilio por mensaje
    private static final com.fasterxml.jackson.databind.ObjectMapper JSON = new com.fasterxml.jackson.databind.ObjectMapper();

    private final RestClient http;
    private final String accountSid;
    private final String authToken;
    private final String numeroOrigen;
    private final String urlPublica;

    public TwilioWhatsApp(@Value("${twilio.account-sid:}") String accountSid,
                          @Value("${twilio.auth-token:}") String authToken,
                          @Value("${twilio.whatsapp-from:+14155238886}") String numeroOrigen,
                          @Value("${twilio.url-publica:}") String urlPublica,
                          @Value("${twilio.api-url:https://api.twilio.com/2010-04-01}") String apiUrl) {
        this.accountSid = accountSid.trim();
        this.authToken = authToken.trim();
        this.numeroOrigen = numeroOrigen.replaceAll("[^\\d+]", "");
        this.urlPublica = urlPublica.trim().replaceAll("/+$", "");

        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(5_000);
        fabrica.setReadTimeout(15_000);
        this.http = RestClient.builder().requestFactory(fabrica).baseUrl(apiUrl).build();
    }

    public boolean configurado() {
        return !accountSid.isBlank() && !authToken.isBlank() && !numeroOrigen.isBlank();
    }

    /** Envía un texto libre. destino = indicativo + número, solo dígitos (573001112233). Lanza excepción si Twilio rechaza. */
    public void enviar(String destino, String texto) {
        if (!configurado()) {
            log.warn("Twilio no está configurado; mensaje para {} no enviado.", Telefonos.enmascarar(destino));
            return;
        }
        MultiValueMap<String, String> form = formulario(destino);
        form.add("Body", texto.length() > LIMITE_TEXTO ? texto.substring(0, LIMITE_TEXTO) : texto);
        publicar(form, destino);
    }

    /**
     * Envía una plantilla de contenido de Twilio (ContentSid HX...). Es lo que exige el número de prueba de Twilio y
     * WhatsApp fuera de la ventana de 24 h. Las variables reemplazan {{1}}, {{2}}... de la plantilla.
     */
    public void enviarContenido(String destino, String contentSid, List<String> variables) {
        if (!configurado()) {
            log.warn("Twilio no está configurado; plantilla para {} no enviada.", Telefonos.enmascarar(destino));
            return;
        }
        Map<String, String> valores = new java.util.LinkedHashMap<>();
        for (int i = 0; i < variables.size(); i++) {
            valores.put(String.valueOf(i + 1), variables.get(i));
        }
        MultiValueMap<String, String> form = formulario(destino);
        form.add("ContentSid", contentSid.trim());
        if (valores.isEmpty()) {
            publicar(form, destino);
            return;
        }
        MultiValueMap<String, String> conVariables = new LinkedMultiValueMap<>(form);
        try {
            conVariables.add("ContentVariables", JSON.writeValueAsString(valores));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudieron preparar las variables de la plantilla", e);
        }
        try {
            publicar(conVariables, destino);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Plantillas sin variables (como las de ejemplo del número de prueba) rechazan ContentVariables: se reintenta sin ellas.
            log.warn("Twilio rechazó la plantilla con variables ({}); se reintenta sin variables.", e.getStatusCode());
            publicar(form, destino);
        }
    }

    /** true si el valor tiene forma de ContentSid de Twilio (HX + 32 caracteres hexadecimales). */
    public static boolean esContentSid(String valor) {
        return valor != null && valor.trim().matches("HX[0-9a-fA-F]{32}");
    }

    private MultiValueMap<String, String> formulario(String destino) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("From", "whatsapp:" + (numeroOrigen.startsWith("+") ? numeroOrigen : "+" + numeroOrigen));
        form.add("To", "whatsapp:+" + destino.replaceAll("\\D", ""));
        if (!urlPublica.isBlank()) {
            form.add("StatusCallback", urlPublica + RUTA_ESTADOS); // Twilio informa sent / delivered / failed
        }
        return form;
    }

    private void publicar(MultiValueMap<String, String> form, String destino) {
        String credenciales = Base64.getEncoder()
                .encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
        http.post()
                .uri("/Accounts/{sid}/Messages.json", accountSid)
                .header("Authorization", "Basic " + credenciales)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
        log.info("WhatsApp (Twilio) enviado a {}", Telefonos.enmascarar(destino));
    }

    /**
     * Valida la firma X-Twilio-Signature de un webhook. Sin token o sin URL pública configurada se rechaza todo:
     * nunca se acepta un webhook sin firmar.
     */
    public boolean firmaValida(String ruta, Map<String, List<String>> parametros, String firma) {
        if (authToken.isBlank() || urlPublica.isBlank() || firma == null || firma.isBlank()) {
            return false;
        }
        StringBuilder datos = new StringBuilder(urlPublica + ruta);
        for (Map.Entry<String, List<String>> p : new TreeMap<>(parametros).entrySet()) {
            List<String> valores = new ArrayList<>(p.getValue());
            valores.sort(null);
            for (String v : valores) {
                datos.append(p.getKey()).append(v == null ? "" : v);
            }
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(authToken.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            byte[] esperado = mac.doFinal(datos.toString().getBytes(StandardCharsets.UTF_8));
            String esperadoB64 = Base64.getEncoder().encodeToString(esperado);
            return MessageDigest.isEqual(esperadoB64.getBytes(StandardCharsets.UTF_8),
                    firma.trim().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }
}
