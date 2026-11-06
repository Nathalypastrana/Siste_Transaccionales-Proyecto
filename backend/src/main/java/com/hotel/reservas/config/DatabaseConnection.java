package com.hotel.reservas.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Módulo de conexión a la base de datos.
 *
 * - Los parámetros (URL, usuario, clave, tiempo de espera) se definen en
 *   application.properties y se pueden sobrescribir con variables de entorno
 *   DB_URL, DB_USER y DB_PASSWORD.
 * - Al arrancar la aplicación verifica la conexión y deja el resultado en el log.
 * - verificar() también lo usa el endpoint GET /api/salud, que la interfaz consulta
 *   para mostrar el estado de la conexión.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseConnection implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    /** Consulta la base; lanza excepción si no hay conexión. */
    public Map<String, String> verificar() {
        String base = jdbcTemplate.queryForObject("SELECT DB_NAME()", String.class);
        String version = jdbcTemplate.queryForObject(
                "SELECT CAST(SERVERPROPERTY('ProductVersion') AS VARCHAR(50))", String.class);
        return Map.of(
                "estado", "OK",
                "baseDatos", String.valueOf(base),
                "versionServidor", String.valueOf(version));
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("Conexión a la base de datos verificada: {}", verificar());
        } catch (Exception e) {
            log.error("No se pudo verificar la conexión a la base de datos: {}", e.getMessage());
        }
    }
}
