package com.hotel.reservas.security;

import com.hotel.reservas.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Genera y valida los tokens JWT (firmados con HMAC-SHA256).
 * El secreto viene de la variable JWT_SECRET (mínimo 32 caracteres). Si no se define, se genera uno
 * aleatorio al arrancar: funciona, pero los tokens dejan de valer al reiniciar el backend.
 */
@Slf4j
@Component
public class JwtService {

    private final SecretKey clave;
    private final Duration vigencia;

    public JwtService(@Value("${seguridad.jwt-secreto:}") String secreto,
                      @Value("${seguridad.jwt-minutos:120}") long minutos) {
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < 32) {
            log.warn("JWT_SECRET no definido o menor de 32 caracteres: se usa un secreto temporal. "
                    + "Los inicios de sesión se pierden al reiniciar el backend.");
            this.clave = Jwts.SIG.HS256.key().build();
        } else {
            this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        }
        this.vigencia = Duration.ofMinutes(minutos);
    }

    public String generar(Usuario u) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(u.getId()))
                .claim("correo", u.getCorreo())
                .claim("rol", u.getRol().name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(vigencia)))
                .signWith(clave)
                .compact();
    }

    public long vigenciaSegundos() {
        return vigencia.toSeconds();
    }

    /** Devuelve el id del usuario si el token es válido y no ha vencido. */
    public Optional<Integer> validar(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
            return Optional.of(Integer.valueOf(c.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
