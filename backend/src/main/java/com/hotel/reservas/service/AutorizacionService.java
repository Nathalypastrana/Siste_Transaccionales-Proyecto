package com.hotel.reservas.service;

import com.hotel.reservas.model.AutorizacionDatos;
import com.hotel.reservas.repository.AutorizacionDatosRepository;
import com.hotel.reservas.util.Telefonos;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Autorización previa del tratamiento de datos para el consultor virtual. */
@Service
@RequiredArgsConstructor
public class AutorizacionService {

    private final AutorizacionDatosRepository repository;

    @Value("${asistente.version-aviso:1.0}")
    private String versionAviso;

    /** Vigente = aceptó la versión actual del aviso y no la ha revocado. */
    @Transactional(readOnly = true)
    public boolean vigente(String telefono) {
        return repository.findByTelefono(Telefonos.ultimos10(telefono))
                .map(a -> !Boolean.TRUE.equals(a.getRevocada()) && versionAviso.equals(a.getVersionAviso()))
                .orElse(false);
    }

    @Transactional
    public void registrar(String telefono) {
        String tel = Telefonos.ultimos10(telefono);
        AutorizacionDatos a = repository.findByTelefono(tel).orElseGet(AutorizacionDatos::new);
        a.setTelefono(tel);
        a.setFechaAutorizacion(LocalDateTime.now());
        a.setVersionAviso(versionAviso);
        a.setRevocada(false);
        a.setFechaRevocacion(null);
        repository.save(a);
    }

    @Transactional
    public void revocar(String telefono) {
        repository.findByTelefono(Telefonos.ultimos10(telefono)).ifPresent(a -> {
            a.setRevocada(true);
            a.setFechaRevocacion(LocalDateTime.now());
        });
    }
}
