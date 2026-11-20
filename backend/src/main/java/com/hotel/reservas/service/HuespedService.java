package com.hotel.reservas.service;

import com.hotel.reservas.dto.HuespedRequest;
import com.hotel.reservas.dto.HuespedResponse;
import com.hotel.reservas.exception.ConflictoReservaException;
import com.hotel.reservas.exception.RecursoNoEncontradoException;
import com.hotel.reservas.model.Huesped;
import com.hotel.reservas.repository.HuespedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HuespedService {

    private final HuespedRepository huespedRepository;
    private final AuditoriaService auditoria;

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
        Huesped h = new Huesped();
        aplicar(h, req);
        verificarDuplicados(h, null);
        Huesped guardado = huespedRepository.save(h);
        auditoria.registrar("HUESPED_CREADO", "Huesped", guardado.getId(), "Huésped registrado");
        return HuespedResponse.de(guardado);
    }

    /** Modifica los datos de un huésped existente, con las mismas validaciones de duplicados que al crearlo. */
    @Transactional
    public HuespedResponse actualizar(Integer id, HuespedRequest req) {
        Huesped h = huespedRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el huésped " + id + "."));
        // Se valida con una copia: si se modificara la entidad antes, la consulta de duplicados la enviaría a la BD.
        Huesped nuevos = new Huesped();
        aplicar(nuevos, req);
        verificarDuplicados(nuevos, id);
        List<String> antes = resumen(h);
        aplicar(h, req);
        Huesped guardado = huespedRepository.save(h);
        auditoria.registrar("HUESPED_MODIFICADO", "Huesped", id, "Campos modificados: " + cambios(antes, resumen(guardado)));
        return HuespedResponse.de(guardado);
    }

    private void aplicar(Huesped h, HuespedRequest req) {
        h.setTipoDocumento(req.tipoDocumento().trim().toUpperCase());
        h.setNumeroDocumento(req.numeroDocumento().trim().toUpperCase());
        h.setNombres(espacios(req.nombres()));
        h.setApellidos(espacios(req.apellidos()));
        // El teléfono se guarda solo con dígitos, para que el índice único de la BD compare igual "+57 300-111" y "57300111".
        String telefono = vacioANulo(req.telefono());
        h.setTelefono(telefono == null ? null : vacioANulo(telefono.replaceAll("\\D", "")));
        String correo = vacioANulo(req.correo());
        h.setCorreo(correo == null ? null : correo.toLowerCase(Locale.ROOT));
    }

    /**
     * No se permiten dos huéspedes con el mismo número de documento, el mismo nombre completo, el mismo correo
     * o el mismo teléfono. Los nombres se comparan sin distinguir mayúsculas, tildes ni espacios repetidos;
     * los teléfonos, por sus últimos 10 dígitos. Se informan todos los campos repetidos en un solo mensaje.
     */
    private void verificarDuplicados(Huesped h, Integer idPropio) {
        String doc = h.getNumeroDocumento();
        String nombre = normalizar(h.getNombres() + " " + h.getApellidos());
        String correo = h.getCorreo();
        String tel = ultimos10(h.getTelefono());

        Set<String> repetidos = new LinkedHashSet<>();
        for (Huesped otro : huespedRepository.findAll()) {
            if (otro.getId().equals(idPropio)) {
                continue;
            }
            if (doc.equalsIgnoreCase(otro.getNumeroDocumento())) {
                repetidos.add("número de documento");
            }
            if (nombre.equals(normalizar(otro.getNombres() + " " + otro.getApellidos()))) {
                repetidos.add("nombres y apellidos");
            }
            if (correo != null && correo.equalsIgnoreCase(otro.getCorreo())) {
                repetidos.add("correo");
            }
            if (!tel.isEmpty() && tel.equals(ultimos10(otro.getTelefono()))) {
                repetidos.add("teléfono");
            }
        }
        if (!repetidos.isEmpty()) {
            throw new ConflictoReservaException("Ya existe otro huésped con el mismo " + String.join(", ", repetidos) + ".");
        }
    }

    private static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static String espacios(String texto) {
        return texto.trim().replaceAll("\\s+", " ");
    }

    private static List<String> resumen(Huesped h) {
        return List.of(h.getTipoDocumento(), h.getNumeroDocumento(), h.getNombres(), h.getApellidos(),
                String.valueOf(h.getTelefono()), String.valueOf(h.getCorreo()));
    }

    /** Lista solo los nombres de los campos que cambiaron (sin sus valores, para no guardar datos personales en la auditoría). */
    private static String cambios(List<String> antes, List<String> despues) {
        String[] campos = {"tipo de documento", "número de documento", "nombres", "apellidos", "teléfono", "correo"};
        List<String> distintos = new ArrayList<>();
        for (int i = 0; i < campos.length; i++) {
            if (!antes.get(i).equals(despues.get(i))) {
                distintos.add(campos[i]);
            }
        }
        return distintos.isEmpty() ? "ninguno" : String.join(", ", distintos);
    }

    private String vacioANulo(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
