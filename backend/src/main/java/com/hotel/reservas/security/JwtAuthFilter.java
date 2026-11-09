package com.hotel.reservas.security;

import com.hotel.reservas.model.Usuario;
import com.hotel.reservas.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lee el token "Authorization: Bearer ..." y, si es válido, identifica al usuario.
 * Se consulta la base en cada petición para que desactivar una cuenta o cambiar su rol tenga efecto
 * de inmediato, sin esperar a que venza el token. No es un @Component a propósito: se agrega solo a la
 * cadena de Spring Security (ver SecurityConfig) para que no se ejecute dos veces.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecera = request.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith("Bearer ")) {
            jwtService.validar(cabecera.substring(7).trim())
                    .flatMap(usuarioRepository::findById)
                    .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                    .ifPresent(this::autenticar);
        }
        chain.doFilter(request, response);
    }

    private void autenticar(Usuario u) {
        UsuarioAutenticado principal = new UsuarioAutenticado(u.getId(), u.getCorreo(), u.getRol());
        var autenticacion = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + u.getRol().name())));
        SecurityContextHolder.getContext().setAuthentication(autenticacion);
    }
}
