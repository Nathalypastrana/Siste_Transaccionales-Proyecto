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

/**
 * Registro de auditoría: quién hizo qué y cuándo. Solo se inserta (nunca se edita ni se borra desde la aplicación).
 * Nunca contiene contraseñas ni tokens.
 */
@Entity
@Table(name = "Auditoria")
@Getter
@Setter
@NoArgsConstructor
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idAuditoria")
    private Integer id;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(name = "idUsuario")
    private Integer usuarioId;

    @Column(name = "correo", length = 100)
    private String correo;

    @Column(name = "rol", length = 20)
    private String rol;

    @Column(name = "accion", nullable = false, length = 40)
    private String accion;

    @Column(name = "entidad", length = 40)
    private String entidad;

    @Column(name = "entidadId", length = 40)
    private String entidadId;

    @Column(name = "detalle", length = 500)
    private String detalle;

    @Column(name = "ip", length = 45)
    private String ip;
}
