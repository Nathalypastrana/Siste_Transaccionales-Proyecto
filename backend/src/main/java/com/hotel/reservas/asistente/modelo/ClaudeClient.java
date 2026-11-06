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

import java.util.List;

/**
 * Implementación de ModeloLenguaje con la API de Claude (Messages API + tool use).
 * Documentación: https://docs.claude.com/en/api/messages
 * Es el proveedor por defecto; se activa con asistente.proveedor=claude (o sin definirlo).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "asistente.proveedor", havingValue = "claude", matchIfMissing = true)
public class ClaudeClient implements ModeloLenguaje {

    private static final int MAX_VUELTAS = 5;

    private final ObjectMapper mapper;
    private final RestClient http;
    private final String apiKey;
    private final String modelo;
    private final int maxTokens;

    public ClaudeClient(ObjectMapper mapper,
                        @Value("${anthropic.api-key:}") String apiKey,
                        @Value("${anthropic.base-url:https://api.anthropic.com}") String baseUrl,
                        @Value("${anthropic.modelo:claude-haiku-5-5}") String modelo,
                        @Value("${asistente.max-tokens:1024}") int maxTokens) {
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
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
    }

    @Override
    public boolean configurado() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String conversar(String sistema, List<Turno> historial, String mensaje, List<Herramienta> herramientas) {
        ObjectNode cuerpo = mapper.createObjectNode();
        cuerpo.put("model", modelo);
        cuerpo.put("max_tokens", maxTokens);
        cuerpo.put("system", sistema);

        ArrayNode tools = cuerpo.putArray("tools");
        for (Herramienta h : herramientas) {
            ObjectNode t = tools.addObject();
            t.put("name", h.nombre());
            t.put("description", h.descripcion());
            try {
                t.set("input_schema", mapper.readTree(h.esquemaJson()));
            } catch (Exception e) {
                throw new IllegalStateException("Esquema inválido en la herramienta " + h.nombre(), e);
            }
        }

        ArrayNode mensajes = cuerpo.putArray("messages");
        for (Turno t : historial) {
            ObjectNode m = mensajes.addObject();
            m.put("role", t.rol());
            m.put("content", t.texto());
        }
        ObjectNode nuevo = mensajes.addObject();
        nuevo.put("role", "user");
        nuevo.put("content", mensaje);

        for (int vuelta = 0; vuelta < MAX_VUELTAS; vuelta++) {
            JsonNode respuesta = llamar(cuerpo);
            JsonNode contenido = respuesta.path("content");

            if (!"tool_use".equals(respuesta.path("stop_reason").asText())) {
                return textoDe(contenido);
            }

            // El modelo pidió herramientas: se agrega su turno y los resultados, y se vuelve a llamar.
            ObjectNode turnoModelo = mensajes.addObject();
            turnoModelo.put("role", "assistant");
            JsonNode copia = contenido.deepCopy();
            turnoModelo.set("content", copia);

            ArrayNode resultados = mapper.createArrayNode();
            for (JsonNode bloque : contenido) {
                if (!"tool_use".equals(bloque.path("type").asText())) {
                    continue;
                }
                resultados.add(ejecutar(bloque, herramientas));
            }
            ObjectNode turnoResultados = mensajes.addObject();
            turnoResultados.put("role", "user");
            turnoResultados.set("content", resultados);
        }
        throw new IllegalStateException("El modelo pidió herramientas más de " + MAX_VUELTAS + " veces seguidas.");
    }

    private ObjectNode ejecutar(JsonNode bloque, List<Herramienta> herramientas) {
        String nombre = bloque.path("name").asText();
        ObjectNode resultado = mapper.createObjectNode();
        resultado.put("type", "tool_result");
        resultado.put("tool_use_id", bloque.path("id").asText());

        Herramienta herramienta = herramientas.stream()
                .filter(h -> h.nombre().equals(nombre))
                .findFirst()
                .orElse(null);
        if (herramienta == null) {
            resultado.put("content", "Herramienta desconocida: " + nombre);
            resultado.put("is_error", true);
            return resultado;
        }

        try {
            log.info("Herramienta solicitada por el modelo: {}", nombre);
            resultado.put("content", herramienta.ejecutor().apply(bloque.path("input")));
        } catch (Exception e) {
            log.warn("La herramienta {} falló: {}", nombre, e.getMessage());
            resultado.put("content", "Error: " + e.getMessage());
            resultado.put("is_error", true);
        }
        return resultado;
    }

    private JsonNode llamar(ObjectNode cuerpo) {
        return http.post()
                .uri("/v1/messages")
                .header("content-type", "application/json")
                .body(cuerpo)
                .retrieve()
                .body(JsonNode.class);
    }

    private String textoDe(JsonNode contenido) {
        StringBuilder sb = new StringBuilder();
        for (JsonNode bloque : contenido) {
            if ("text".equals(bloque.path("type").asText())) {
                sb.append(bloque.path("text").asText());
            }
        }
        return sb.toString().trim();
    }
}
