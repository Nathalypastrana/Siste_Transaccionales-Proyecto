package com.hotel.reservas.service;

import com.hotel.reservas.dto.AuditoriaResponse;
import com.hotel.reservas.model.Auditoria;
import com.hotel.reservas.repository.AuditoriaRepository;
import com.hotel.reservas.security.UsuarioAutenticado;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Auditoría de acciones. Se registra en una transacción propia para que el registro no dependa de si la operación
 * principal termina bien o mal, y un fallo al auditar nunca rompe la operación del usuario.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private static final int LIMITE_DEFECTO = 200;
    private static final int LIMITE_MAXIMO = 1000;

    private final AuditoriaRepository repository;
    private final PlatformTransactionManager transacciones;

    /** Registra una acción hecha por el usuario de la sesión actual. */
    public void registrar(String accion, String entidad, Object entidadId, String detalle) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioAutenticado u) {
            guardar(u.id(), u.correo(), u.rol().name(), accion, entidad, entidadId, detalle);
        } else {
            guardar(null, null, null, accion, entidad, entidadId, detalle);
        }
    }

    /** Registra una acción con un actor explícito (inicio de sesión, recuperación de clave: aún no hay sesión). */
    public void registrarComo(Integer usuarioId, String correo, String rol, String accion, String entidad,
                              Object entidadId, String detalle) {
        guardar(usuarioId, correo, rol, accion, entidad, entidadId, detalle);
    }

    private void guardar(Integer usuarioId, String correo, String rol, String accion, String entidad,
                         Object entidadId, String detalle) {
        try {
            Auditoria a = new Auditoria();
            a.setUsuarioId(usuarioId);
            a.setCorreo(recortar(correo, 100));
            a.setRol(rol);
            a.setAccion(recortar(accion, 40));
            a.setEntidad(recortar(entidad, 40));
            a.setEntidadId(entidadId == null ? null : recortar(String.valueOf(entidadId), 40));
            a.setDetalle(recortar(detalle, 500));
            a.setIp(ipActual());
            TransactionTemplate plantilla = new TransactionTemplate(transacciones);
            plantilla.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            plantilla.executeWithoutResult(estado -> repository.save(a));
        } catch (Exception e) {
            log.error("No se pudo registrar la auditoría de {}: {}", accion, e.getMessage());
        }
    }

    private String ipActual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes sra) {
            HttpServletRequest r = sra.getRequest();
            return recortar(r.getRemoteAddr(), 45);
        }
        return null;
    }

    private static String recortar(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    @Transactional(readOnly = true)
    public List<AuditoriaResponse> listar(LocalDate desde, LocalDate hasta, String accion, String texto, Integer limite) {
        int tope = limite == null || limite < 1 ? LIMITE_DEFECTO : Math.min(limite, LIMITE_MAXIMO);
        Specification<Auditoria> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (desde != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("fecha"), desde.atStartOfDay()));
            }
            if (hasta != null) {
                p.add(cb.lessThan(root.get("fecha"), hasta.plusDays(1).atStartOfDay()));
            }
            if (accion != null && !accion.isBlank()) {
                p.add(cb.equal(root.get("accion"), accion.trim()));
            }
            if (texto != null && !texto.isBlank()) {
                String like = "%" + texto.trim().toLowerCase() + "%";
                p.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.<String>get("correo"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("detalle"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("entidadId"), "")), like)));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        return repository.findAll(spec, PageRequest.of(0, tope, Sort.by(Sort.Direction.DESC, "fecha", "id")))
                .stream().map(AuditoriaResponse::de).toList();
    }
}
