package com.hotel.reservas.config;

import com.hotel.reservas.model.Rol;
import com.hotel.reservas.model.Usuario;
import com.hotel.reservas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea el primer administrador general si la base no tiene ninguno activo, usando ADMIN_CORREO y ADMIN_PASSWORD.
 * Si ya existe un administrador activo no hace nada (nunca cambia claves de cuentas existentes).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InicializadorAdmin implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder encoder;

    @Value("${seguridad.admin-correo:}")
    private String correo;
    @Value("${seguridad.admin-password:}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        boolean hayAdmin = usuarioRepository.findAll().stream()
                .anyMatch(u -> u.getRol() == Rol.ADMIN_GENERAL && Boolean.TRUE.equals(u.getActivo()));
        if (hayAdmin) {
            return;
        }
        if (correo.isBlank() || password.length() < 8) {
            log.warn("No hay administrador general activo. Define ADMIN_CORREO y ADMIN_PASSWORD (mínimo 8 caracteres) "
                    + "y reinicia para crear el primero.");
            return;
        }
        var existente = usuarioRepository.findByCorreo(correo.trim().toLowerCase());
        if (existente.isPresent()) {
            Usuario e = existente.get();
            if (e.getRol() == Rol.ADMINISTRADOR && Boolean.TRUE.equals(e.getActivo())) {
                // Instalaciones anteriores: la cuenta indicada en ADMIN_CORREO pasa a ser administrador general.
                e.setRol(Rol.ADMIN_GENERAL);
                usuarioRepository.save(e);
                log.info("La cuenta {} fue promovida a administrador general.", e.getCorreo());
            } else {
                log.warn("ADMIN_CORREO ya existe con otro rol o inactivo; no se modifica.");
            }
            return;
        }
        Usuario u = new Usuario();
        u.setNombre("Administrador general");
        u.setCorreo(correo.trim().toLowerCase());
        u.setPassword(encoder.encode(password));
        u.setRol(Rol.ADMIN_GENERAL);
        u.setActivo(true);
        usuarioRepository.save(u);
        log.info("Administrador general inicial creado: {}", u.getCorreo());
    }
}
