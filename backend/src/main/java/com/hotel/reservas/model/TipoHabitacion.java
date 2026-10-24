package com.hotel.reservas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "TipoHabitacion")
@Getter
@Setter
@NoArgsConstructor
public class TipoHabitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idTipoHabitacion")
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 50, unique = true)
    private String nombre;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "capacidad", nullable = false)
    private Integer capacidad;
}
