package com.hotel.reservas.controller;

import com.hotel.reservas.config.DatabaseConnection;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Estado de la conexión con la base de datos (200 si responde, 503 si no). */
@RestController
@RequestMapping("/api/salud")
@RequiredArgsConstructor
public class SaludController {

    private final DatabaseConnection databaseConnection;

    @GetMapping
    public ResponseEntity<Map<String, String>> estado() {
        try {
            return ResponseEntity.ok(databaseConnection.verificar());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "estado", "SIN_CONEXION",
                    "detail", "No se pudo conectar con la base de datos."));
        }
    }
}
