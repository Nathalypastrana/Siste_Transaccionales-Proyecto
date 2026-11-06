package com.hotel.reservas.asistente.modelo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de ModeloLenguaje con la API de Gemini (Google AI Studio), con llamada a funciones.
 * Se activa con asistente.proveedor=gemini (variable ASISTENTE_PROVEEDOR=gemini).
 * Documentación: https://ai.google.dev/gemini-api/docs/function-calling
 *
 * Importante: en los modelos con razonamiento, el turno del modelo que contiene la llamada a una función
 * puede traer "firmas" (thoughtSignature) que hay que devolver intactas; por eso ese turno se reenvía
 * tal como llegó, sin reconstruirlo.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "asistente.proveedor", havingValue = "gemini")
public class GeminiClient implements ModeloLenguaje {

    private static final int MAX_VUELTAS = 5;

    private final ObjectMapper mapper;
    private final RestClient http;
    private final String apiKey;
    private final String modelo;
    private final int maxTokens;

    public GeminiClient(ObjectMapper mapper,
                        @Value("${gemini.api-key:}") String apiKey,
                        @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
                        @Value("${gemini.modelo:gemini-3.5-flash-lite}") String modelo,
                        @Value("${gemini.max-tokens:4096}") int maxTokens) {
        this.mapper = mapper;
        this.apiKey = apiKey;
        this.modelo = modelo;
        this.maxTokens = maxTokens;

        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(5_000);
        fabrica.setReadTimeout(60_000);
        this.http = RestClient.builder()
                .requestFactory(fabrica)
                .baseUrl(baseUrl)
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
    }

    @Override
    public boolean configurado() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String conversar(String sistema, List<Turno> historial, String mensaje, List<Herramienta> herramientas) {
        ObjectNode cuerpo = mapper.createObjectNode();
        cuerpo.putObject("system_instruction").putArray("parts").addObject().put("text", sistema);

        ArrayNode declaraciones = cuerpo.putArray("tools").addObject().putArray("functionDeclarations");
        for (Herramienta h : herramientas) {
            ObjectNode d = declaraciones.addObject();
            d.put("name", h.nombre());
            d.put("description", h.descripcion());
            try {
                JsonNode esquema = mapper.readTree(h.esquemaJson());
                // Gemini rechaza un objeto de parámetros sin propiedades: si no hay parámetros, se omite.
                if (esquema.path("properties").size() > 0) {
                    d.set("parameters", esquema);
                }
            } catch (Exception e) {
                throw new IllegalStateException("Esquema inválido en la herramienta " + h.nombre(), e);
            }
        }

        cuerpo.putObject("generationConfig").put("maxOutputTokens", maxTokens);

        ArrayNode contenidos = cuerpo.putArray("contents");
        for (Turno t : historial) {
            agregarTexto(contenidos, "assistant".equals(t.rol()) ? "model" : "user", t.texto());
        }
        agregarTexto(contenidos, "user", mensaje);

        for (int vuelta = 0; vuelta < MAX_VUELTAS; vuelta++) {
            JsonNode respuesta = llamar(cuerpo);
            JsonNode turnoModelo = respuesta.path("candidates").path(0).path("content");
            JsonNode partes = turnoModelo.path("parts");

            List<JsonNode> llamadas = new ArrayList<>();
            for (JsonNode parte : partes) {
                if (parte.has("functionCall")) {
                    llamadas.add(parte.get("functionCall"));
                }
            }
            if (llamadas.isEmpty()) {
                return textoDe(partes);
            }

            // El modelo pidió funciones: se reenvía su turno intacto y luego los resultados.
            JsonNode copia = turnoModelo.deepCopy();
            if (copia instanceof ObjectNode objeto && !objeto.has("role")) {
                objeto.put("role", "model");
            }
            contenidos.add(copia);

            ObjectNode turnoResultados = contenidos.addObject();
            turnoResultados.put("role", "user");
            ArrayNode partesResultado = turnoResultados.putArray("parts");
            for (JsonNode llamada : llamadas) {
                partesResultado.add(ejecutar(llamada, herramientas));
            }
        }
        throw new IllegalStateException("El modelo pidió funciones más de " + MAX_VUELTAS + " veces seguidas.");
    }

    private void agregarTexto(ArrayNode contenidos, String rol, String texto) {
        ObjectNode c = contenidos.addObject();
        c.put("role", rol);
        c.putArray("parts").addObject().put("text", texto);
    }

    private ObjectNode ejecutar(JsonNode llamada, List<Herramienta> herramientas) {
        String nombre = llamada.path("name").asText();
        ObjectNode parte = mapper.createObjectNode();
        ObjectNode funcionRespuesta = parte.putObject("functionResponse");
        funcionRespuesta.put("name", nombre);

        Herramienta herramienta = herramientas.stream()
                .filter(h -> h.nombre().equals(nombre))
                .findFirst()
                .orElse(null);
        if (herramienta == null) {
            funcionRespuesta.set("response", error("Herramienta desconocida: " + nombre));
            return parte;
        }

        try {
            log.info("Función solicitada por el modelo: {}", nombre);
            String resultado = herramienta.ejecutor().apply(llamada.path("args"));
            funcionRespuesta.set("response", comoObjeto(resultado));
        } catch (Exception e) {
            log.warn("La herramienta {} falló: {}", nombre, e.getMessage());
            funcionRespuesta.set("response", error(e.getMessage()));
        }
        return parte;
    }

    /** Gemini exige que la respuesta de una función sea un objeto JSON. */
    private JsonNode comoObjeto(String texto) {
        try {
            JsonNode nodo = mapper.readTree(texto);
            if (nodo.isObject()) {
                return nodo;
            }
        } catch (Exception ignorada) {
            // no era JSON: se envuelve como texto
        }
        ObjectNode envuelto = mapper.createObjectNode();
        envuelto.put("resultado", texto);
        return envuelto;
    }

    private ObjectNode error(String mensaje) {
        ObjectNode e = mapper.createObjectNode();
        e.put("error", String.valueOf(mensaje));
        return e;
    }

    private JsonNode llamar(ObjectNode cuerpo) {
        try {
            return http.post()
                    .uri("/models/{modelo}:generateContent", modelo)
                    .header("content-type", "application/json")
                    .body(cuerpo)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (org.springframework.web.client.RestClientResponseException e) {
            // Muestra la respuesta de Google (sin la clave) para poder diagnosticar
            throw new IllegalStateException("Gemini respondió HTTP " + e.getStatusCode().value()
                    + " con el modelo '" + modelo + "': " + e.getResponseBodyAsString(), e);
        }
    }

    private String textoDe(JsonNode partes) {
        StringBuilder sb = new StringBuilder();
        for (JsonNode parte : partes) {
            if (parte.has("text") && !parte.path("thought").asBoolean(false)) {
                sb.append(parte.path("text").asText());
            }
        }
        return sb.toString().trim();
    }
}
