package com.hotel.reservas.service;

import com.hotel.reservas.model.RecuperacionClave;
import com.hotel.reservas.model.Usuario;
import com.hotel.reservas.repository.RecuperacionClaveRepository;
import com.hotel.reservas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Recuperación de contraseña por correo: enlace de un solo uso que vence a los 30 minutos.
 * Se guarda solo el hash del token. Máximo 3 solicitudes por hora por cuenta.
 */
@Service
@RequiredArgsConstructor
public class RecuperacionClaveService {

    private static final int MINUTOS_VIGENCIA = 30;
    private static final int MAX_SOLICITUDES_HORA = 3;
    private static final int MIN_PASSWORD = 8;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final RecuperacionClaveRepository repository;
    private final PasswordEncoder encoder;
    private final CorreoService correo;
    private final AuditoriaService auditoria;

    @Value("${app.url-frontend:http://localhost:5173}")
    private String urlFrontend;

    /** No revela si el correo existe: el resultado es el mismo en todos los casos. */
    @Transactional
    public void solicitar(String correoSolicitado) {
        String c = correoSolicitado.trim().toLowerCase();
        Optional<Usuario> encontrado = usuarioRepository.findByCorreo(c)
                .filter(u -> Boolean.TRUE.equals(u.getActivo()));
        if (encontrado.isEmpty()) {
            auditoria.registrarComo(null, c, null, "CLAVE_RECUPERACION_SOLICITADA", "Usuario", null,
                    "Correo no registrado o cuenta inactiva");
            return;
        }
        Usuario u = encontrado.get();
        if (repository.countByUsuarioIdAndCreadaAfter(u.getId(), LocalDateTime.now().minusHours(1)) >= MAX_SOLICITUDES_HORA) {
            auditoria.registrarComo(u.getId(), u.getCorreo(), u.getRol().name(), "CLAVE_RECUPERACION_SOLICITADA",
                    "Usuario", u.getId(), "Rechazada: demasiadas solicitudes en una hora");
            return;
        }
        repository.findByUsuarioIdAndUsadoFalse(u.getId()).forEach(r -> r.setUsado(true));

        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RecuperacionClave r = new RecuperacionClave();
        r.setUsuarioId(u.getId());
        r.setTokenHash(hash(token));
        r.setExpira(LocalDateTime.now().plusMinutes(MINUTOS_VIGENCIA));
        repository.save(r);

        String enlace = urlFrontend + "/restablecer?token=" + token;
        correo.enviar(u.getCorreo(), "Restablecer tu contraseña",
                "Hola " + u.getNombre() + ",\n\nPara crear una nueva contraseña abre este enlace (vence en "
                        + MINUTOS_VIGENCIA + " minutos y solo sirve una vez):\n\n" + enlace
                        + "\n\nSi no lo pediste tú, ignora este mensaje: tu contraseña no cambia.");
        auditoria.registrarComo(u.getId(), u.getCorreo(), u.getRol().name(), "CLAVE_RECUPERACION_SOLICITADA",
                "Usuario", u.getId(), "Enlace enviado");
    }

    @Transactional
    public void restablecer(String token, String nuevaClave) {
        if (nuevaClave.length() < MIN_PASSWORD) {
            throw new IllegalArgumentException("La contraseña debe tener al menos " + MIN_PASSWORD + " caracteres.");
        }
        RecuperacionClave r = repository.findByTokenHash(hash(token.trim()))
                .filter(x -> !Boolean.TRUE.equals(x.getUsado()) && x.getExpira().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "El enlace no es válido o ya venció. Solicita uno nuevo."));
        Usuario u = usuarioRepository.findById(r.getUsuarioId())
                .filter(x -> Boolean.TRUE.equals(x.getActivo()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "El enlace no es válido o ya venció. Solicita uno nuevo."));
        u.setPassword(encoder.encode(nuevaClave));
        r.setUsado(true);
        repository.findByUsuarioIdAndUsadoFalse(u.getId()).forEach(x -> x.setUsado(true));
        auditoria.registrarComo(u.getId(), u.getCorreo(), u.getRol().name(), "CLAVE_RESTABLECIDA", "Usuario",
                u.getId(), "Contraseña cambiada con enlace de recuperación");
    }

    private static String hash(String token) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
