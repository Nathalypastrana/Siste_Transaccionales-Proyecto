package com.hotel.reservas.repository;

import com.hotel.reservas.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByCorreo(String correo);

    List<Usuario> findByActivoTrueOrderByNombreAsc();
}
