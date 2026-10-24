package com.hotel.reservas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Huesped",
        uniqueConstraints = @UniqueConstraint(name = "UQ_Huesped_Documento",
                columnNames = {"tipoDocumento", "numeroDocumento"}))
@Getter
@Setter
@NoArgsConstructor
public class Huesped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idHuesped")
    private Integer id;

    @Column(name = "tipoDocumento", nullable = false, length = 20)
    private String tipoDocumento;

    @Column(name = "numeroDocumento", nullable = false, length = 30)
    private String numeroDocumento;

    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "correo", length = 100)
    private String correo;
}
