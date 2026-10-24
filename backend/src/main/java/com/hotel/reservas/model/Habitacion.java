package com.hotel.reservas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Habitacion")
@Getter
@Setter
@NoArgsConstructor
public class Habitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idHabitacion")
    private Integer id;

    @Column(name = "numero", nullable = false, length = 10, unique = true)
    private String numero;

    @Column(name = "piso", nullable = false)
    private Integer piso;

    @Column(name = "capacidad", nullable = false)
    private Integer capacidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoHabitacion estado = EstadoHabitacion.DISPONIBLE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idTipoHabitacion", nullable = false)
    private TipoHabitacion tipoHabitacion;
}
