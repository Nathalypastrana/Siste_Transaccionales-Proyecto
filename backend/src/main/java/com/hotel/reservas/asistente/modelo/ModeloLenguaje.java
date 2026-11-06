package com.hotel.reservas.asistente.modelo;

import java.util.List;

/**
 * Contrato con el modelo de lenguaje. El resto del sistema solo conoce esta interfaz:
 * cambiar de proveedor (Claude, otro LLM) implica crear otra clase que la implemente,
 * sin tocar el webhook, las herramientas ni la API REST.
 */
public interface ModeloLenguaje {

    /**
     * Conversa con el modelo. Si este solicita herramientas, las ejecuta y le devuelve
     * los resultados hasta obtener la respuesta final en texto.
     *
     * @param sistema      instrucciones y contexto del huésped
     * @param historial    turnos anteriores de la conversación
     * @param mensaje      mensaje nuevo del huésped
     * @param herramientas herramientas disponibles para esta conversación
     */
    String conversar(String sistema, List<Turno> historial, String mensaje, List<Herramienta> herramientas);

    /** Indica si el proveedor está configurado (por ejemplo, si hay clave de API). */
    boolean configurado();
}
