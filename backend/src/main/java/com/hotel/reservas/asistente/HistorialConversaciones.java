package com.hotel.reservas.asistente;

import com.hotel.reservas.asistente.modelo.Turno;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Memoria de corto plazo de cada conversación (en memoria, por número de teléfono) y límite de
 * mensajes por hora para controlar el costo de la API del modelo.
 * Solo se guardan los textos finales de cada turno (pregunta y respuesta), no los resultados de herramientas.
 */
@Component
public class HistorialConversaciones {

    private static final int MAX_TURNOS = 10;
    private static final Duration CADUCIDAD = Duration.ofMinutes(30);

    private record Intercambio(Instant momento, String pregunta, String respuesta) {
    }

    private final Map<String, Deque<Intercambio>> conversaciones = new HashMap<>();
    private final Map<String, Deque<Instant>> recientes = new HashMap<>();
    private final int maxMensajesPorHora;

    public HistorialConversaciones(@Value("${asistente.max-mensajes-por-hora:20}") int maxMensajesPorHora) {
        this.maxMensajesPorHora = maxMensajesPorHora;
    }

    /** Registra un mensaje entrante; devuelve false si el número superó su límite por hora. */
    public synchronized boolean permitir(String telefono) {
        Instant ahora = Instant.now();
        Deque<Instant> marcas = recientes.computeIfAbsent(telefono, k -> new ArrayDeque<>());
        while (!marcas.isEmpty() && marcas.peekFirst().isBefore(ahora.minus(Duration.ofHours(1)))) {
            marcas.pollFirst();
        }
        if (marcas.size() >= maxMensajesPorHora) {
            return false;
        }
        marcas.addLast(ahora);
        return true;
    }

    public synchronized List<Turno> obtener(String telefono) {
        Deque<Intercambio> cola = conversaciones.get(telefono);
        List<Turno> turnos = new ArrayList<>();
        if (cola == null) {
            return turnos;
        }
        Instant limite = Instant.now().minus(CADUCIDAD);
        cola.removeIf(i -> i.momento().isBefore(limite));
        for (Intercambio i : cola) {
            turnos.add(new Turno("user", i.pregunta()));
            turnos.add(new Turno("assistant", i.respuesta()));
        }
        return turnos;
    }

    public synchronized void agregar(String telefono, String pregunta, String respuesta) {
        Deque<Intercambio> cola = conversaciones.computeIfAbsent(telefono, k -> new ArrayDeque<>());
        cola.addLast(new Intercambio(Instant.now(), pregunta, respuesta));
        while (cola.size() > MAX_TURNOS) {
            cola.pollFirst();
        }
    }
}
