package com.hotel.reservas.service;

import com.hotel.reservas.dto.UsuarioAdminResponse;
import com.hotel.reservas.dto.UsuarioRequest;
import com.hotel.reservas.exception.ConflictoReservaException;
import com.hotel.reservas.exception.RecursoNoEncontradoException;
import com.hotel.reservas.model.Rol;
import com.hotel.reservas.model.Usuario;
import com.hotel.reservas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Gestionar usuarios (RF 11 / CU-02). Las contraseñas se guardan cifradas con BCrypt. */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final int MIN_PASSWORD = 8;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder encoder;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<UsuarioAdminResponse> listar(Rol actor) {
        return usuarioRepository.findAll(Sort.by("nombre")).stream()
                .filter(u -> actor == Rol.ADMIN_GENERAL || u.getRol() == Rol.RECEPCIONISTA)
                .map(UsuarioAdminResponse::de).toList();
    }

    /** El administrador del hotel solo gestiona recepcionistas; el general gestiona cualquier rol. */
    private void validarAlcance(Rol actor, Rol rolDestino) {
        if (actor != Rol.ADMIN_GENERAL && rolDestino != Rol.RECEPCIONISTA) {
            throw new AccessDeniedException("Solo el administrador general puede gestionar cuentas de administración.");
        }
    }

    @Transactional
    public UsuarioAdminResponse crear(UsuarioRequest req, Rol actor) {
        validarAlcance(actor, req.rol());
        String correo = req.correo().trim().toLowerCase();
        if (usuarioRepository.findByCorreo(correo).isPresent()) {
            throw new ConflictoReservaException("Ya existe un usuario con ese correo.");
        }
        validarPassword(req.password(), true);
        Usuario u = new Usuario();
        u.setNombre(req.nombre().trim());
        u.setCorreo(correo);
        u.setPassword(encoder.encode(req.password()));
        u.setRol(req.rol());
        u.setActivo(req.activo() == null ? Boolean.TRUE : req.activo());
        u = usuarioRepository.save(u);
        auditoria.registrar("USUARIO_CREADO", "Usuario", u.getId(), u.getCorreo() + ", rol " + u.getRol());
        return UsuarioAdminResponse.de(u);
    }

    /** @param actualId usuario que hace el cambio: no puede quitarse a sí mismo el acceso de administrador. */
    @Transactional
    public UsuarioAdminResponse actualizar(Integer id, UsuarioRequest req, Integer actualId, Rol actor) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));
        validarAlcance(actor, u.getRol());
        validarAlcance(actor, req.rol());
        String correo = req.correo().trim().toLowerCase();
        usuarioRepository.findByCorreo(correo).ifPresent(otro -> {
            if (!otro.getId().equals(id)) {
                throw new ConflictoReservaException("Ya existe un usuario con ese correo.");
            }
        });
        boolean activo = req.activo() == null ? Boolean.TRUE.equals(u.getActivo()) : req.activo();
        if (id.equals(actualId) && (!activo || req.rol() != u.getRol())) {
            throw new ConflictoReservaException(
                    "No puedes desactivar tu propia cuenta ni cambiarte el rol.");
        }
        String antes = u.getCorreo() + ", rol " + u.getRol() + ", " + (Boolean.TRUE.equals(u.getActivo()) ? "activo" : "inactivo");
        u.setNombre(req.nombre().trim());
        u.setCorreo(correo);
        u.setRol(req.rol());
        u.setActivo(activo);
        boolean cambioClave = req.password() != null && !req.password().isBlank();
        if (cambioClave) {
            validarPassword(req.password(), false);
            u.setPassword(encoder.encode(req.password()));
        }
        auditoria.registrar("USUARIO_ACTUALIZADO", "Usuario", id, "Antes: " + antes + ". Ahora: " + correo + ", rol "
                + req.rol() + ", " + (activo ? "activo" : "inactivo") + (cambioClave ? ", contraseña cambiada" : ""));
        return UsuarioAdminResponse.de(u);
    }

    private void validarPassword(String password, boolean obligatoria) {
        if (password == null || password.isBlank()) {
            if (obligatoria) {
                throw new IllegalArgumentException("La contraseña es obligatoria.");
            }
            return;
        }
        if (password.length() < MIN_PASSWORD) {
            throw new IllegalArgumentException("La contraseña debe tener al menos " + MIN_PASSWORD + " caracteres.");
        }
    }
}
