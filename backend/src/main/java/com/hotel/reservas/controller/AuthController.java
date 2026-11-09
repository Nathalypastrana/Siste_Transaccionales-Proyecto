package com.hotel.reservas.controller;

import com.hotel.reservas.dto.LoginRequest;
import com.hotel.reservas.dto.LoginResponse;
import com.hotel.reservas.dto.UsuarioResponse;
import com.hotel.reservas.dto.MensajeResponse;
import com.hotel.reservas.dto.RestablecerRequest;
import com.hotel.reservas.dto.SolicitudRecuperacion;
import com.hotel.reservas.repository.UsuarioRepository;
import com.hotel.reservas.service.RecuperacionClaveService;
import com.hotel.reservas.security.UsuarioAutenticado;
import com.hotel.reservas.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UsuarioRepository usuarioRepository;
    private final RecuperacionClaveService recuperacion;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.iniciarSesion(request);
    }

    /** Renueva la sesión: devuelve un token nuevo con vigencia completa. */
    @PostMapping("/renovar")
    public LoginResponse renovar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return authService.renovar(usuario.id());
    }

    /** Pide el enlace para restablecer la contraseña. Siempre responde igual, exista o no el correo. */
    @PostMapping("/olvide")
    public MensajeResponse olvide(@Valid @RequestBody SolicitudRecuperacion request) {
        recuperacion.solicitar(request.correo());
        return new MensajeResponse("Si el correo está registrado, te enviamos un enlace para restablecer la contraseña. "
                + "Vence en 30 minutos.");
    }

    @PostMapping("/restablecer")
    public MensajeResponse restablecer(@Valid @RequestBody RestablecerRequest request) {
        recuperacion.restablecer(request.token(), request.password());
        return new MensajeResponse("Contraseña actualizada. Ya puedes iniciar sesión.");
    }

    /** Usuario de la sesión actual (la interfaz lo usa para restaurar la sesión). */
    @GetMapping("/yo")
    public UsuarioResponse yo(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return usuarioRepository.findById(usuario.id()).map(UsuarioResponse::de).orElseThrow();
    }
}
