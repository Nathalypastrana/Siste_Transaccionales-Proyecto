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
 * Autorización para el tratamiento de datos personales en el consultor virtual (Ley 1581 de 2012).
 * Guarda cuándo aceptó la persona y qué versión del aviso vio. El teléfono se guarda como los últimos 10 dígitos.
 */
@Entity
@Table(name = "AutorizacionDatos")
@Getter
@Setter
@NoArgsConstructor
public class AutorizacionDatos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idAutorizacion")
    private Integer id;

    @Column(name = "telefono", nullable = false, length = 10, unique = true)
    private String telefono;

    @Column(name = "fechaAutorizacion", nullable = false)
    private LocalDateTime fechaAutorizacion = LocalDateTime.now();

    @Column(name = "versionAviso", nullable = false, length = 20)
    private String versionAviso;

    @Column(name = "revocada", nullable = false)
    private Boolean revocada = false;

    @Column(name = "fechaRevocacion")
    private LocalDateTime fechaRevocacion;
}
