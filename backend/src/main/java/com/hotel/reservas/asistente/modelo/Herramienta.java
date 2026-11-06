package com.hotel.reservas.asistente.modelo;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.function.Function;

/**
 * Herramienta que el modelo puede solicitar. La define la aplicación: el modelo
 * solo pide ejecutarla con parámetros estructurados y nunca accede a la base de datos.
 *
 * @param nombre      identificador de la herramienta
 * @param descripcion cuándo usarla (el modelo lo lee para decidir)
 * @param esquemaJson JSON Schema de los parámetros de entrada
 * @param ejecutor    ejecuta la herramienta con los parámetros y devuelve el resultado como texto
 */
public record Herramienta(
        String nombre,
        String descripcion,
        String esquemaJson,
        Function<JsonNode, String> ejecutor
) {
}
