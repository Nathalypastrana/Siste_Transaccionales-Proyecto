package com.hotel.reservas.service;

import com.hotel.reservas.dto.LoginRequest;
import com.hotel.reservas.dto.LoginResponse;
import com.hotel.reservas.dto.UsuarioResponse;
import com.hotel.reservas.exception.CredencialesInvalidasException;
import com.hotel.reservas.exception.DemasiadosIntentosException;
import com.hotel.reservas.model.Usuario;
import com.hotel.reservas.repository.UsuarioRepository;
import com.hotel.reservas.security.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Inicio de sesión (RF 01 / CU-01). */
@Slf4j
@Service
public class AuthService {

    private static final int MAX_FALLOS = 5;
    private static final Duration VENTANA = Duration.ofMinutes(15);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final AuditoriaService auditoria;
    private final boolean migrarClavesPlanas;
    private final String hashFicticio;
    private final Map<String, Deque<Instant>> fallos = new ConcurrentHashMap<>();

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder encoder, JwtService jwtService,
                       AuditoriaService auditoria,
                       @Value("${seguridad.migrar-claves-planas:true}") boolean migrarClavesPlanas) {
        this.usuarioRepository = usuarioRepository;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.auditoria = auditoria;
        this.migrarClavesPlanas = migrarClavesPlanas;
        this.hashFicticio = encoder.encode("clave-ficticia-para-igualar-tiempos");
    }

    /**
     * Valida correo y contraseña. Los mensajes de error no distinguen entre "usuario inexistente",
     * "contraseña incorrecta" y "cuenta inactiva", y siempre se calcula un hash para que el tiempo de
     * respuesta no revele si el correo existe. Tras 5 fallos en 15 minutos se bloquea ese correo un rato.
     */
    @Transactional
    public LoginResponse iniciarSesion(LoginRequest req) {
        String correo = req.correo().trim().toLowerCase();
        if (fallosRecientes(correo) >= MAX_FALLOS) {
            auditoria.registrarComo(null, correo, null, "LOGIN_BLOQUEADO", "Usuario", null,
                    "Demasiados intentos fallidos");
            throw new DemasiadosIntentosException();
        }
        Optional<Usuario> encontrado = usuarioRepository.findByCorreo(correo)
                .filter(u -> Boolean.TRUE.equals(u.getActivo()));

        boolean correcto = false;
        if (encontrado.isPresent()) {
            correcto = verificar(encontrado.get(), req.password());
        } else {
            encoder.matches(req.password(), hashFicticio);
        }
        if (!correcto) {
            registrarFallo(correo);
            auditoria.registrarComo(null, correo, null, "LOGIN_FALLIDO", "Usuario", null, "Credenciales inválidas");
            throw new CredencialesInvalidasException();
        }
        fallos.remove(correo);
        Usuario u = encontrado.get();
        auditoria.registrarComo(u.getId(), u.getCorreo(), u.getRol().name(), "LOGIN_OK", "Usuario", u.getId(), null);
        return new LoginResponse(jwtService.generar(u), jwtService.vigenciaSegundos(), UsuarioResponse.de(u));
    }

    /** Emite un token nuevo para la sesión vigente (la persona debe pedirlo antes de que venza el actual). */
    @Transactional
    public LoginResponse renovar(Integer usuarioId) {
        Usuario u = usuarioRepository.findById(usuarioId)
                .filter(x -> Boolean.TRUE.equals(x.getActivo()))
                .orElseThrow(CredencialesInvalidasException::new);
        auditoria.registrarComo(u.getId(), u.getCorreo(), u.getRol().name(), "SESION_RENOVADA", "Usuario", u.getId(), null);
        return new LoginResponse(jwtService.generar(u), jwtService.vigenciaSegundos(), UsuarioResponse.de(u));
    }

    private boolean verificar(Usuario u, String clave) {
        String guardada = u.getPassword();
        if (guardada != null && guardada.startsWith("$2")) {
            return encoder.matches(clave, guardada);
        }
        // Cuenta creada a mano con la clave en texto plano: se acepta una vez y se convierte a BCrypt.
        if (migrarClavesPlanas && guardada != null
                && MessageDigest.isEqual(guardada.getBytes(StandardCharsets.UTF_8), clave.getBytes(StandardCharsets.UTF_8))) {
            u.setPassword(encoder.encode(clave));
            usuarioRepository.save(u);
            log.warn("La clave del usuario {} estaba en texto plano y se convirtió a BCrypt.", u.getCorreo());
            return true;
        }
        return false;
    }

    private long fallosRecientes(String correo) {
        Deque<Instant> cola = fallos.get(correo);
        if (cola == null) {
            return 0;
        }
        synchronized (cola) {
            Instant limite = Instant.now().minus(VENTANA);
            while (!cola.isEmpty() && cola.peekFirst().isBefore(limite)) {
                cola.pollFirst();
            }
            return cola.size();
        }
    }

    private void registrarFallo(String correo) {
        Deque<Instant> cola = fallos.computeIfAbsent(correo, k -> new ArrayDeque<>());
        synchronized (cola) {
            cola.addLast(Instant.now());
        }
    }
}
