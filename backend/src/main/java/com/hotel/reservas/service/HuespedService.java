package com.hotel.reservas.service;

import com.hotel.reservas.dto.HuespedRequest;
import com.hotel.reservas.dto.HuespedResponse;
import com.hotel.reservas.exception.ConflictoReservaException;
import com.hotel.reservas.model.Huesped;
import com.hotel.reservas.repository.HuespedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HuespedService {

    private final HuespedRepository huespedRepository;

    @Transactional(readOnly = true)
    public List<HuespedResponse> listar() {
        return huespedRepository.findAllByOrderByApellidosAscNombresAsc().stream()
                .map(HuespedResponse::de)
                .toList();
    }

    /**
     * Busca un huésped por su número de teléfono (por ejemplo, el de WhatsApp).
     * Compara solo los dígitos y los últimos 10, para ignorar indicativo, espacios y guiones.
     */
    @Transactional(readOnly = true)
    public Optional<HuespedResponse> buscarPorTelefono(String telefono) {
        String buscado = ultimos10(telefono);
        if (buscado.isEmpty()) {
            return Optional.empty();
        }
        return huespedRepository.findByTelefonoIsNotNull().stream()
                .filter(h -> buscado.equals(ultimos10(h.getTelefono())))
                .findFirst()
                .map(HuespedResponse::de);
    }

    private static String ultimos10(String telefono) {
        if (telefono == null) {
            return "";
        }
        String digitos = telefono.replaceAll("\\D", "");
        return digitos.length() > 10 ? digitos.substring(digitos.length() - 10) : digitos;
    }

    @Transactional
    public HuespedResponse crear(HuespedRequest req) {
        String tipo = req.tipoDocumento().trim().toUpperCase();
        String numero = req.numeroDocumento().trim();

        if (huespedRepository.findByTipoDocumentoAndNumeroDocumento(tipo, numero).isPresent()) {
            throw new ConflictoReservaException("Ya existe un huésped con ese documento.");
        }

        Huesped h = new Huesped();
        h.setTipoDocumento(tipo);
        h.setNumeroDocumento(numero);
        h.setNombres(req.nombres().trim());
        h.setApellidos(req.apellidos().trim());
        h.setTelefono(vacioANulo(req.telefono()));
        h.setCorreo(vacioANulo(req.correo()));
        return HuespedResponse.de(huespedRepository.save(h));
    }

    private String vacioANulo(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
