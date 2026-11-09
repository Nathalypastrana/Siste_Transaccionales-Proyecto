package com.hotel.reservas.controller;

import com.hotel.reservas.asistente.modelo.ModeloLenguaje;
import com.hotel.reservas.dto.EstadoSistema;
import com.hotel.reservas.model.Rol;
import com.hotel.reservas.repository.AutorizacionDatosRepository;
import com.hotel.reservas.repository.HabitacionRepository;
import com.hotel.reservas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Estado y configuración del programa: solo el administrador general. No expone secretos ni datos personales. */
@RestController
@RequestMapping("/api/sistema")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN_GENERAL')")
public class SistemaController {

    private final UsuarioRepository usuarioRepository;
    private final HabitacionRepository habitacionRepository;
    private final AutorizacionDatosRepository autorizacionRepository;
    private final List<ModeloLenguaje> modelos;

    @Value("${asistente.proveedor:claude}")
    private String proveedor;
    @Value("${asistente.simulador-habilitado:false}")
    private boolean simulador;
    @Value("${asistente.version-aviso:1.0}")
    private String versionAviso;
    @Value("${whatsapp.access-token:}")
    private String whatsappToken;
    @Value("${whatsapp.phone-number-id:}")
    private String whatsappNumero;
    @Value("${seguridad.jwt-secreto:}")
    private String jwtSecreto;

    @GetMapping("/estado")
    public EstadoSistema estado() {
        Map<Rol, Long> conteo = new EnumMap<>(Rol.class);
        long inactivos = 0;
        for (var u : usuarioRepository.findAll()) {
            if (Boolean.TRUE.equals(u.getActivo())) {
                conteo.merge(u.getRol(), 1L, Long::sum);
            } else {
                inactivos++;
            }
        }
        Map<String, Long> porRol = new LinkedHashMap<>();
        for (Rol r : Rol.values()) {
            porRol.put(r.name(), conteo.getOrDefault(r, 0L));
        }
        var autorizaciones = autorizacionRepository.findAll();
        long revocadas = autorizaciones.stream().filter(a -> Boolean.TRUE.equals(a.getRevocada())).count();
        boolean modeloListo = modelos.stream().anyMatch(ModeloLenguaje::configurado);

        return new EstadoSistema(
                LocalDateTime.now(ZoneId.of("America/Bogota")),
                porRol,
                inactivos,
                habitacionRepository.count(),
                proveedor,
                modeloListo,
                simulador,
                !whatsappToken.isBlank() && !whatsappNumero.isBlank(),
                versionAviso,
                autorizaciones.size() - revocadas,
                revocadas,
                jwtSecreto.length() >= 32);
    }
}
