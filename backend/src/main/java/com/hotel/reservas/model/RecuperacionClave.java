package com.hotel.reservas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Solicitud de recuperación de contraseña. Se guarda solo el hash (SHA-256) del token, nunca el token. */
@Entity
@Table(name = "RecuperacionClave")
@Getter
@Setter
@NoArgsConstructor
public class RecuperacionClave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idRecuperacion")
    private Integer id;

    @Column(name = "idUsuario", nullable = false)
    private Integer usuarioId;

    @Column(name = "tokenHash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "creada", nullable = false)
    private LocalDateTime creada = LocalDateTime.now();

    @Column(name = "expira", nullable = false)
    private LocalDateTime expira;

    @Column(name = "usado", nullable = false)
    private Boolean usado = false;
}
